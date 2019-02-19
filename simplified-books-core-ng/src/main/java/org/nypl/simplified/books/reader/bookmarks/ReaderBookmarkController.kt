package org.nypl.simplified.books.reader.bookmarks

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.util.concurrent.FluentFuture
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListeningScheduledExecutorService
import com.google.common.util.concurrent.MoreExecutors
import com.io7m.jfunctional.Some
import com.io7m.junreachable.UnimplementedCodeException
import org.nypl.simplified.books.accounts.AccountAuthenticationCredentials
import org.nypl.simplified.books.accounts.AccountEvent
import org.nypl.simplified.books.accounts.AccountEventCreation.AccountCreationSucceeded
import org.nypl.simplified.books.accounts.AccountEventDeletion.AccountDeletionSucceeded
import org.nypl.simplified.books.accounts.AccountEventUpdated
import org.nypl.simplified.books.accounts.AccountID
import org.nypl.simplified.books.accounts.AccountType
import org.nypl.simplified.books.book_database.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleEPUB
import org.nypl.simplified.books.controller.ProfilesControllerType
import org.nypl.simplified.books.profiles.ProfileEvent
import org.nypl.simplified.books.profiles.ProfileNoneCurrentException
import org.nypl.simplified.books.profiles.ProfileReadableType
import org.nypl.simplified.books.reader.ReaderBookmark
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkEvent.ReaderBookmarkSaved
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkEvent.ReaderBookmarkSyncFinished
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkEvent.ReaderBookmarkSyncStarted
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkPolicyInput.Event.Local.AccountCreated
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkPolicyInput.Event.Local.AccountDeleted
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkPolicyInput.Event.Local.AccountUpdated
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkPolicyInput.Event.Local.BookmarkCreated
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkPolicyInput.Event.Local.BookmarkDeleteRequested
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkPolicyInput.Event.Remote.BookmarkReceived
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkPolicyInput.Event.Remote.BookmarkSaved
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkPolicyInput.Event.Remote.SyncingEnabled
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkPolicyOutput.Command
import org.nypl.simplified.observable.ObservableReadableType
import org.nypl.simplified.observable.ObservableType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.concurrent.Executors

/**
 * The default implementation of the bookmark controller interface.
 *
 * This implementation generally makes the assumption that bookmark syncing is not particularly
 * critical and so simply logs and otherwise ignores errors. Syncing is treated as best-effort and
 * all failures are assumed to be temporary.
 */

class ReaderBookmarkController private constructor(
  private val threads: (Runnable) -> Thread,
  private val httpCalls: ReaderBookmarkHTTPCallsType,
  private val bookmarkEventsOut: ObservableType<ReaderBookmarkEvent>,
  private val profilesController: ProfilesControllerType)
  : ReaderBookmarkControllerType {

  /**
   * A trivial Thread subclass for efficient checks to determine whether or not the current
   * thread is a controller thread.
   */

  private class ReaderBookmarkControllerThread(thread: Thread) : Thread(thread)

  private val executor: ListeningScheduledExecutorService =
    MoreExecutors.listeningDecorator(
      Executors.newScheduledThreadPool(1) { runnable ->
        ReaderBookmarkControllerThread(this.threads.invoke(runnable))
      })

  override fun close() {
    this.executor.shutdown()
  }

  override val bookmarkEvents: ObservableReadableType<ReaderBookmarkEvent>
    get() = this.bookmarkEventsOut

  private val logger = LoggerFactory.getLogger(ReaderBookmarkController::class.java)
  private val objectMapper = ObjectMapper()

  @Volatile
  private var policyState: ReaderBookmarkPolicyState

  init {
    this.profilesController.profileEvents().subscribe { event -> this.onProfileEvent(event) }
    this.profilesController.accountEvents().subscribe { event -> this.onAccountEvent(event) }

    try {
      val profile = this.profilesController.profileCurrent()
      this.policyState = setupPolicyForProfile(this.logger, profile)
      this.executor.execute(OpCheckSyncStatusForProfile(
        logger = this.logger,
        httpCalls = this.httpCalls,
        profile = profile,
        evaluatePolicyInput = { input -> this.evaluatePolicyInput(profile, input) }))
    } catch (e: ProfileNoneCurrentException) {
      this.logger.debug("no profile is current, using an empty engine state")
      this.policyState = ReaderBookmarkPolicyState(accountState = mapOf(), bookmarksByAccount = mapOf())
    }
  }

  /**
   * The type of operations that can be performed in the controller.
   */

  abstract class ReaderBookmarkControllerOp(
    val logger: Logger) : Runnable {

    abstract fun runActual()

    override fun run() {
      try {
        this.logger.debug("{}: started", this.javaClass.simpleName)
        checkControllerThread()
        this.runActual()
      } finally {
        this.logger.debug("{}: finished", this.javaClass.simpleName)
      }
    }
  }

  /**
   * An operation that checks the sync status of all accounts that support syncing
   * in a given profile.
   */

  private class OpCheckSyncStatusForProfile(
    logger: Logger,
    private val httpCalls: ReaderBookmarkHTTPCallsType,
    private val profile: ProfileReadableType,
    private val evaluatePolicyInput: (ReaderBookmarkPolicyInput) -> Unit)
    : ReaderBookmarkControllerOp(logger) {

    override fun runActual() {
      this.logger.debug("[{}]: syncing all accounts", this.profile.id().id())

      return this.getPossiblySyncableAccounts(this.profile)
        .forEach(this::checkSyncingIsEnabledForEntry)
    }

    private fun checkSyncingIsEnabledForEntry(entry: Map.Entry<AccountID, SyncableAccount?>) {
      val account = entry.value
      return if (account != null) {
        OpCheckSyncStatusForAccount(
          logger = this.logger,
          httpCalls = this.httpCalls,
          profile = this.profile,
          syncableAccount = account,
          evaluatePolicyInput = this.evaluatePolicyInput)
          .run()
      } else {

      }
    }

    private fun getPossiblySyncableAccounts(
      profile: ProfileReadableType): Map<AccountID, SyncableAccount?> {
      this.logger.debug("[{}]: querying accounts for syncing", profile.id().id())
      return profile.accounts().mapValues { entry -> accountSupportsSyncing(entry.value) }
    }
  }

  /**
   * An operation that checks the sync status of a specific account in a given profile.
   */

  private class OpCheckSyncStatusForAccount(
    logger: Logger,
    private val httpCalls: ReaderBookmarkHTTPCallsType,
    private val profile: ProfileReadableType,
    private val syncableAccount: SyncableAccount,
    private val evaluatePolicyInput: (ReaderBookmarkPolicyInput) -> Unit)
    : ReaderBookmarkControllerOp(logger) {

    override fun runActual() {
      this.logger.debug(
        "[{}]: checking sync status for account {}",
        this.profile.id().id(),
        this.syncableAccount.account.id().id())

      val syncable =
        this.checkSyncingIsEnabledForAccount(this.profile, this.syncableAccount)

      this.evaluatePolicyInput.invoke(SyncingEnabled(
        accountID = this.syncableAccount.account.id(),
        enabled = syncable != null))
    }

    private fun checkSyncingIsEnabledForAccount(
      profile: ProfileReadableType,
      account: SyncableAccount): SyncableAccount? {

      return try {
        this.logger.debug(
          "[{}]: checking account {} has syncing enabled",
          profile.id().id(),
          account.account.id().id())

        if (this.httpCalls.syncingIsEnabled(account.settingsURI, account.credentials)) {
          this.logger.debug(
            "[{}]: account {} has syncing enabled",
            profile.id().id(),
            account.account.id().id())
          account
        } else {
          this.logger.debug(
            "[{}]: account {} has does not have syncing enabled",
            profile.id().id(),
            account.account.id().id())
          null
        }
      } catch (e: Exception) {
        this.logger.error("error checking account for syncing: ", e)
        return null
      }
    }
  }

  /**
   * An operation that checks the sync status of all accounts that support syncing
   * in a given profile.
   */

  private class OpSyncAccountInProfile(
    logger: Logger,
    private val httpCalls: ReaderBookmarkHTTPCallsType,
    private val bookmarkEventsOut: ObservableType<ReaderBookmarkEvent>,
    private val objectMapper: ObjectMapper,
    private val profile: ProfileReadableType,
    private val accountID: AccountID,
    private val evaluatePolicyInput: (ReaderBookmarkPolicyInput) -> Unit)
    : ReaderBookmarkControllerOp(logger) {

    override fun runActual() {
      this.logger.debug("[{}]: syncing account {}",
        profile.id().id(),
        accountID.id())

      val syncable =
        accountSupportsSyncing(this.profile.account(this.accountID))

      if (syncable == null) {
        this.logger.error("account is no longer syncable!")
        return
      }

      this.bookmarkEventsOut.send(ReaderBookmarkSyncStarted(syncable.account.id()))

      val bookmarks: List<ReaderBookmark> =
        try {
          this.httpCalls.bookmarksGet(syncable.annotationsURI, syncable.credentials)
            .map { annotation -> parseBookmarkOrNull(this.logger, this.objectMapper, annotation) }
            .filterNotNull()
        } catch (e: Exception) {
          this.logger.error("[{}]: could not receive bookmarks for account {}: ",
            this.profile.id().id(),
            syncable.account.id().id(),
            e)
          listOf()
        }

      this.logger.debug("[{}]: received {} bookmarks", profile.id().id(), bookmarks.size)
      for (bookmark in bookmarks) {
        this.evaluatePolicyInput(BookmarkReceived(syncable.account.id(), bookmark))
      }

      this.bookmarkEventsOut.send(ReaderBookmarkSyncFinished(syncable.account.id()))
    }
  }

  /**
   * An operation that remotely deletes a bookmark.
   */

  private class OpRemotelyDeleteBookmark(
    logger: Logger,
    private val httpCalls: ReaderBookmarkHTTPCallsType,
    private val profile: ProfileReadableType,
    private val accountID: AccountID,
    private val bookmark: ReaderBookmark)
    : ReaderBookmarkControllerOp(logger) {

    override fun runActual() {
      try {
        this.logger.debug(
          "[{}]: remote deleting bookmark {}",
          this.profile.id().id(),
          this.bookmark.bookmarkId.value)

        if (this.bookmark.uri == null) {
          this.logger.debug(
            "[{}]: cannot remotely delete bookmark {} because it has no URI",
            this.profile.id().id(),
            this.bookmark.bookmarkId.value)
          return
        }

        val syncInfo = accountSupportsSyncing(this.profile.account(this.accountID))
        if (syncInfo == null) {
          this.logger.debug(
            "[{}]: cannot remotely delete bookmark {} because the account is not syncable",
            this.profile.id().id(),
            this.bookmark.bookmarkId.value)
          return
        }

        this.httpCalls.bookmarkDelete(
          bookmarkURI = this.bookmark.uri,
          credentials = syncInfo.credentials)

      } catch (e: Exception) {
        this.logger.error("error sending bookmark: ", e)
      }
    }
  }

  /**
   * An operation that remotely sends a bookmark.
   */

  private class OpRemotelySendBookmark(
    logger: Logger,
    private val httpCalls: ReaderBookmarkHTTPCallsType,
    private val profile: ProfileReadableType,
    private val objectMapper: ObjectMapper,
    private val accountID: AccountID,
    private val bookmark: ReaderBookmark,
    private val evaluatePolicyInput: (ReaderBookmarkPolicyInput) -> Unit)
    : ReaderBookmarkControllerOp(logger) {

    override fun runActual() {
      try {
        this.logger.debug("[{}]: remote sending bookmark {}",
          this.profile.id().id(),
          this.bookmark.bookmarkId.value)

        val syncInfo = accountSupportsSyncing(this.profile.account(this.accountID))
        if (syncInfo == null) {
          this.logger.debug(
            "[{}]: cannot remotely send bookmark {} because the account is not syncable",
            this.profile.id().id(),
            this.bookmark.bookmarkId.value)
          return
        }

        this.httpCalls.bookmarkAdd(
          annotationsURI = syncInfo.annotationsURI,
          credentials = syncInfo.credentials,
          bookmark = BookmarkAnnotations.fromBookmark(this.objectMapper, this.bookmark))

        this.evaluatePolicyInput(BookmarkSaved(this.accountID, this.bookmark))
      } catch (e: Exception) {
        this.logger.error("error sending bookmark: ", e)
      }
    }
  }

  /**
   * An operation that locally saves a bookmark.
   */

  private class OpLocallySaveBookmark(
    logger: Logger,
    private val profile: ProfileReadableType,
    private val bookmarkEventsOut: ObservableType<ReaderBookmarkEvent>,
    private val accountID: AccountID,
    private val bookmark: ReaderBookmark)
    : ReaderBookmarkControllerOp(logger) {

    override fun runActual() {
      try {
        this.logger.debug(
          "[{}]: locally saving bookmark {}",
          this.profile.id().id(),
          this.bookmark.bookmarkId.value)

        val account = this.profile.account(this.accountID)
        val books = account.bookDatabase()
        val entry = books.entry(this.bookmark.book)
        val handle = entry.findFormatHandle(BookDatabaseEntryFormatHandleEPUB::class.java)
        if (handle != null) {
          handle.setBookmarks(handle.format.bookmarks.plus(this.bookmark))
          this.bookmarkEventsOut.send(ReaderBookmarkSaved(this.accountID, this.bookmark))
        } else {
          this.logger.debug("[{}]: unable to save bookmark; no format handle", this.profile.id().id())
        }
      } catch (e: Exception) {
        this.logger.error("error saving bookmark locally: ", e)
      }
    }
  }

  /**
   * An operation that handles user-created bookmarks.
   */

  private class OpUserCreatedABookmark(
    logger: Logger,
    private val accountID: AccountID,
    private val bookmark: ReaderBookmark,
    private val evaluatePolicyInput: (ReaderBookmarkPolicyInput) -> Unit)
    : ReaderBookmarkControllerOp(logger) {

    override fun runActual() {
      this.evaluatePolicyInput.invoke(BookmarkCreated(this.accountID, this.bookmark))
    }
  }

  /**
   * An operation that handles user-created bookmarks.
   */

  private class OpUserRequestedDeletionOfABookmark(
    logger: Logger,
    private val accountID: AccountID,
    private val bookmark: ReaderBookmark,
    private val evaluatePolicyInput: (ReaderBookmarkPolicyInput) -> Unit)
    : ReaderBookmarkControllerOp(logger) {

    override fun runActual() {
      this.evaluatePolicyInput.invoke(BookmarkDeleteRequested(this.accountID, this.bookmark))
    }
  }

  private data class SyncableAccount(
    val account: AccountType,
    val settingsURI: URI,
    val annotationsURI: URI,
    val credentials: AccountAuthenticationCredentials)

  private fun reconfigureForProfile(profile: ProfileReadableType) {
    this.logger.debug("[{}]: configuring bookmark controller for profile", profile.id().id())
    this.policyState = setupPolicyForProfile(this.logger, profile)
  }

  private fun onProfileEvent(event: ProfileEvent) {
    try {
      val currentProfile = this.profilesController.profileCurrent()
      this.executor.execute { this.reconfigureForProfile(currentProfile) }
    } catch (e: ProfileNoneCurrentException) {
      this.logger.error("onProfileEvent: no profile is current")
    }
  }

  private fun onAccountEvent(event: AccountEvent) {
    this.executor.execute {
      val profile = this.profilesController.profileCurrent()

      if (event is AccountCreationSucceeded) {
        this.onEventAccountCreated(profile, event)
      } else if (event is AccountDeletionSucceeded) {
        this.onEventAccountDeleted(profile, event)
      } else if (event is AccountEventUpdated) {
        this.onEventAccountUpdated(profile, event)
      }
    }
  }

  private fun onEventAccountUpdated(
    profile: ProfileReadableType,
    event: AccountEventUpdated) {
    this.logger.debug("[{}]: account updated", profile.id().id())

    val account =
      profile.account(event.accountID)

    val accountStateCurrent =
      ReaderBookmarkPolicy.evaluatePolicy(ReaderBookmarkPolicy.getAccountState(event.accountID),
        this.policyState)
        .result

    val accountState =
      ReaderBookmarkPolicyAccountState(
        accountID = account.id(),
        syncSupportedByAccount = account.provider().supportsSimplyESynchronization(),
        syncEnabledOnServer = if (accountStateCurrent != null) accountStateCurrent.syncEnabledOnServer else false,
        syncPermittedByUser = account.preferences().bookmarkSyncingPermitted)

    this.evaluatePolicyInput(profile, AccountUpdated(accountState))
  }

  private fun onEventAccountDeleted(
    profile: ProfileReadableType,
    event: AccountDeletionSucceeded) {
    checkControllerThread()
    this.logger.debug("[{}]: account deleted", profile.id().id())
    this.evaluatePolicyInput(profile, AccountDeleted(event.id()))
  }

  private fun onEventAccountCreated(
    profile: ProfileReadableType,
    event: AccountCreationSucceeded) {
    checkControllerThread()
    this.logger.debug("[{}]: account created", profile.id().id())

    val account =
      profile.account(event.id())

    val accountState =
      ReaderBookmarkPolicyAccountState(
        accountID = account.id(),
        syncSupportedByAccount = account.provider().supportsSimplyESynchronization(),
        syncEnabledOnServer = false,
        syncPermittedByUser = account.preferences().bookmarkSyncingPermitted)

    this.evaluatePolicyInput(profile, AccountCreated(accountState))
  }

  private fun evaluatePolicyInput(
    profile: ProfileReadableType,
    input: ReaderBookmarkPolicyInput) {
    checkControllerThread()

    val result =
      ReaderBookmarkPolicy.evaluatePolicy(
        ReaderBookmarkPolicy.evaluateInput(input),
        this.policyState)

    this.policyState = result.newState
    result.outputs.forEach { output -> this.evaluatePolicyOutput(profile, output) }
  }

  private fun evaluatePolicyOutput(
    profile: ProfileReadableType,
    output: ReaderBookmarkPolicyOutput) {
    checkControllerThread()

    this.logger.debug("[{}]: evaluatePolicyOutput: {}", profile.id().id(), output)

    return when (output) {
      is Command.LocallySaveBookmark ->
        OpLocallySaveBookmark(
          logger = this.logger,
          profile = profile,
          bookmarkEventsOut = this.bookmarkEventsOut,
          accountID = output.accountID,
          bookmark = output.bookmark)
          .run()

      is Command.RemotelySendBookmark ->
        OpRemotelySendBookmark(
          logger = this.logger,
          httpCalls = this.httpCalls,
          profile = profile,
          accountID = output.accountID,
          objectMapper = this.objectMapper,
          evaluatePolicyInput = { input -> this.evaluatePolicyInput(profile, input) },
          bookmark = output.bookmark)
          .run()

      is Command.RemotelyFetchBookmarks ->
        OpSyncAccountInProfile(
          logger = this.logger,
          httpCalls = this.httpCalls,
          profile = profile,
          accountID = output.accountID,
          objectMapper = this.objectMapper,
          bookmarkEventsOut = this.bookmarkEventsOut,
          evaluatePolicyInput = { input -> this.evaluatePolicyInput(profile, input) })
          .run()

      is Command.RemotelyDeleteBookmark ->
        OpRemotelyDeleteBookmark(
          logger = this.logger,
          httpCalls = this.httpCalls,
          profile = profile,
          accountID = output.accountID,
          bookmark = output.bookmark)
          .run()

      is ReaderBookmarkPolicyOutput.Event.LocalBookmarkAlreadyExists ->
        throw UnimplementedCodeException()
    }
  }

  override fun onBookmarkCreated(
    accountID: AccountID,
    bookmark: ReaderBookmark): FluentFuture<Unit> {

    return try {
      val profile = this.profilesController.profileCurrent()
      FluentFuture.from(this.executor.submit<Unit> {
        OpUserCreatedABookmark(
          logger = this.logger,
          evaluatePolicyInput = { input -> this.evaluatePolicyInput(profile, input) },
          accountID = accountID,
          bookmark = bookmark)
      })
    } catch (e: ProfileNoneCurrentException) {
      this.logger.error("onBookmarkCreated: no profile is current: ", e)
      FluentFuture.from(Futures.immediateFailedFuture(e))
    }
  }

  override fun onBookmarkDeleteRequested(
    accountID: AccountID,
    bookmark: ReaderBookmark): FluentFuture<Unit> {

    return try {
      val profile = this.profilesController.profileCurrent()
      FluentFuture.from(this.executor.submit<Unit> {
        OpUserRequestedDeletionOfABookmark(
          logger = this.logger,
          evaluatePolicyInput = { input -> this.evaluatePolicyInput(profile, input) },
          accountID = accountID,
          bookmark = bookmark)
      })
    } catch (e: ProfileNoneCurrentException) {
      this.logger.error("onBookmarkDeleteRequested: no profile is current: ", e)
      FluentFuture.from(Futures.immediateFailedFuture(e))
    }
  }

  companion object {

    private fun setupPolicyForProfile(
      logger: Logger,
      profile: ProfileReadableType): ReaderBookmarkPolicyState {
      logger.debug("[{}]: configuring bookmark policy state", profile.id().id())
      return ReaderBookmarkPolicyState.create(
          initialAccounts = this.accountStatesForProfile(profile),
          locallySaved = this.bookmarksForProfile(logger, profile))
    }

    private fun accountStatesForProfile(
      profile: ProfileReadableType): Set<ReaderBookmarkPolicyAccountState> {
      return profile.accounts()
        .map { pair -> this.accountStateForAccount(pair.value) }
        .toSet()
    }

    private fun accountStateForAccount(account: AccountType): ReaderBookmarkPolicyAccountState {
      return ReaderBookmarkPolicyAccountState(
        accountID = account.id(),
        syncSupportedByAccount = account.provider().supportsSimplyESynchronization(),
        syncEnabledOnServer = false,
        syncPermittedByUser = account.preferences().bookmarkSyncingPermitted)
    }

    private fun bookmarksForProfile(
      logger: Logger,
      profile: ProfileReadableType): Map<AccountID, Set<ReaderBookmark>> {
      val books = mutableMapOf<AccountID, Set<ReaderBookmark>>()
      val accounts = profile.accounts().values
      for (account in accounts) {
        books.put(account.id(), this.bookmarksForAccount(account))
      }
      logger.debug("[{}]: collected {} bookmarks for profile", profile.id().id(), books.size)
      return books
    }

    private fun bookmarksForAccount(account: AccountType): Set<ReaderBookmark> {
      val bookDatabase = account.bookDatabase()
      val books = mutableSetOf<ReaderBookmark>()

      for (bookId in bookDatabase.books()) {
        val entry = bookDatabase.entry(bookId)
        val handle =
          entry.findFormatHandle(BookDatabaseEntryFormatHandleEPUB::class.java)

        if (handle != null) {
          books.addAll(handle.format.bookmarks)
          val lastRead = handle.format.lastReadLocation
          if (lastRead != null) {
            books.add(lastRead)
          }
        }
      }

      return books
    }

    private fun parseBookmarkOrNull(
      logger: Logger,
      objectMapper: ObjectMapper,
      annotation: BookmarkAnnotation): ReaderBookmark? {
      return try {
        BookmarkAnnotations.toBookmark(objectMapper, annotation)
      } catch (e: Exception) {
        logger.error("unable to parse bookmark: ", e)
        null
      }
    }

    private fun accountSupportsSyncing(account: AccountType): SyncableAccount? {
      return if (account.provider().supportsSimplyESynchronization()) {
        val settingsOpt = account.provider().patronSettingsURI()
        val annotationsOpt = account.provider().annotationsURI()
        val credentialsOpt = account.credentials()
        if (credentialsOpt is Some<AccountAuthenticationCredentials>
          && settingsOpt is Some<URI>
          && annotationsOpt is Some<URI>) {
          return SyncableAccount(
            account = account,
            settingsURI = settingsOpt.get(),
            annotationsURI = annotationsOpt.get(),
            credentials = credentialsOpt.get())
        } else {
          null
        }
      } else {
        null
      }
    }

    private fun checkControllerThread() {
      if (!(Thread.currentThread() is ReaderBookmarkControllerThread)) {
        throw IllegalStateException("Current thread is not the controller thread")
      }
    }

    /**
     * Create a new bookmark controller.
     */

    fun create(
      threads: (Runnable) -> Thread,
      events: ObservableType<ReaderBookmarkEvent>,
      httpCalls: ReaderBookmarkHTTPCallsType,
      profilesController: ProfilesControllerType): ReaderBookmarkControllerType {
      return ReaderBookmarkController(threads, httpCalls, events, profilesController)
    }
  }
}
