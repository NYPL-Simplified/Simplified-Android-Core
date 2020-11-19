package org.nypl.simplified.books.controller

import com.google.common.base.Preconditions
import com.io7m.jfunctional.Some
import org.joda.time.DateTime
import org.librarysimplified.http.api.LSHTTPClientType
import org.librarysimplified.http.api.LSHTTPResponseStatus
import org.nypl.simplified.accounts.api.AccountAuthenticatedHTTP
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
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.util.HashSet
import java.util.concurrent.Callable

class BookSyncTask(
  private val booksController: BooksControllerType,
  private val account: AccountType,
  private val accountRegistry: AccountProviderRegistryType,
  private val bookRegistry: BookRegistryType,
  private val http: LSHTTPClientType,
  private val feedParser: OPDSFeedParserType
) : Callable<Unit> {

  private val logger = LoggerFactory.getLogger(BookSyncTask::class.java)

  @Throws(Exception::class)
  override fun call() {
    try {
      this.logger.debug("syncing account {}", this.account.id)
      return this.execute()
    } finally {
      this.logger.debug("finished syncing account {}", this.account.id)
    }
  }

  @Throws(Exception::class)
  private fun execute() {
    val provider = this.updateAccountProvider()

    val providerAuth = provider.authentication
    if (providerAuth == AccountProviderAuthenticationDescription.Anonymous) {
      this.logger.debug("account does not support syncing")
      return
    }

    val credentials = this.account.loginState.credentials
    if (credentials == null) {
      this.logger.debug("no credentials, aborting!")
      return
    }

    val loansURI = provider.loansURI
    if (loansURI == null) {
      this.logger.debug("no loans URI, aborting!")
      return
    }

    val request =
      this.http.newRequest(loansURI)
        .setAuthorization(AccountAuthenticatedHTTP.createAuthorization(credentials))
        .build()

    val response = request.execute()
    return when (val status = response.status) {
      is LSHTTPResponseStatus.Responded.OK ->
        this.onHTTPOK(status.bodyStream ?: ByteArrayInputStream(ByteArray(0)), provider)
      is LSHTTPResponseStatus.Responded.Error ->
        this.onHTTPError(status, provider)
      is LSHTTPResponseStatus.Failed ->
        throw IOException(status.exception)
    }
  }

  private fun updateAccountProvider(): AccountProviderType {
    this.logger.debug("resolving the existing account provider")

    val oldProvider = this.account.provider
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
        this.account.setAccountProvider(newProviderResult.result)
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
    provider: AccountProviderType
  ) {
    return stream.use { ok ->
      this.parseFeed(ok, provider)
    }
  }

  @Throws(OPDSParseException::class)
  private fun parseFeed(
    stream: InputStream,
    provider: AccountProviderType
  ) {
    val feed = this.feedParser.parse(provider.loansURI, stream)
    this.updateAnnotations(feed)

    /*
     * Obtain the set of books that are on disk already. If any
     * of these books are not in the received feed, then they have
     * expired and should be deleted.
     */

    val bookDatabase = this.account.bookDatabase
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
      this.booksController.bookRevoke(this.account, revoke_id)
    }
  }

  /**
   * Check to see if the feed contains an annotations link. If it does, update the account
   * provider to indicate that the provider does support bookmark syncing.
   */

  private fun updateAnnotations(feed: OPDSAcquisitionFeed) {
    this.logger.debug("checking feed for annotations link")

    if (feed.annotations.isSome) {
      this.logger.debug("feed contains annotations link: setting sync support to 'true'")

      val newAccountProvider =
        AccountProvider.copy(this.account.provider)
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

      this.account.setAccountProvider(newProvider)
      Preconditions.checkArgument(
        this.account.provider.supportsSimplyESynchronization,
        "Support for syncing must now be enabled"
      )
    } else {
      this.logger.debug("feed does not contain annotations link")
    }
  }

  @Throws(Exception::class)
  private fun onHTTPError(
    result: LSHTTPResponseStatus.Responded.Error,
    provider: AccountProviderType
  ) {
    if (result.properties.status == 401) {
      this.logger.debug("removing credentials due to 401 server response")
      this.account.setLoginState(AccountLoginState.AccountNotLoggedIn)
      return
    }

    throw IOException(String.format("%s: %d: %s", provider.loansURI, result.properties.status, result.properties.message))
  }
}
