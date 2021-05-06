package org.nypl.simplified.viewer.epub.readium2

import android.app.Activity
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.webkit.WebView
import androidx.annotation.UiThread
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import io.reactivex.disposables.Disposable
import org.joda.time.LocalDateTime
import org.librarysimplified.r2.api.SR2Bookmark
import org.librarysimplified.r2.api.SR2Command
import org.librarysimplified.r2.api.SR2ControllerType
import org.librarysimplified.r2.api.SR2Event
import org.librarysimplified.r2.api.SR2Event.SR2BookmarkEvent.SR2BookmarkCreated
import org.librarysimplified.r2.api.SR2Event.SR2BookmarkEvent.SR2BookmarkDeleted
import org.librarysimplified.r2.api.SR2Event.SR2BookmarkEvent.SR2BookmarksLoaded
import org.librarysimplified.r2.api.SR2Event.SR2CommandEvent.SR2CommandEventCompleted.SR2CommandExecutionFailed
import org.librarysimplified.r2.api.SR2Event.SR2CommandEvent.SR2CommandEventCompleted.SR2CommandExecutionSucceeded
import org.librarysimplified.r2.api.SR2Event.SR2CommandEvent.SR2CommandExecutionRunningLong
import org.librarysimplified.r2.api.SR2Event.SR2CommandEvent.SR2CommandExecutionStarted
import org.librarysimplified.r2.api.SR2Event.SR2Error.SR2ChapterNonexistent
import org.librarysimplified.r2.api.SR2Event.SR2Error.SR2WebViewInaccessible
import org.librarysimplified.r2.api.SR2Locator
import org.librarysimplified.r2.vanilla.SR2Controllers
import org.librarysimplified.r2.views.SR2ControllerReference
import org.librarysimplified.r2.views.SR2ReaderFragment
import org.librarysimplified.r2.views.SR2ReaderFragmentFactory
import org.librarysimplified.r2.views.SR2ReaderParameters
import org.librarysimplified.r2.views.SR2ReaderViewEvent
import org.librarysimplified.r2.views.SR2ReaderViewEvent.SR2ReaderViewBookEvent.SR2BookLoadingFailed
import org.librarysimplified.r2.views.SR2ReaderViewEvent.SR2ReaderViewControllerEvent.SR2ControllerBecameAvailable
import org.librarysimplified.r2.views.SR2ReaderViewEvent.SR2ReaderViewNavigationEvent.SR2ReaderViewNavigationClose
import org.librarysimplified.r2.views.SR2ReaderViewEvent.SR2ReaderViewNavigationEvent.SR2ReaderViewNavigationOpenTOC
import org.librarysimplified.r2.views.SR2ReaderViewModel
import org.librarysimplified.r2.views.SR2ReaderViewModelFactory
import org.librarysimplified.r2.views.SR2TOCFragment
import org.librarysimplified.services.api.Services
import org.nypl.drm.core.AdobeAdeptFileAsset
import org.nypl.drm.core.AxisNowFileAsset
import org.nypl.drm.core.ContentProtectionProvider
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.analytics.api.AnalyticsEvent
import org.nypl.simplified.analytics.api.AnalyticsType
import org.nypl.simplified.books.api.BookDRMInformation
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.reader.bookmarks.api.ReaderBookmarkServiceType
import org.nypl.simplified.ui.thread.api.UIThreadServiceType
import org.readium.r2.shared.publication.asset.FileAsset
import org.readium.r2.streamer.Streamer
import org.readium.r2.streamer.parser.epub.EpubParser
import org.slf4j.LoggerFactory
import java.util.ServiceLoader
import java.util.concurrent.ExecutionException

/**
 * The main reader activity for reading an EPUB using Readium 2.
 */

class Reader2Activity : AppCompatActivity() {

  companion object {

    private const val ARG_PARAMETERS =
      "org.nypl.simplified.viewer.epub.readium2.ReaderActivity2.parameters"

    /**
     * Start a new reader for the given book.
     */

    fun startActivity(
      context: Activity,
      parameters: Reader2ActivityParameters
    ) {
      val intent = Intent(context, Reader2Activity::class.java)
      val bundle = Bundle().apply {
        this.putSerializable(this@Companion.ARG_PARAMETERS, parameters)
      }
      intent.putExtras(bundle)
      context.startActivity(intent)
    }
  }

  private val logger =
    LoggerFactory.getLogger(Reader2Activity::class.java)

  private lateinit var account: AccountType
  private lateinit var analyticsService: AnalyticsType
  private lateinit var bookmarkService: ReaderBookmarkServiceType
  private lateinit var contentProtectionProviders: List<ContentProtectionProvider>
  private lateinit var parameters: Reader2ActivityParameters
  private lateinit var profilesController: ProfilesControllerType
  private lateinit var readerBookmarks: ReaderBookmarkServiceType
  private lateinit var readerFragmentFactory: SR2ReaderFragmentFactory
  private lateinit var uiThread: UIThreadServiceType
  private var controller: SR2ControllerType? = null
  private var controllerSubscription: Disposable? = null
  private var viewSubscription: Disposable? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    /*
     * Enable webview debugging for debug builds
     */

    if ((this.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
      WebView.setWebContentsDebuggingEnabled(true)
    }

    val services = Services.serviceDirectory()

    this.analyticsService =
      services.requireService(AnalyticsType::class.java)
    this.bookmarkService =
      services.requireService(ReaderBookmarkServiceType::class.java)
    this.profilesController =
      services.requireService(ProfilesControllerType::class.java)
    this.readerBookmarks =
      services.requireService(ReaderBookmarkServiceType::class.java)
    this.uiThread =
      services.requireService(UIThreadServiceType::class.java)
    this.contentProtectionProviders =
      ServiceLoader.load(ContentProtectionProvider::class.java)
        .toList()

    this.logger.debug("loaded {} content protection providers", this.contentProtectionProviders.size)
    this.contentProtectionProviders.forEachIndexed { index, provider ->
      this.logger.debug("[{}] available provider {}", index, provider.javaClass.canonicalName)
    }

    val intent =
      this.intent ?: throw IllegalStateException("ReaderActivity2 requires an intent")
    val extras =
      intent.extras ?: throw IllegalStateException("ReaderActivity2 Intent lacks parameters")

    this.parameters =
      extras.getSerializable(ARG_PARAMETERS) as Reader2ActivityParameters

    try {
      this.account =
        this.profilesController.profileCurrent()
          .account(this.parameters.accountId)
    } catch (e: Exception) {
      this.logger.error("unable to locate account: ", e)
      this.finish()
      return
    }

    if (savedInstanceState == null) {
      this.setContentView(R.layout.reader2)
    }
  }

  override fun onResume() {
    super.onResume()
    this.startReader()
  }

  override fun onStop() {
    super.onStop()
    this.controllerSubscription?.dispose()
    this.viewSubscription?.dispose()

    /*
     * If the activity is finishing, send an analytics event.
     */

    if (this.isFinishing) {
      val profile = this.profilesController.profileCurrent()

      this.analyticsService.publishEvent(
        AnalyticsEvent.BookClosed(
          timestamp = LocalDateTime.now(),
          credentials = this.account.loginState.credentials,
          profileUUID = profile.id.uuid,
          accountProvider = this.account.provider.id,
          accountUUID = this.account.id.uuid,
          opdsEntry = this.parameters.entry.feedEntry
        )
      )
    }
  }

  /**
   * Start the reader with the given EPUB.
   */

  @UiThread
  private fun startReader() {
    this.uiThread.checkIsUIThread()

    /*
     * Instantiate any content protections that might be needed for DRM...
     */

    val contentProtections =
      this.contentProtectionProviders.mapNotNull { provider ->
        this.logger.debug("instantiating content protection provider {}", provider.javaClass.canonicalName)
        provider.create(this)
      }

    val streamer =
      Streamer(
        context = this,
        parsers = listOf(EpubParser()),
        contentProtections = contentProtections,
        ignoreDefaultParsers = true
      )

    /*
     * Load the most recently configured theme from the profile's preferences.
     */

    val initialTheme =
      Reader2Themes.toSR2(
        this.profilesController.profileCurrent()
          .preferences()
          .readerPreferences
      )

    val bookFile =
      when (val drmInfo = this.parameters.drmInfo) {
        is BookDRMInformation.ACS ->
          AdobeAdeptFileAsset(
            fileAsset = FileAsset(this.parameters.file),
            adobeRightsFile = drmInfo.rights?.first
          )
        is BookDRMInformation.AXIS ->
          AxisNowFileAsset(
            fileAsset = FileAsset(this.parameters.file),
            axisLicense = drmInfo.license,
            axisUserKey = drmInfo.userKey
          )
        else -> FileAsset(this.parameters.file)
      }

    val readerParameters =
      SR2ReaderParameters(
        streamer = streamer,
        bookFile = bookFile,
        bookId = this.parameters.entry.feedEntry.id,
        theme = initialTheme,
        controllers = SR2Controllers()
      )

    this.readerFragmentFactory =
      SR2ReaderFragmentFactory(readerParameters)

    val readerModel =
      ViewModelProvider(this, SR2ReaderViewModelFactory(readerParameters))
        .get(SR2ReaderViewModel::class.java)

    this.viewSubscription =
      readerModel.viewEvents.subscribe(this::onViewEvent)

    this.supportFragmentManager.beginTransaction()
      .replace(R.id.reader2FragmentHost, this.readerFragmentFactory.instantiate(this.classLoader, SR2ReaderFragment::class.java.name))
      .commit()
  }

  /**
   * Handle incoming messages from the view fragments.
   */

  private fun onViewEvent(event: SR2ReaderViewEvent) {
    return when (event) {
      SR2ReaderViewNavigationClose ->
        this.uiThread.runOnUIThread {
          this.supportFragmentManager.popBackStack()
        }

      SR2ReaderViewNavigationOpenTOC ->
        this.uiThread.runOnUIThread(this::openTOC)

      is SR2ControllerBecameAvailable ->
        this.onControllerBecameAvailable(event.reference)
      is SR2BookLoadingFailed ->
        this.onBookLoadingFailed(event.exception)
    }
  }

  private fun onControllerBecameAvailable(reference: SR2ControllerReference) {
    this.controller = reference.controller

    /*
     * Subscribe to messages from the controller.
     */

    this.controllerSubscription =
      reference.controller.events.subscribe(this::onControllerEvent)

    if (reference.isFirstStartup) {
      val bookmarks =
        Reader2Bookmarks.loadBookmarks(
          bookmarkService = this.bookmarkService,
          accountID = this.parameters.accountId,
          bookID = this.parameters.bookId,
          bookMetadata = reference.controller.bookMetadata
        )

      val lastRead = bookmarks.find { bookmark -> bookmark.type == SR2Bookmark.Type.LAST_READ }
      reference.controller.submitCommand(SR2Command.BookmarksLoad(bookmarks))
      if (lastRead != null) {
        reference.controller.submitCommand(SR2Command.OpenChapter(lastRead.locator))
      } else {
        val first = reference.controller.bookMetadata.readingOrder.first()
        reference.controller.submitCommand(
          SR2Command.OpenChapter(SR2Locator.SR2LocatorPercent(first.chapterHref, 0.0))
        )
      }
    } else {
      // Refresh whatever the controller was looking at previously.
      reference.controller.submitCommand(SR2Command.Refresh)
    }
  }

  /**
   * Handle incoming messages from the controller.
   */

  private fun onControllerEvent(
    event: SR2Event
  ) {
    return when (event) {
      is SR2BookmarkCreated -> {
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

      is SR2BookmarkDeleted -> {
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
      SR2BookmarksLoaded,
      is SR2ChapterNonexistent,
      is SR2WebViewInaccessible,
      is SR2CommandExecutionStarted,
      is SR2CommandExecutionRunningLong,
      is SR2CommandExecutionSucceeded,
      is SR2CommandExecutionFailed -> {
        // Nothing
      }
    }
  }

  /**
   * Open the table of contents.
   */

  @UiThread
  private fun openTOC() {
    this.uiThread.checkIsUIThread()

    if (this.isFinishing || this.isDestroyed) {
      return
    }

    this.supportFragmentManager.beginTransaction()
      .replace(R.id.reader2FragmentHost, this.readerFragmentFactory.instantiate(this.classLoader, SR2TOCFragment::class.java.name))
      .addToBackStack(null)
      .commit()
  }

  /**
   * Loading a book failed.
   */

  private fun onBookLoadingFailed(
    exception: Throwable
  ) {
    this.uiThread.runOnUIThread {
      val actualException =
        if (exception is ExecutionException) {
          exception.cause ?: exception
        } else {
          exception
        }

      AlertDialog.Builder(this)
        .setTitle(R.string.bookOpenFailedTitle)
        .setMessage(this.getString(R.string.bookOpenFailedMessage, actualException.javaClass.name, actualException.message))
        .setOnDismissListener { this.finish() }
        .create()
        .show()
    }
  }
}
