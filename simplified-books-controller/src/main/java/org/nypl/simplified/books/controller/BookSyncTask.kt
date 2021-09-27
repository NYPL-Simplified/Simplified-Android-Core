package org.nypl.simplified.books.controller

import com.io7m.jfunctional.Some
import org.librarysimplified.http.api.LSHTTPClientType
import org.librarysimplified.http.api.LSHTTPResponseStatus
import org.nypl.simplified.accounts.api.AccountAuthenticatedHTTP
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.api.AccountLoginState
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryType
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.api.BookIDs
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryType
import org.nypl.simplified.books.book_database.api.BookDatabaseException
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.book_registry.BookStatus
import org.nypl.simplified.books.book_registry.BookWithStatus
import org.nypl.simplified.books.controller.api.BooksControllerType
import org.nypl.simplified.feeds.api.FeedLoaderType
import org.nypl.simplified.feeds.api.FeedLoading
import org.nypl.simplified.opds.core.OPDSAvailabilityRevoked
import org.nypl.simplified.opds.core.OPDSFeedParserType
import org.nypl.simplified.opds.core.OPDSParseException
import org.nypl.simplified.patron.api.PatronUserProfile
import org.nypl.simplified.patron.api.PatronUserProfileParsersType
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
import java.util.concurrent.TimeUnit

class BookSyncTask(
  private val accountID: AccountID,
  profileID: ProfileID,
  profiles: ProfilesDatabaseType,
  private val booksController: BooksControllerType,
  private val accountRegistry: AccountProviderRegistryType,
  private val bookRegistry: BookRegistryType,
  private val feedLoader: FeedLoaderType,
  private val patronParsers: PatronUserProfileParsersType,
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

    this.fetchPatronUserProfile(
      account = account,
      credentials = credentials
    )

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

  private fun fetchPatronUserProfile(
    account: AccountType,
    credentials: AccountAuthenticationCredentials
  ) {
    try {
      val profile =
        PatronUserProfiles.runPatronProfileRequest(
          taskRecorder = this.taskRecorder,
          patronParsers = this.patronParsers,
          credentials = credentials,
          http = this.http,
          account = account
        )

      account.updateCredentialsIfAvailable {
        this.withNewAnnotationsURI(it, profile)
      }
    } catch (e: Exception) {
      this.logger.error("patron user profile: ", e)
    }
  }

  private fun withNewAnnotationsURI(
    currentCredentials: AccountAuthenticationCredentials,
    profile: PatronUserProfile
  ): AccountAuthenticationCredentials {
    return when (currentCredentials) {
      is AccountAuthenticationCredentials.Basic ->
        currentCredentials.copy(annotationsURI = profile.annotationsURI)
      is AccountAuthenticationCredentials.OAuthWithIntermediary ->
        currentCredentials.copy(annotationsURI = profile.annotationsURI)
      is AccountAuthenticationCredentials.SAML2_0 ->
        currentCredentials.copy(annotationsURI = profile.annotationsURI)
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
     * Now delete/revoke any book that previously existed, but is not in the
     * received set.
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
          } else {
            this.logger.debug("[{}] deleting", existingId.brief())
            this.updateRegistryForBook(account, dbEntry)
            dbEntry.delete()
          }
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

  private fun updateRegistryForBook(
    account: AccountType,
    dbEntry: BookDatabaseEntryType
  ) {
    this.logger.debug("attempting to fetch book permalink to update registry")
    val alternateOpt = dbEntry.book.entry.alternate
    return if (alternateOpt is Some<URI>) {
      val alternate = alternateOpt.get()
      val entry =
        FeedLoading.loadSingleEntryFeed(
          feedLoader = this.feedLoader,
          taskRecorder = this.taskRecorder,
          accountID = this.accountID,
          uri = alternate,
          timeout = Pair(30L, TimeUnit.SECONDS),
          httpAuth = null,
          method = "GET"
        )

      /*
       * Write a new book database entry based on the server state, and pretend that all the
       * books have been deleted. The code will delete the entire database entry after this
       * method returns anyway, so this code ensures that something sensible goes into the
       * book registry.
       */

      dbEntry.writeOPDSEntry(entry.feedEntry)
      val newBook = dbEntry.book.copy(formats = emptyList())
      val status = BookStatus.fromBook(newBook)

      this.logger.debug("book's new state is {}", status)
      this.bookRegistry.update(BookWithStatus(newBook, status))
    } else {
      throw IOException("No alternate link is available")
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
