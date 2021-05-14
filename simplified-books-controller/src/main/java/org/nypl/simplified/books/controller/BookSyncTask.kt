package org.nypl.simplified.books.controller

import com.google.common.base.Preconditions
import com.io7m.jfunctional.Some
import org.joda.time.DateTime
import org.librarysimplified.http.api.LSHTTPClientType
import org.librarysimplified.http.api.LSHTTPResponseStatus
import org.nypl.simplified.accounts.api.AccountAuthenticatedHTTP
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.api.AccountLoginState
import org.nypl.simplified.accounts.api.AccountProvider
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryType
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.api.BookIDs
import org.nypl.simplified.books.book_database.api.BookDatabaseException
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.book_registry.BookStatus
import org.nypl.simplified.books.book_registry.BookWithStatus
import org.nypl.simplified.books.controller.api.BooksControllerType
import org.nypl.simplified.opds.core.OPDSAcquisitionFeed
import org.nypl.simplified.opds.core.OPDSAvailabilityRevoked
import org.nypl.simplified.opds.core.OPDSFeedParserType
import org.nypl.simplified.opds.core.OPDSParseException
import org.nypl.simplified.profiles.api.ProfileID
import org.nypl.simplified.profiles.api.ProfilesDatabaseType
import org.nypl.simplified.taskrecorder.api.TaskRecorder
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.util.HashSet

class BookSyncTask(
  accountID: AccountID,
  profileID: ProfileID,
  profiles: ProfilesDatabaseType,
  private val booksController: BooksControllerType,
  private val accountRegistry: AccountProviderRegistryType,
  private val bookRegistry: BookRegistryType,
  private val http: LSHTTPClientType,
  private val feedParser: OPDSFeedParserType
) : AbstractBookTask(accountID, profileID, profiles) {

  override val logger =
    LoggerFactory.getLogger(BookSyncTask::class.java)

  override val taskRecorder =
    TaskRecorder.create()

  override fun execute(account: AccountType): TaskResult.Success<Unit> {
    this.logger.debug("syncing account {}", account.id)
    this.taskRecorder.beginNewStep("Syncing...")

    val provider = this.updateAccountProvider(account)
    val providerAuth = provider.authentication
    if (providerAuth == AccountProviderAuthenticationDescription.Anonymous) {
      this.logger.debug("account does not support syncing")
      return this.taskRecorder.finishSuccess(Unit)
    }

    val credentials = account.loginState.credentials
    if (credentials == null) {
      this.logger.debug("no credentials, aborting!")
      return this.taskRecorder.finishSuccess(Unit)
    }

    val loansURI = provider.loansURI
    if (loansURI == null) {
      this.logger.debug("no loans URI, aborting!")
      return this.taskRecorder.finishSuccess(Unit)
    }

    val request =
      this.http.newRequest(loansURI)
        .setAuthorization(AccountAuthenticatedHTTP.createAuthorization(credentials))
        .build()

    val response = request.execute()
    return when (val status = response.status) {
      is LSHTTPResponseStatus.Responded.OK -> {
        this.onHTTPOK(status.bodyStream ?: ByteArrayInputStream(ByteArray(0)), provider, account)
        this.taskRecorder.finishSuccess(Unit)
      }
      is LSHTTPResponseStatus.Responded.Error -> {
        val recovered = this.onHTTPError(status, account)

        if (recovered) {
          this.taskRecorder.finishSuccess(Unit)
        } else {
          val message = String.format("%s: %d: %s", provider.loansURI, status.properties.status, status.properties.message)
          val exception = IOException(message)
          this.taskRecorder.currentStepFailed(
            message = message,
            errorCode = "syncFailed",
            exception = exception
          )
          throw TaskFailedHandled(exception)
        }
      }
      is LSHTTPResponseStatus.Failed ->
        throw IOException(status.exception)
    }
  }

  override fun onFailure(result: TaskResult.Failure<Unit>) {
    // Nothing to do
  }

  private fun updateAccountProvider(account: AccountType): AccountProviderType {
    this.logger.debug("resolving the existing account provider")

    val oldProvider = account.provider
    var newDescription =
      this.accountRegistry.findAccountProviderDescription(oldProvider.id)
    if (newDescription == null) {
      this.logger.debug("could not find account description for {} in registry", oldProvider.id)
      newDescription = oldProvider.toDescription()
    } else {
      this.logger.debug("found account description for {} in registry", oldProvider.id)
    }

    val newProviderResult =
      this.accountRegistry.resolve(
        { accountProvider, message ->
          this.logger.debug("[{}]: {}", accountProvider, message)
        },
        newDescription
      )

    return when (newProviderResult) {
      is TaskResult.Success -> {
        this.logger.debug("successfully resolved the account provider")
        account.setAccountProvider(newProviderResult.result)
        newProviderResult.result
      }
      is TaskResult.Failure -> {
        this.logger.error("failed to resolve account provider: ", newProviderResult.exception)
        oldProvider
      }
    }
  }

  @Throws(IOException::class)
  private fun onHTTPOK(
    stream: InputStream,
    provider: AccountProviderType,
    account: AccountType
  ) {
    return stream.use { ok ->
      this.parseFeed(ok, provider, account)
    }
  }

  @Throws(OPDSParseException::class)
  private fun parseFeed(
    stream: InputStream,
    provider: AccountProviderType,
    account: AccountType
  ) {
    val feed = this.feedParser.parse(provider.loansURI, stream)
    this.updateAnnotations(feed, account)

    /*
     * Obtain the set of books that are on disk already. If any
     * of these books are not in the received feed, then they have
     * expired and should be deleted.
     */

    val bookDatabase = account.bookDatabase
    val existing = bookDatabase.books()

    /*
     * Handle each book in the received feed.
     */

    val received = HashSet<BookID>(64)
    val entries = feed.feedEntries
    for (opdsEntry in entries) {
      val bookId = BookIDs.newFromOPDSEntry(opdsEntry)
      received.add(bookId)
      this.logger.debug("[{}] updating", bookId.brief())

      try {
        val databaseEntry = bookDatabase.createOrUpdate(bookId, opdsEntry)
        val book = databaseEntry.book
        this.bookRegistry.update(BookWithStatus(book, BookStatus.fromBook(book)))
      } catch (e: BookDatabaseException) {
        this.logger.error("[{}] unable to update database entry: ", bookId.brief(), e)
      }
    }

    /*
     * Now delete any book that previously existed, but is not in the
     * received set. Queue any revoked books for completion and then
     * deletion.
     */

    val revoking = HashSet<BookID>(existing.size)
    for (existingId in existing) {
      try {
        this.logger.debug("[{}] checking for deletion", existingId.brief())

        if (!received.contains(existingId)) {
          val dbEntry = bookDatabase.entry(existingId)
          val a = dbEntry.book.entry.availability
          if (a is OPDSAvailabilityRevoked) {
            revoking.add(existingId)
          }

          this.logger.debug("[{}] deleting", existingId.brief())
          dbEntry.delete()
          this.bookRegistry.clearFor(existingId)
        } else {
          this.logger.debug("[{}] keeping", existingId.brief())
        }
      } catch (x: Throwable) {
        this.logger.error("[{}]: unable to delete entry: ", existingId.value(), x)
      }
    }

    /*
     * Finish the revocation of any books that need it.
     */

    for (revoke_id in revoking) {
      this.logger.debug("[{}] revoking", revoke_id.brief())
      this.booksController.bookRevoke(account.id, revoke_id)
    }
  }

  /**
   * Check to see if the feed contains an annotations link. If it does, update the account
   * provider to indicate that the provider does support bookmark syncing.
   */

  private fun updateAnnotations(feed: OPDSAcquisitionFeed, account: AccountType) {
    this.logger.debug("checking feed for annotations link")

    if (feed.annotations.isSome) {
      this.logger.debug("feed contains annotations link: setting sync support to 'true'")

      val newAccountProvider =
        AccountProvider.copy(account.provider)
          .copy(
            annotationsURI = (feed.annotations as Some<URI>).get(),
            updated = DateTime.now()
          )
      Preconditions.checkArgument(
        newAccountProvider.supportsSimplyESynchronization,
        "Support for syncing must now be enabled"
      )

      val newProvider = this.accountRegistry.updateProvider(newAccountProvider)
      Preconditions.checkArgument(
        newProvider.supportsSimplyESynchronization,
        "Support for syncing must now be enabled"
      )

      account.setAccountProvider(newProvider)
      Preconditions.checkArgument(
        account.provider.supportsSimplyESynchronization,
        "Support for syncing must now be enabled"
      )
    } else {
      this.logger.debug("feed does not contain annotations link")
    }
  }

  /**
   * Returns whether we recovered from the error.
   */

  private fun onHTTPError(
    result: LSHTTPResponseStatus.Responded.Error,
    account: AccountType
  ): Boolean {
    if (result.properties.status == 401) {
      this.logger.debug("removing credentials due to 401 server response")
      account.setLoginState(AccountLoginState.AccountNotLoggedIn)
      return true
    }
    return false
  }
}
