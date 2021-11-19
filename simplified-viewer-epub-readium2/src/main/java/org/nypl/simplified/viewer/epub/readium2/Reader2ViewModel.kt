package org.nypl.simplified.viewer.epub.readium2

import android.content.pm.ApplicationInfo
import android.webkit.WebView
import androidx.lifecycle.ViewModel
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import hu.akarnokd.rxjava2.subjects.UnicastWorkSubject
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import org.joda.time.LocalDateTime
import org.librarysimplified.r2.api.SR2Bookmark
import org.librarysimplified.r2.api.SR2Command
import org.librarysimplified.r2.api.SR2ControllerType
import org.librarysimplified.r2.api.SR2Event
import org.librarysimplified.r2.views.SR2ReaderViewEvent
import org.librarysimplified.r2.views.SR2ReaderViewModel
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.analytics.api.AnalyticsEvent
import org.nypl.simplified.analytics.api.AnalyticsType
import org.nypl.simplified.profiles.api.ProfileReadableType
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.reader.bookmarks.api.ReaderBookmarkServiceType
import java.util.concurrent.Executors

internal class Reader2ViewModel(
  private val applicationInfo: ApplicationInfo,
  private val parameters: Reader2ActivityParameters,
  private val profilesController: ProfilesControllerType,
  private val bookmarkService: ReaderBookmarkServiceType,
  private val analyticsService: AnalyticsType,
  private val readerModel: SR2ReaderViewModel
) : ViewModel() {

  private val ioExecutor: ListeningExecutorService =
    MoreExecutors.listeningDecorator(
      Executors.newFixedThreadPool(1) { runnable ->
        val thread = Thread(runnable)
        thread.name = "org.librarysimplified.r2.io"
        thread
      }
    )

  private val profile: ProfileReadableType =
    this.profilesController.profileCurrent()

  private val account: AccountType =
    this.profile.account(this.parameters.accountId)

  private var controller: SR2ControllerType? = null
  private var controllerLock: Any = Any()
  private var pendingBookmarks: List<SR2Bookmark> = emptyList()

  init {

    /*
     * Enable webview debugging for debug builds
     */

    if ((this.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
      WebView.setWebContentsDebuggingEnabled(true)
    }

    /*
    * Load bookmarks. They will be submitted to the controller, along with the last position,
    * as soon as they've been loaded and the controller is ready.
    */

    ioExecutor.execute {
      val bookmarks = Reader2Bookmarks.loadBookmarks(
        bookmarkService = this.bookmarkService,
        accountID = this.parameters.accountId,
        bookID = this.parameters.bookId,
      )
      synchronized(this.controllerLock) {
        val controllerNow = this.controller
        if (controllerNow == null) {
          this.pendingBookmarks = bookmarks
        } else {
          this.onBookmarksAndControllerReady(controllerNow, bookmarks)
        }
      }
    }
  }

  private val viewEventsUnicast: UnicastWorkSubject<SR2ReaderViewEvent> =
    UnicastWorkSubject.create()

  private val subscriptions: CompositeDisposable =
    CompositeDisposable(
      this.readerModel.viewEvents
        .subscribe(this::onViewEvent),
      this.readerModel.controllerEvents
        .subscribe(this::onControllerEvent)
    )

  val viewEvents: Observable<SR2ReaderViewEvent>
    get() = viewEventsUnicast.observeOn(AndroidSchedulers.mainThread())

  override fun onCleared() {
    super.onCleared()
    this.sendBookClosedAnalyticsEvent()
    this.ioExecutor.shutdown()
    this.subscriptions.clear()
  }

  private fun sendBookClosedAnalyticsEvent() {
    this.analyticsService.publishEvent(
      AnalyticsEvent.BookClosed(
        timestamp = LocalDateTime.now(),
        credentials = this.account.loginState.credentials,
        profileUUID = this.profile.id.uuid,
        accountProvider = this.account.provider.id,
        accountUUID = this.account.id.uuid,
        opdsEntry = this.parameters.entry.feedEntry
      )
    )
  }

  private fun onViewEvent(event: SR2ReaderViewEvent) {
    this.viewEventsUnicast.onNext(event)

    return when (event) {
      SR2ReaderViewEvent.SR2ReaderViewNavigationEvent.SR2ReaderViewNavigationClose -> {
        // Nothing to do
      }
      SR2ReaderViewEvent.SR2ReaderViewNavigationEvent.SR2ReaderViewNavigationOpenTOC -> {
        // Nothing to do
      }
      is SR2ReaderViewEvent.SR2ReaderViewBookEvent.SR2BookOpened -> {
        synchronized(this.controllerLock) {
          this.controller = event.controller
          if (this.pendingBookmarks.isNotEmpty()) {
            this.onBookmarksAndControllerReady(event.controller, pendingBookmarks)
            this.pendingBookmarks = emptyList()
          }
        }
      }
      is SR2ReaderViewEvent.SR2ReaderViewBookEvent.SR2BookLoadingFailed -> {
        // Nothing to do
      }
    }
  }

  private fun onBookmarksAndControllerReady(controller: SR2ControllerType, bookmarks: List<SR2Bookmark>) {
    val lastRead = bookmarks.find { bookmark -> bookmark.type == SR2Bookmark.Type.LAST_READ }
    controller.submitCommand(SR2Command.BookmarksLoad(bookmarks))
    val startLocator = lastRead?.locator ?: controller.bookMetadata.start
    controller.submitCommand(SR2Command.OpenChapter(startLocator))
  }

  /**
   * Handle incoming messages from the controller.
   */

  private fun onControllerEvent(
    event: SR2Event
  ) {
    return when (event) {
      is SR2Event.SR2BookmarkEvent.SR2BookmarkCreated -> {
        val bookmark =
          Reader2Bookmarks.fromSR2Bookmark(
            bookEntry = this.parameters.entry,
            deviceId = Reader2Devices.deviceId(this.profilesController, this.parameters.bookId),
            source = event.bookmark
          )

        this.bookmarkService.bookmarkCreate(
          accountID = this.parameters.accountId,
          bookmark = bookmark
        )

        Unit
      }

      is SR2Event.SR2BookmarkEvent.SR2BookmarkDeleted -> {
        val bookmark =
          Reader2Bookmarks.fromSR2Bookmark(
            bookEntry = this.parameters.entry,
            deviceId = Reader2Devices.deviceId(this.profilesController, this.parameters.bookId),
            source = event.bookmark
          )

        this.bookmarkService.bookmarkDelete(
          accountID = this.account.id,
          bookmark = bookmark
        )

        Unit
      }

      is SR2Event.SR2ThemeChanged -> {
        this.profilesController.profileUpdate { current ->
          current.copy(
            preferences = current.preferences.copy(
              readerPreferences = Reader2Themes.fromSR2(event.theme)
            )
          )
        }

        Unit
      }

      is SR2Event.SR2OnCenterTapped,
      is SR2Event.SR2ReadingPositionChanged,
      SR2Event.SR2BookmarkEvent.SR2BookmarksLoaded,
      is SR2Event.SR2Error.SR2ChapterNonexistent,
      is SR2Event.SR2Error.SR2WebViewInaccessible,
      is SR2Event.SR2ExternalLinkSelected,
      is SR2Event.SR2CommandEvent.SR2CommandExecutionStarted,
      is SR2Event.SR2CommandEvent.SR2CommandExecutionRunningLong,
      is SR2Event.SR2CommandEvent.SR2CommandEventCompleted.SR2CommandExecutionSucceeded,
      is SR2Event.SR2CommandEvent.SR2CommandEventCompleted.SR2CommandExecutionFailed -> {
        // Nothing
      }
    }
  }
}
