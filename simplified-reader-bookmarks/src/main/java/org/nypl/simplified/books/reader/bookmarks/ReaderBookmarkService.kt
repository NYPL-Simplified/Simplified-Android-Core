package org.nypl.simplified.books.reader.bookmarks

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.util.concurrent.FluentFuture
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListeningScheduledExecutorService
import com.google.common.util.concurrent.MoreExecutors
import io.reactivex.Observable
import io.reactivex.subjects.Subject
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountEventCreation.AccountEventCreationSucceeded
import org.nypl.simplified.accounts.api.AccountEventDeletion.AccountEventDeletionSucceeded
import org.nypl.simplified.accounts.api.AccountEventLoginStateChanged
import org.nypl.simplified.accounts.api.AccountEventUpdated
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.api.AccountLoginState
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.api.Bookmark
import org.nypl.simplified.books.api.BookmarkKind.ReaderBookmarkExplicit
import org.nypl.simplified.books.api.BookmarkKind.ReaderBookmarkLastReadLocation
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleEPUB
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkPolicyInput.Event.Local.AccountCreated
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkPolicyInput.Event.Local.AccountDeleted
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkPolicyInput.Event.Local.AccountLoggedIn
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkPolicyInput.Event.Local.AccountUpdated
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkPolicyInput.Event.Local.BookmarkCreated
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkPolicyInput.Event.Local.BookmarkDeleteRequested
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkPolicyInput.Event.Remote.BookmarkReceived
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkPolicyInput.Event.Remote.BookmarkSaved
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkPolicyInput.Event.Remote.SyncingEnabled
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkPolicyOutput.Command
import org.nypl.simplified.profiles.api.ProfileEvent
import org.nypl.simplified.profiles.api.ProfileNoneCurrentException
import org.nypl.simplified.profiles.api.ProfileReadableType
import org.nypl.simplified.profiles.api.ProfileSelection
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.reader.bookmarks.api.BookmarkAnnotation
import org.nypl.simplified.reader.bookmarks.api.BookmarkAnnotations
import org.nypl.simplified.reader.bookmarks.api.ReaderBookmarkEvent
import org.nypl.simplified.reader.bookmarks.api.ReaderBookmarkEvent.ReaderBookmarkSaved
import org.nypl.simplified.reader.bookmarks.api.ReaderBookmarkEvent.ReaderBookmarkSyncFinished
import org.nypl.simplified.reader.bookmarks.api.ReaderBookmarkEvent.ReaderBookmarkSyncStarted
import org.nypl.simplified.reader.bookmarks.api.ReaderBookmarkHTTPCallsType
import org.nypl.simplified.reader.bookmarks.api.ReaderBookmarkServiceProviderType
import org.nypl.simplified.reader.bookmarks.api.ReaderBookmarkServiceProviderType.Requirements
import org.nypl.simplified.reader.bookmarks.api.ReaderBookmarkServiceType
import org.nypl.simplified.reader.bookmarks.api.ReaderBookmarkServiceUsableType.SyncEnableResult
import org.nypl.simplified.reader.bookmarks.api.ReaderBookmarkServiceUsableType.SyncEnableResult.SYNC_DISABLED
import org.nypl.simplified.reader.bookmarks.api.ReaderBookmarkServiceUsableType.SyncEnableResult.SYNC_ENABLED
import org.nypl.simplified.reader.bookmarks.api.ReaderBookmarkServiceUsableType.SyncEnableResult.SYNC_ENABLE_NOT_SUPPORTED
import org.nypl.simplified.reader.bookmarks.api.ReaderBookmarks
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.concurrent.Callable
import java.util.concurrent.Executors

/**
 * The default implementation of the bookmark service interface.
 *
 * This implementation generally makes the assumption that bookmark syncing is not particularly
 * critical and so simply logs and otherwise ignores errors. Syncing is treated as best-effort and
 * all failures are assumed to be temporary.
 */

class ReaderBookmarkService private constructor(
  private val threads: (Runnable) -> Thread,
  private val httpCalls: ReaderBookmarkHTTPCallsType,
  private val bookmarkEventsOut: Subject<ReaderBookmarkEvent>,
  private val profilesController: ProfilesControllerType
) : ReaderBookmarkServiceType {

  /**
   * A trivial Thread subclass for efficient checks to determine whether or not the current
   * thread is a service thread.
   */

  private class ReaderBookmarkServiceThread(thread: Thread) : Thread(thread)

  private val executor: ListeningScheduledExecutorService =
    MoreExecutors.listeningDecorator(
      Executors.newScheduledThreadPool(1) { runnable ->
        ReaderBookmarkServiceThread(this.threads.invoke(runnable))
      }
    )

  override fun close() {
    this.executor.shutdown()
  }

  override val bookmarkEvents: Observable<ReaderBookmarkEvent>
    get() = this.bookmarkEventsOut

  private val logger = LoggerFactory.getLogger(ReaderBookmarkService::class.java)
  private val objectMapper = ObjectMapper()

  @Volatile
  private var policyState: ReaderBookmarkPolicyState

  init {
    this.profilesController.profileEvents().subscribe { event -> this.onProfileEvent(event) }
    this.profilesController.accountEvents().subscribe { event -> this.onAccountEvent(event) }

    try {
      val profile = this.profilesController.profileCurrent()
      this.policyState = setupPolicyForProfile(this.logger, profile)
      this.executor.submit(
        OpCheckSyncStatusForProfile(
          logger = this.logger,
          httpCalls = this.httpCalls,
          profile = profile,
          evaluatePolicyInput = { input -> this.evaluatePolicyInput(profile, input) }
        )
      )
    } catch (e: ProfileNoneCurrentException) {
      this.logger.debug("no profile is current, using an empty engine state")
      this.policyState =
        ReaderBookmarkPolicyState(accountState = mapOf(), bookmarksByAccount = mapOf())
    }
  }

  /**
   * The type of operations that can be performed in the controller.
   */

  abstract class ReaderBookmarkControllerOp<T>(
    val logger: Logger
  ) : Callable<T> {

    abstract fun runActual(): T

    override fun call(): T {
      try {
        this.logger.debug("{}: started", this.javaClass.simpleName)
        checkServiceThread()
        return this.runActual()
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
    private val evaluatePolicyInput: (ReaderBookmarkPolicyInput) -> Unit
  ) : ReaderBookmarkControllerOp<Unit>(logger) {

    override fun runActual() {
      this.logger.debug("[{}]: syncing all accounts", this.profile.id.uuid)

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
          evaluatePolicyInput = this.evaluatePolicyInput
        ).call()
      } else {
      }
    }

    private fun getPossiblySyncableAccounts(
      profile: ProfileReadableType
    ): Map<AccountID, SyncableAccount?> {
      this.logger.debug("[{}]: querying accounts for syncing", profile.id.uuid)
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
    private val evaluatePolicyInput: (ReaderBookmarkPolicyInput) -> Unit
  ) : ReaderBookmarkControllerOp<Unit>(logger) {

    override fun runActual() {
      this.logger.debug(
        "[{}]: checking sync status for account {}",
        this.profile.id.uuid,
        this.syncableAccount.account.id
      )

      val syncable =
        this.checkSyncingIsEnabledForAccount(this.profile, this.syncableAccount)

      this.syncableAccount.account.setPreferences(
        this.syncableAccount.account.preferences.copy(
          bookmarkSyncingPermitted = syncable != null
        )
      )

      this.evaluatePolicyInput.invoke(
        SyncingEnabled(
          accountID = this.syncableAccount.account.id,
          enabled = syncable != null
        )
      )
    }

    private fun checkSyncingIsEnabledForAccount(
      profile: ProfileReadableType,
      account: SyncableAccount
    ): SyncableAccount? {
      return try {
        this.logger.debug(
          "[{}]: checking account {} has syncing enabled",
          profile.id.uuid,
          account.account.id
        )

        if (this.httpCalls.syncingIsEnabled(account.settingsURI, account.credentials)) {
          this.logger.debug(
            "[{}]: account {} has syncing enabled",
            profile.id.uuid,
            account.account.id
          )
          account
        } else {
          this.logger.debug(
            "[{}]: account {} does not have syncing enabled",
            profile.id.uuid,
            account.account.id
          )
          null
        }
      } catch (e: Exception) {
        this.logger.error("error checking account for syncing: ", e)
        return null
      }
    }
  }

  /**
   * An operation that synchronizes bookmarks for all accounts that want it.
   */

  private class OpSyncAccountInProfile(
    logger: Logger,
    private val httpCalls: ReaderBookmarkHTTPCallsType,
    private val bookmarkEventsOut: Subject<ReaderBookmarkEvent>,
    private val objectMapper: ObjectMapper,
    private val profile: ProfileReadableType,
    private val accountID: AccountID,
    private val evaluatePolicyInput: (ReaderBookmarkPolicyInput) -> Unit
  ) : ReaderBookmarkControllerOp<Unit>(logger) {

    override fun runActual() {
      this.logger.debug(
        "[{}]: syncing account {}",
        this.profile.id.uuid,
        this.accountID
      )

      val syncable =
        accountSupportsSyncing(this.profile.account(this.accountID))

      if (syncable == null) {
        this.logger.error("account is no longer syncable!")
        return
      }

      this.bookmarkEventsOut.onNext(ReaderBookmarkSyncStarted(syncable.account.id))

      val bookmarks: List<Bookmark> =
        try {
          this.httpCalls.bookmarksGet(syncable.annotationsURI, syncable.credentials)
            .map { annotation -> parseBookmarkOrNull(this.logger, this.objectMapper, annotation) }
            .filterNotNull()
        } catch (e: Exception) {
          this.logger.error(
            "[{}]: could not receive bookmarks for account {}: ",
            this.profile.id.uuid,
            syncable.account.id,
            e
          )
          listOf()
        }

      this.logger.debug("[{}]: received {} bookmarks", this.profile.id.uuid, bookmarks.size)
      for (bookmark in bookmarks) {
        this.evaluatePolicyInput(BookmarkReceived(syncable.account.id, bookmark))
      }

      this.bookmarkEventsOut.onNext(ReaderBookmarkSyncFinished(syncable.account.id))
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
    private val bookmark: Bookmark
  ) : ReaderBookmarkControllerOp<Unit>(logger) {

    override fun runActual() {
      try {
        this.logger.debug(
          "[{}]: remote deleting bookmark {}",
          this.profile.id.uuid,
          this.bookmark.bookmarkId.value
        )

        val bookmarkURI = this.bookmark.uri
        if (bookmarkURI == null) {
          this.logger.debug(
            "[{}]: cannot remotely delete bookmark {} because it has no URI",
            this.profile.id.uuid,
            this.bookmark.bookmarkId.value
          )
          return
        }

        val syncInfo = accountSupportsSyncing(this.profile.account(this.accountID))
        if (syncInfo == null) {
          this.logger.debug(
            "[{}]: cannot remotely delete bookmark {} because the account is not syncable",
            this.profile.id.uuid,
            this.bookmark.bookmarkId.value
          )
          return
        }

        this.httpCalls.bookmarkDelete(
          bookmarkURI = bookmarkURI,
          credentials = syncInfo.credentials
        )
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
    private val bookmark: Bookmark,
    private val evaluatePolicyInput: (ReaderBookmarkPolicyInput) -> Unit
  ) : ReaderBookmarkControllerOp<Unit>(logger) {

    override fun runActual() {
      try {
        this.logger.debug(
          "[{}]: remote sending bookmark {}",
          this.profile.id.uuid,
          this.bookmark.bookmarkId.value
        )

        val syncInfo = accountSupportsSyncing(this.profile.account(this.accountID))
        if (syncInfo == null) {
          this.logger.debug(
            "[{}]: cannot remotely send bookmark {} because the account is not syncable",
            this.profile.id.uuid,
            this.bookmark.bookmarkId.value
          )
          return
        }

        this.httpCalls.bookmarkAdd(
          annotationsURI = syncInfo.annotationsURI,
          credentials = syncInfo.credentials,
          bookmark = BookmarkAnnotations.fromBookmark(this.objectMapper, this.bookmark)
        )

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
    private val bookmarkEventsOut: Subject<ReaderBookmarkEvent>,
    private val accountID: AccountID,
    private val bookmark: Bookmark
  ) : ReaderBookmarkControllerOp<Unit>(logger) {

    override fun runActual() {
      try {
        this.logger.debug(
          "[{}]: locally saving bookmark {}",
          this.profile.id.uuid,
          this.bookmark.bookmarkId.value
        )

        val account = this.profile.account(this.accountID)
        val books = account.bookDatabase
        val entry = books.entry(this.bookmark.book)
        val handle =
          entry.findFormatHandle(BookDatabaseEntryFormatHandleEPUB::class.java)

        if (handle != null) {
          when (this.bookmark.kind) {
            ReaderBookmarkLastReadLocation ->
              handle.setLastReadLocation(this.bookmark)
            ReaderBookmarkExplicit ->
              handle.setBookmarks(handle.format.bookmarks.plus(this.bookmark))
          }

          this.bookmarkEventsOut.onNext(ReaderBookmarkSaved(this.accountID, this.bookmark))
        } else {
          this.logger.debug("[{}]: unable to save bookmark; no format handle", this.profile.id.uuid)
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
    private val bookmark: Bookmark,
    private val evaluatePolicyInput: (ReaderBookmarkPolicyInput) -> Unit
  ) : ReaderBookmarkControllerOp<Unit>(logger) {

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
    private val bookmark: Bookmark,
    private val evaluatePolicyInput: (ReaderBookmarkPolicyInput) -> Unit
  ) : ReaderBookmarkControllerOp<Unit>(logger) {

    override fun runActual() {
      this.evaluatePolicyInput.invoke(BookmarkDeleteRequested(this.accountID, this.bookmark))
    }
  }

  /**
   * An operation that loads bookmarks.
   */

  private class OpUserRequestedBookmarks(
    logger: Logger,
    private val profile: ProfileReadableType,
    private val accountID: AccountID,
    private val book: BookID
  ) : ReaderBookmarkControllerOp<ReaderBookmarks>(logger) {

    override fun runActual(): ReaderBookmarks {
      try {
        this.logger.debug("[{}]: loading bookmarks", this.profile.id.uuid)

        val account = this.profile.account(this.accountID)
        val books = account.bookDatabase
        val entry = books.entry(this.book)
        val handle = entry.findFormatHandle(BookDatabaseEntryFormatHandleEPUB::class.java)
        if (handle != null) {
          return ReaderBookmarks(
            handle.format.lastReadLocation,
            handle.format.bookmarks
          )
        }
      } catch (e: Exception) {
        this.logger.error("error saving bookmark locally: ", e)
      }

      this.logger.debug("[{}]: returning empty bookmarks", this.profile.id.uuid)
      return ReaderBookmarks(null, listOf())
    }
  }

  /**
   * An operation that enables/disable syncing for an account.
   */

  private class OpEnableSync(
    logger: Logger,
    private val httpCalls: ReaderBookmarkHTTPCallsType,
    private val profile: ProfileReadableType,
    private val syncableAccount: SyncableAccount,
    private val enable: Boolean
  ) : ReaderBookmarkControllerOp<SyncEnableResult>(logger) {

    override fun runActual(): SyncEnableResult {
      this.logger.debug(
        "[{}]: {} syncing for account {}",
        this.profile.id.uuid,
        if (this.enable) "enabling" else "disabling",
        this.syncableAccount.account.id.uuid
      )

      this.httpCalls.syncingEnable(
        settingsURI = this.syncableAccount.settingsURI,
        credentials = this.syncableAccount.credentials,
        enabled = this.enable
      )

      this.syncableAccount.account.setPreferences(
        this.syncableAccount.account.preferences.copy(bookmarkSyncingPermitted = enable)
      )

      return when (enable) {
        true -> SYNC_ENABLED
        false -> SYNC_DISABLED
      }
    }
  }

  private data class SyncableAccount(
    val account: AccountType,
    val settingsURI: URI,
    val annotationsURI: URI,
    val credentials: AccountAuthenticationCredentials
  )

  private fun reconfigureForProfile(profile: ProfileReadableType) {
    this.logger.debug("[{}]: reconfiguring bookmark controller for profile", profile.id.uuid)
    this.policyState = setupPolicyForProfile(this.logger, profile)
    this.executor.submit(
      OpCheckSyncStatusForProfile(
        logger = this.logger,
        httpCalls = this.httpCalls,
        profile = profile,
        evaluatePolicyInput = { input -> this.evaluatePolicyInput(profile, input) }
      )
    )
  }

  private fun onProfileEvent(event: ProfileEvent) {
    if (event is ProfileSelection.ProfileSelectionInProgress) {
      try {
        val currentProfile = this.profilesController.profileCurrent()
        this.logger.debug("[{}]: a new profile was selected", currentProfile.id.uuid)
        this.executor.execute { this.reconfigureForProfile(currentProfile) }
      } catch (e: ProfileNoneCurrentException) {
        this.logger.error("onProfileEvent: no profile is current")
      }
    }
  }

  private fun onAccountEvent(event: AccountEvent) {
    this.executor.execute {
      val profile = this.profilesController.profileCurrent()

      if (event is AccountEventCreationSucceeded) {
        this.onEventAccountCreated(profile, event)
      } else if (event is AccountEventDeletionSucceeded) {
        this.onEventAccountDeleted(profile, event)
      } else if (event is AccountEventUpdated) {
        this.onEventAccountUpdated(profile, event)
      } else if (event is AccountEventLoginStateChanged) {
        this.onEventAccountEventLoginStateChanged(profile, event)
      }
    }
  }

  private fun onEventAccountEventLoginStateChanged(
    profile: ProfileReadableType,
    event: AccountEventLoginStateChanged
  ) {
    return when (event.state) {
      AccountLoginState.AccountNotLoggedIn,
      is AccountLoginState.AccountLoggingIn,
      is AccountLoginState.AccountLoginFailed,
      is AccountLoginState.AccountLoggingOut,
      is AccountLoginState.AccountLoggingInWaitingForExternalAuthentication,
      is AccountLoginState.AccountLogoutFailed -> {
        // We don't care about these
      }

      is AccountLoginState.AccountLoggedIn -> {
        this.logger.debug("[{}]: account {} logged in", profile.id.uuid, event.accountID.uuid)

        val account =
          profile.account(event.accountID)

        val accountStateCurrent =
          ReaderBookmarkPolicy.evaluatePolicy(
            ReaderBookmarkPolicy.getAccountState(event.accountID),
            this.policyState
          ).result

        val accountState =
          ReaderBookmarkPolicyAccountState(
            accountID = account.id,
            syncSupportedByAccount = account.loginState.credentials?.annotationsURI != null,
            syncEnabledOnServer = if (accountStateCurrent != null) accountStateCurrent.syncEnabledOnServer else false,
            syncPermittedByUser = account.preferences.bookmarkSyncingPermitted
          )

        this.evaluatePolicyInput(profile, AccountLoggedIn(accountState))
      }
    }
  }

  private fun onEventAccountUpdated(
    profile: ProfileReadableType,
    event: AccountEventUpdated
  ) {
    this.logger.debug("[{}]: account updated", profile.id.uuid)

    val account =
      profile.account(event.accountID)

    val accountStateCurrent =
      ReaderBookmarkPolicy.evaluatePolicy(
        ReaderBookmarkPolicy.getAccountState(event.accountID),
        this.policyState
      ).result

    val accountState =
      ReaderBookmarkPolicyAccountState(
        accountID = account.id,
        syncSupportedByAccount = account.loginState.credentials?.annotationsURI != null,
        syncEnabledOnServer = accountStateCurrent?.syncEnabledOnServer ?: false,
        syncPermittedByUser = account.preferences.bookmarkSyncingPermitted
      )

    this.evaluatePolicyInput(profile, AccountUpdated(accountState))
  }

  private fun onEventAccountDeleted(
    profile: ProfileReadableType,
    event: AccountEventDeletionSucceeded
  ) {
    checkServiceThread()
    this.logger.debug("[{}]: account deleted", profile.id.uuid)
    this.evaluatePolicyInput(profile, AccountDeleted(event.id))
  }

  private fun onEventAccountCreated(
    profile: ProfileReadableType,
    event: AccountEventCreationSucceeded
  ) {
    checkServiceThread()
    this.logger.debug("[{}]: account created", profile.id.uuid)

    val account = profile.account(event.id)

    val accountState =
      ReaderBookmarkPolicyAccountState(
        accountID = account.id,
        syncSupportedByAccount = account.loginState.credentials?.annotationsURI != null,
        syncEnabledOnServer = false,
        syncPermittedByUser = account.preferences.bookmarkSyncingPermitted
      )

    this.evaluatePolicyInput(profile, AccountCreated(accountState))
  }

  /**
   * The main method that evaluates the bookmark policy input and then acts upon the results.
   */

  private fun evaluatePolicyInput(
    profile: ProfileReadableType,
    input: ReaderBookmarkPolicyInput
  ) {
    checkServiceThread()

    val result =
      ReaderBookmarkPolicy.evaluatePolicy(
        ReaderBookmarkPolicy.evaluateInput(input),
        this.policyState
      )

    this.policyState = result.newState
    result.outputs.forEach { output -> this.evaluatePolicyOutput(profile, output) }
  }

  private fun evaluatePolicyOutput(
    profile: ProfileReadableType,
    output: ReaderBookmarkPolicyOutput
  ) {
    checkServiceThread()

    this.logger.debug("[{}]: evaluatePolicyOutput: {}", profile.id.uuid, output)

    return when (output) {
      is Command.LocallySaveBookmark ->
        OpLocallySaveBookmark(
          logger = this.logger,
          profile = profile,
          bookmarkEventsOut = this.bookmarkEventsOut,
          accountID = output.accountID,
          bookmark = output.bookmark
        )
          .call()

      is Command.RemotelySendBookmark ->
        OpRemotelySendBookmark(
          logger = this.logger,
          httpCalls = this.httpCalls,
          profile = profile,
          accountID = output.accountID,
          objectMapper = this.objectMapper,
          evaluatePolicyInput = { input -> this.evaluatePolicyInput(profile, input) },
          bookmark = output.bookmark
        )
          .call()

      is Command.RemotelyFetchBookmarks ->
        OpSyncAccountInProfile(
          logger = this.logger,
          httpCalls = this.httpCalls,
          profile = profile,
          accountID = output.accountID,
          objectMapper = this.objectMapper,
          bookmarkEventsOut = this.bookmarkEventsOut,
          evaluatePolicyInput = { input -> this.evaluatePolicyInput(profile, input) }
        )
          .call()

      is Command.RemotelyDeleteBookmark ->
        OpRemotelyDeleteBookmark(
          logger = this.logger,
          httpCalls = this.httpCalls,
          profile = profile,
          accountID = output.accountID,
          bookmark = output.bookmark
        )
          .call()

      is ReaderBookmarkPolicyOutput.Event.LocalBookmarkAlreadyExists ->
        this.logger.warn("local bookmark already exists: {}", output.bookmark.bookmarkId)
    }
  }

  override fun bookmarkSyncEnable(
    accountID: AccountID,
    enabled: Boolean
  ): FluentFuture<SyncEnableResult> {
    return try {
      val profile =
        this.profilesController.profileCurrent()
      val syncable =
        accountSupportsSyncing(profile.account(accountID))

      if (syncable == null) {
        this.logger.error("bookmarkSyncEnable: account does not support syncing")
        return FluentFuture.from(Futures.immediateFuture(SYNC_ENABLE_NOT_SUPPORTED))
      }

      val opEnable =
        OpEnableSync(
          logger = this.logger,
          httpCalls = this.httpCalls,
          profile = profile,
          syncableAccount = syncable,
          enable = enabled
        )

      val opCheck =
        OpCheckSyncStatusForProfile(
          logger = this.logger,
          httpCalls = this.httpCalls,
          profile = profile,
          evaluatePolicyInput = { input -> this.evaluatePolicyInput(profile, input) }
        )

      FluentFuture.from(this.executor.submit(opEnable))
        .transform(
          { result ->
            opCheck.call()
            result
          },
          this.executor
        )
    } catch (e: ProfileNoneCurrentException) {
      this.logger.error("bookmarkSyncEnable: no profile is current: ", e)
      FluentFuture.from(Futures.immediateFailedFuture(e))
    }
  }

  override fun bookmarkCreate(
    accountID: AccountID,
    bookmark: Bookmark
  ): FluentFuture<Unit> {
    return try {
      val profile = this.profilesController.profileCurrent()
      FluentFuture.from(
        this.executor.submit(
          OpUserCreatedABookmark(
            logger = this.logger,
            evaluatePolicyInput = { input -> this.evaluatePolicyInput(profile, input) },
            accountID = accountID,
            bookmark = bookmark
          )
        )
      )
    } catch (e: ProfileNoneCurrentException) {
      this.logger.error("bookmarkCreate: no profile is current: ", e)
      FluentFuture.from(Futures.immediateFailedFuture(e))
    }
  }

  override fun bookmarkDelete(
    accountID: AccountID,
    bookmark: Bookmark
  ): FluentFuture<Unit> {
    return try {
      val profile = this.profilesController.profileCurrent()
      FluentFuture.from(
        this.executor.submit(
          OpUserRequestedDeletionOfABookmark(
            logger = this.logger,
            evaluatePolicyInput = { input -> this.evaluatePolicyInput(profile, input) },
            accountID = accountID,
            bookmark = bookmark
          )
        )
      )
    } catch (e: ProfileNoneCurrentException) {
      this.logger.error("bookmarkDelete: no profile is current: ", e)
      FluentFuture.from(Futures.immediateFailedFuture(e))
    }
  }

  override fun bookmarkLoad(
    accountID: AccountID,
    book: BookID
  ): FluentFuture<ReaderBookmarks> {
    return try {
      val profile = this.profilesController.profileCurrent()
      FluentFuture.from(
        this.executor.submit(
          OpUserRequestedBookmarks(
            logger = this.logger,
            accountID = accountID,
            profile = profile,
            book = book
          )
        )
      )
    } catch (e: ProfileNoneCurrentException) {
      this.logger.error("bookmarkLoad: no profile is current: ", e)
      FluentFuture.from(Futures.immediateFailedFuture(e))
    }
  }

  companion object : ReaderBookmarkServiceProviderType {

    private fun setupPolicyForProfile(
      logger: Logger,
      profile: ProfileReadableType
    ): ReaderBookmarkPolicyState {
      logger.debug("[{}]: configuring bookmark policy state", profile.id.uuid)
      return ReaderBookmarkPolicyState.create(
        initialAccounts = this.accountStatesForProfile(profile),
        locallySaved = this.bookmarksForProfile(logger, profile)
      )
    }

    private fun accountStatesForProfile(
      profile: ProfileReadableType
    ): Set<ReaderBookmarkPolicyAccountState> {
      return profile.accounts()
        .map { pair -> this.accountStateForAccount(pair.value) }
        .toSet()
    }

    private fun accountStateForAccount(account: AccountType): ReaderBookmarkPolicyAccountState {
      return ReaderBookmarkPolicyAccountState(
        accountID = account.id,
        syncSupportedByAccount = account.loginState.credentials?.annotationsURI != null,
        syncEnabledOnServer = false,
        syncPermittedByUser = account.preferences.bookmarkSyncingPermitted
      )
    }

    private fun bookmarksForProfile(
      logger: Logger,
      profile: ProfileReadableType
    ): Map<AccountID, Set<Bookmark>> {
      val books = mutableMapOf<AccountID, Set<Bookmark>>()
      val accounts = profile.accounts().values
      for (account in accounts) {
        books.put(account.id, this.bookmarksForAccount(account))
      }
      logger.debug("[{}]: collected {} bookmarks for profile", profile.id.uuid, books.size)
      return books
    }

    private fun bookmarksForAccount(account: AccountType): Set<Bookmark> {
      val bookDatabase = account.bookDatabase
      val books = mutableSetOf<Bookmark>()

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
      annotation: BookmarkAnnotation
    ): Bookmark? {
      return try {
        BookmarkAnnotations.toBookmark(objectMapper, annotation)
      } catch (e: Exception) {
        logger.error("unable to parse bookmark: ", e)
        null
      }
    }

    private fun accountSupportsSyncing(account: AccountType): SyncableAccount? {
      val annotationsURI = account.loginState.credentials?.annotationsURI

      return if (annotationsURI != null) {
        val settingsOpt = account.provider.patronSettingsURI
        val credentials = account.loginState.credentials
        if (credentials != null && settingsOpt != null) {
          return SyncableAccount(
            account = account,
            settingsURI = settingsOpt,
            annotationsURI = annotationsURI,
            credentials = credentials
          )
        } else {
          null
        }
      } else {
        null
      }
    }

    private fun checkServiceThread() {
      if (Thread.currentThread() !is ReaderBookmarkServiceThread) {
        throw IllegalStateException("Current thread is not the service thread")
      }
    }

    override fun createService(
      requirements: Requirements
    ): ReaderBookmarkServiceType {
      return ReaderBookmarkService(
        threads = requirements.threads,
        httpCalls = requirements.httpCalls,
        bookmarkEventsOut = requirements.events,
        profilesController = requirements.profilesController
      )
    }
  }
}
