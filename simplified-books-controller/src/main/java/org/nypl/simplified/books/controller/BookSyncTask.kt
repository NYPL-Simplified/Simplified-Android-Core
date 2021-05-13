package org.nypl.simplified.books.controller

import org.librarysimplified.http.api.LSHTTPClientType
import org.librarysimplified.http.api.LSHTTPResponseStatus
import org.nypl.simplified.accounts.api.AccountAuthenticatedHTTP
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountLoginState
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
import org.nypl.simplified.opds.core.OPDSAvailabilityRevoked
import org.nypl.simplified.opds.core.OPDSFeedParserType
import org.nypl.simplified.opds.core.OPDSParseException
import org.nypl.simplified.patron.api.PatronUserProfileParsersType
import org.nypl.simplified.taskrecorder.api.TaskRecorder
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.util.HashSet
import java.util.concurrent.Callable

class BookSyncTask(
  private val booksController: BooksControllerType,
  private val account: AccountType,
  private val accountRegistry: AccountProviderRegistryType,
  private val bookRegistry: BookRegistryType,
  private val patronParsers: PatronUserProfileParsersType,
  private val http: LSHTTPClientType,
  private val feedParser: OPDSFeedParserType
) : Callable<TaskResult<Unit>> {

  private val logger =
    LoggerFactory.getLogger(BookSyncTask::class.java)
  private val taskRecorder =
    TaskRecorder.create()

  @Throws(Exception::class)
  override fun call(): TaskResult<Unit> {
    return try {
      this.logger.debug("syncing account {}", this.account.id)
      this.execute()
      this.taskRecorder.finishSuccess(Unit)
    } catch (e: Exception) {
      this.taskRecorder.currentStepFailed(e.message ?: e.javaClass.name, "unexpectedException", e)
      this.taskRecorder.finishFailure()
    } finally {
      this.logger.debug("finished syncing account {}", this.account.id)
    }
  }

  @Throws(Exception::class)
  private fun execute() {
    this.taskRecorder.beginNewStep("Updating account provider…")
    val provider = this.updateAccountProvider()

    this.taskRecorder.beginNewStep("Syncing account…")
    val providerAuth = provider.authentication
    if (providerAuth == AccountProviderAuthenticationDescription.Anonymous) {
      this.taskRecorder.currentStepSucceeded("Account does not support syncing.")
      return
    }

    val credentials = this.account.loginState.credentials
    if (credentials == null) {
      this.taskRecorder.currentStepSucceeded("No credentials, aborting!")
      return
    }

    this.fetchPatronUserProfile(credentials)

    val loansURI = provider.loansURI
    if (loansURI == null) {
      this.taskRecorder.currentStepSucceeded("No loans URI, aborting!")
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

  private fun fetchPatronUserProfile(
    credentials: AccountAuthenticationCredentials
  ): Any {
    return try {
      val profile =
        PatronUserProfiles.runPatronProfileRequest(
          taskRecorder = this.taskRecorder,
          patronParsers = this.patronParsers,
          credentials = credentials,
          http = this.http,
          account = this.account
        )

      when (val currentCredentials = this.account.loginState.credentials) {
        is AccountAuthenticationCredentials.Basic ->
          currentCredentials.copy(annotationsURI = profile.annotationsURI)
        is AccountAuthenticationCredentials.OAuthWithIntermediary ->
          currentCredentials.copy(annotationsURI = profile.annotationsURI)
        is AccountAuthenticationCredentials.SAML2_0 ->
          currentCredentials.copy(annotationsURI = profile.annotationsURI)
        null ->
          Unit
      }
    } catch (e: Exception) {
      this.logger.error("patron user profile: ", e)
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
