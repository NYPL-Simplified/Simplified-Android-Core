package org.nypl.simplified.viewer.epub.readium2

import android.app.Activity
import android.content.Context.ACCESSIBILITY_SERVICE
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.webkit.WebView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.disposables.Disposable
import org.librarysimplified.r2.api.SR2Bookmark
import org.librarysimplified.r2.api.SR2Bookmark.Type.EXPLICIT
import org.librarysimplified.r2.api.SR2Bookmark.Type.LAST_READ
import org.librarysimplified.r2.api.SR2Command
import org.librarysimplified.r2.api.SR2ControllerProviderType
import org.librarysimplified.r2.api.SR2ControllerType
import org.librarysimplified.r2.api.SR2Event
import org.librarysimplified.r2.api.SR2Event.SR2BookmarkEvent.SR2BookmarkCreated
import org.librarysimplified.r2.api.SR2Event.SR2BookmarkEvent.SR2BookmarkDeleted
import org.librarysimplified.r2.api.SR2Event.SR2BookmarkEvent.SR2BookmarksLoaded
import org.librarysimplified.r2.api.SR2Event.SR2Error.SR2ChapterNonexistent
import org.librarysimplified.r2.api.SR2Event.SR2Error.SR2WebViewInaccessible
import org.librarysimplified.r2.api.SR2Event.SR2OnCenterTapped
import org.librarysimplified.r2.api.SR2Event.SR2ReadingPositionChanged
import org.librarysimplified.r2.api.SR2Locator.SR2LocatorChapterEnd
import org.librarysimplified.r2.api.SR2Locator.SR2LocatorPercent
import org.librarysimplified.r2.vanilla.SR2Controllers
import org.librarysimplified.r2.views.SR2ControllerHostType
import org.librarysimplified.r2.views.SR2ReaderFragment
import org.librarysimplified.r2.views.SR2ReaderFragmentParameters
import org.librarysimplified.r2.views.SR2TOCFragment
import org.librarysimplified.services.api.Services
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.books.api.BookChapterProgress
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.api.BookLocation
import org.nypl.simplified.books.api.Bookmark
import org.nypl.simplified.books.api.BookmarkKind.ReaderBookmarkExplicit
import org.nypl.simplified.books.api.BookmarkKind.ReaderBookmarkLastReadLocation
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.reader.bookmarks.api.ReaderBookmarkServiceType
import org.nypl.simplified.reader.bookmarks.api.ReaderBookmarks
import org.nypl.simplified.ui.thread.api.UIThreadServiceType
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * The main reader activity for reading an EPUB using Readium 2.
 */
class ReaderActivity : AppCompatActivity(), SR2ControllerHostType {

  companion object {

    private const val ARG_ACCOUNT_ID =
      "org.nypl.simplified.app.ReaderActivity.account"
    private const val ARG_BOOK_ID =
      "org.nypl.simplified.app.ReaderActivity.book"
    private const val ARG_FILE =
      "org.nypl.simplified.app.ReaderActivity.file"
    private const val ARG_ENTRY =
      "org.nypl.simplified.app.ReaderActivity.entry"

    private const val SYSTEM_UI_DELAY_MILLIS = 5000L

    /**
     * Start a new reader for the given book.
     */
    fun startActivity(
      context: Activity,
      accountId: AccountID,
      bookId: BookID,
      file: File,
      entry: FeedEntry.FeedEntryOPDS
    ) {
      val intent = Intent(context, ReaderActivity::class.java)

      val bundle = Bundle().apply {
        this.putSerializable(this@Companion.ARG_ACCOUNT_ID, accountId)
        this.putSerializable(this@Companion.ARG_BOOK_ID, bookId)
        this.putSerializable(this@Companion.ARG_FILE, file)
        this.putSerializable(this@Companion.ARG_ENTRY, entry)
      }
      intent.putExtras(bundle)
      context.startActivity(intent)
    }
  }

  private lateinit var accountId: AccountID
  private lateinit var bookEntry: FeedEntry.FeedEntryOPDS
  private lateinit var bookFile: File
  private lateinit var bookId: BookID
  private lateinit var bookmarkService: ReaderBookmarkServiceType
  private lateinit var profiles: ProfilesControllerType
  private lateinit var uiThread: UIThreadServiceType
  private lateinit var readerFragment: SR2ReaderFragment

  private val handler = Handler(Looper.getMainLooper())
  private val hideSystemUiRunnable = Runnable { this.hideSystemUi() }
  private val logger = LoggerFactory.getLogger(ReaderActivity::class.java)

  private var controller: SR2ControllerType? = null
  private var controllerSubscription: Disposable? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val services = Services.serviceDirectory()

    this.uiThread =
      services.requireService(UIThreadServiceType::class.java)
    this.bookmarkService =
      services.requireService(ReaderBookmarkServiceType::class.java)
    this.profiles =
      services.requireService(ProfilesControllerType::class.java)

    this.accountId =
      this.intent?.extras?.getSerializable(ARG_ACCOUNT_ID) as AccountID
    this.bookId =
      this.intent?.extras?.getSerializable(ARG_BOOK_ID) as BookID
    this.bookFile =
      this.intent?.extras?.getSerializable(ARG_FILE) as File
    this.bookEntry =
      this.intent?.extras?.getSerializable(ARG_ENTRY) as FeedEntry.FeedEntryOPDS

    if (savedInstanceState == null) {
      this.setContentView(R.layout.reader2)

      if (!this.isScreenReaderEnabled()) {
        // Init the window with the proper flags
        this.showSystemUi()
      }

      this.supportActionBar?.apply {
        this.title = this@ReaderActivity.bookEntry.feedEntry.title
        this.setDisplayHomeAsUpEnabled(false)
      }

      this.readerFragment =
        SR2ReaderFragment.create(SR2ReaderFragmentParameters(this.bookFile))

      this.supportFragmentManager
        .beginTransaction()
        .replace(R.id.reader_container, readerFragment)
        .commit()
    } else {
      this.readerFragment =
        this.supportFragmentManager.findFragmentById(R.id.reader_container) as SR2ReaderFragment
    }

    // Enable webview debugging for debug builds
    if ((this.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
      WebView.setWebContentsDebuggingEnabled(true)
    }
  }

  override fun onResumeFragments() {
    super.onResumeFragments()
    if (!this.isScreenReaderEnabled()) {
      this.hideSystemUiDelayed()
    }
  }

  override fun onStop() {
    super.onStop()
    this.controllerSubscription?.dispose()
  }

  override fun onControllerBecameAvailable(
    controller: SR2ControllerType,
    isFirstStartup: Boolean
  ) {
    this.controller = controller

    this.controllerSubscription =
      controller.events.subscribe(this::onControllerEvent)

    if (isFirstStartup) {
      // Navigate to the first chapter or saved reading position.
      val bookmarks = this.loadBookmarks()
      val lastRead = bookmarks.find { bookmark -> bookmark.type == LAST_READ }
      controller.submitCommand(SR2Command.BookmarksLoad(bookmarks))
      if (lastRead != null) {
        controller.submitCommand(SR2Command.OpenChapter(lastRead.locator))
      } else {
        controller.submitCommand(SR2Command.OpenChapter(SR2LocatorPercent(0, 0.0)))
      }
    } else {
      // Refresh whatever the controller was looking at previously.
      controller.submitCommand(SR2Command.Refresh)
    }
  }

  override fun onControllerRequired(): SR2ControllerProviderType {
    return SR2Controllers()
  }

  override fun onNavigationClose() {
    this.supportFragmentManager.popBackStack()
  }

  override fun onNavigationOpenTableOfContents() {
    this.cancelSystemUiDelayed()
    this.supportFragmentManager.beginTransaction()
      .replace(R.id.fragment_container, SR2TOCFragment())
      .hide(this.readerFragment)
      .addToBackStack(null)
      .commit()
  }

  /** Cancel any pending tasks to hide the system ui. */

  private fun cancelSystemUiDelayed() {
    this.handler.removeCallbacks(this.hideSystemUiRunnable)
  }

  /** Post a delayed task to switch to immersive mode and hide the system ui. */

  private fun hideSystemUiDelayed() {
    this.cancelSystemUiDelayed()
    this.handler.postDelayed(this.hideSystemUiRunnable, SYSTEM_UI_DELAY_MILLIS)
  }

  private fun onControllerEvent(event: SR2Event) {
    return when (event) {
      is SR2ChapterNonexistent -> {
        this.uiThread.runOnUIThread {
          Toast.makeText(this, "Chapter nonexistent: ${event.chapterIndex}", Toast.LENGTH_SHORT).show()
        }
      }

      is SR2WebViewInaccessible -> {
        // Unused
      }

      is SR2OnCenterTapped -> {
        this.uiThread.runOnUIThread {
          if (!this.isScreenReaderEnabled()) {
            this.toggleSystemUi()
          }
        }
      }

      is SR2ReadingPositionChanged -> {
        // Unused
      }

      is SR2BookmarkCreated -> {
        this.bookmarkService.bookmarkCreate(
          accountID = this.accountId,
          bookmark = fromSR2Bookmark(event.bookmark)
        )
        Unit
      }

      is SR2BookmarkDeleted -> {
        // Unused
      }

      SR2BookmarksLoaded -> {
        // Unused
      }
    }
  }

  private fun loadBookmarks(): List<SR2Bookmark> {
    val rawBookmarks =
      this.loadRawBookmarks()
    val lastRead =
      rawBookmarks.lastRead?.let(this@ReaderActivity::toSR2Bookmark)
    val explicits =
      rawBookmarks.bookmarks.mapNotNull(this@ReaderActivity::toSR2Bookmark)

    val results = mutableListOf<SR2Bookmark>()
    lastRead?.let(results::add)
    results.addAll(explicits)
    return results.toList()
  }

  /**
   * Convert an SR2 bookmark to a SimplyE bookmark.
   */

  private fun fromSR2Bookmark(
    source: SR2Bookmark
  ): Bookmark {
    val progress = BookChapterProgress(
      chapterIndex = source.locator.chapterIndex,
      chapterProgress = when (val locator = source.locator) {
        is SR2LocatorPercent -> locator.chapterProgress
        is SR2LocatorChapterEnd -> 1.0
      }
    )

    val location = BookLocation(
      progress = progress,
      contentCFI = null,
      idRef = null
    )

    val kind = when (source.type) {
      EXPLICIT -> ReaderBookmarkExplicit
      LAST_READ -> ReaderBookmarkLastReadLocation
    }

    return Bookmark(
      opdsId = this.bookEntry.feedEntry.id,
      location = location,
      time = source.date.toLocalDateTime(),
      kind = kind,
      chapterTitle = source.title,
      bookProgress = source.bookProgress,
      deviceID = this.getDeviceIDString(),
      uri = null
    )
  }

  private fun getDeviceIDString(): String? {
    val account = this.profiles.profileAccountForBook(this.bookId)
    val state = account.loginState
    val credentials = state.credentials
    if (credentials != null) {
      val preActivation = credentials.adobeCredentials
      if (preActivation != null) {
        val postActivation = preActivation.postActivationCredentials
        if (postActivation != null) {
          return postActivation.deviceID.value
        }
      }
    }
    // Yes, really return a string that says "null"
    return "null"
  }

  /**
   * Convert a SimplyE bookmark to an SR2 bookmark.
   */

  private fun toSR2Bookmark(
    source: Bookmark
  ): SR2Bookmark? {
    val progress = source.location.progress
    return if (progress != null) {
      SR2Bookmark(
        date = source.time.toDateTime(),
        type = when (source.kind) {
          ReaderBookmarkLastReadLocation -> LAST_READ
          ReaderBookmarkExplicit -> EXPLICIT
        },
        title = source.chapterTitle,
        locator = SR2LocatorPercent(
          chapterIndex = progress.chapterIndex,
          chapterProgress = progress.chapterProgress
        ),
        bookProgress = source.bookProgress
      )
    } else {
      null
    }
  }

  private fun loadRawBookmarks(): ReaderBookmarks {
    return try {
      this.bookmarkService
        .bookmarkLoad(this.accountId, this.bookId)
        .get(10L, TimeUnit.SECONDS)
    } catch (e: Exception) {
      this.logger.error("could not load bookmarks: ", e)
      ReaderBookmarks(null, emptyList())
    }
  }
}

/** Returns `true` if accessibility services are enabled. */
private fun Activity.isScreenReaderEnabled(): Boolean {
  val am = this.getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
  return am.isEnabled || am.isTouchExplorationEnabled
}

/** Returns `true` if fullscreen or immersive mode is not set. */
private fun Activity.isSystemUiVisible(): Boolean {
  return this.window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0
}

/** Enable fullscreen or immersive mode. */
private fun Activity.hideSystemUi() {
  this.window.decorView.systemUiVisibility = (
    View.SYSTEM_UI_FLAG_IMMERSIVE
      or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
      or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
      or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
      or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
      or View.SYSTEM_UI_FLAG_FULLSCREEN
    )
}

/** Disable fullscreen or immersive mode. */
private fun Activity.showSystemUi() {
  this.window.decorView.systemUiVisibility = (
    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
      or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
      or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    )
}

/** Toggle fullscreen or immersive mode. */
private fun Activity.toggleSystemUi() {
  if (this.isSystemUiVisible()) {
    this.hideSystemUi()
  } else {
    this.showSystemUi()
  }
}
