package org.librarysimplified.r2.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.UiThread
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.lifecycle.ViewModelProviders
import com.google.common.base.Function
import com.google.common.util.concurrent.FluentFuture
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import io.reactivex.disposables.Disposable
import org.librarysimplified.r2.api.SR2Bookmark
import org.librarysimplified.r2.api.SR2Bookmark.Type.EXPLICIT
import org.librarysimplified.r2.api.SR2Command
import org.librarysimplified.r2.api.SR2ControllerConfiguration
import org.librarysimplified.r2.api.SR2ControllerType
import org.librarysimplified.r2.api.SR2Event
import org.librarysimplified.r2.api.SR2Event.SR2BookmarkEvent.SR2BookmarkCreated
import org.librarysimplified.r2.api.SR2Event.SR2BookmarkEvent.SR2BookmarksLoaded
import org.librarysimplified.r2.api.SR2Event.SR2Error.SR2ChapterNonexistent
import org.librarysimplified.r2.api.SR2Event.SR2Error.SR2WebViewInaccessible
import org.librarysimplified.r2.api.SR2Event.SR2OnCenterTapped
import org.librarysimplified.r2.api.SR2Event.SR2ReadingPositionChanged
import org.librarysimplified.r2.api.SR2Locator
import org.librarysimplified.r2.views.internal.SR2ControllerReference
import org.librarysimplified.r2.views.internal.SR2ReaderViewModel
import org.nypl.simplified.ui.thread.api.UIThread
import org.slf4j.LoggerFactory

class SR2ReaderFragment(
  private val parameters: SR2ReaderFragmentParameters
) : Fragment() {

  private val logger = LoggerFactory.getLogger(SR2ReaderFragment::class.java)

  companion object {
    private const val PARAMETERS_ID =
      "org.librarysimplified.r2.views.SR2ReaderFragment.parameters"

    /**
     * Create a book detail fragment for the given parameters.
     */

    fun createFactory(
      parameters: SR2ReaderFragmentParameters
    ): FragmentFactory = Factory(parameters)
  }

  private class Factory(val parameters: SR2ReaderFragmentParameters) : FragmentFactory() {

    override fun instantiate(classLoader: ClassLoader, className: String): Fragment =
      when (className) {
        SR2ReaderFragment::javaClass.name -> SR2ReaderFragment(parameters)
        else -> super.instantiate(classLoader, className)
      }
  }

  private lateinit var controllerHost: SR2ControllerHostType
  private lateinit var menu: Menu
  private lateinit var menuBookmarkItem: MenuItem
  private lateinit var positionPageView: TextView
  private lateinit var positionPercentView: TextView
  private lateinit var positionTitleView: TextView
  private lateinit var progressView: ProgressBar
  private lateinit var readerModel: SR2ReaderViewModel
  private lateinit var webView: WebView
  private var controller: SR2ControllerType? = null
  private var controllerSubscription: Disposable? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    this.setHasOptionsMenu(true)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val view =
      inflater.inflate(R.layout.sr2_reader, container, false)

    this.webView = view.findViewById(R.id.readerWebView)
    this.progressView = view.findViewById(R.id.reader2_progress)
    this.positionPageView = view.findViewById(R.id.reader2_position_page)
    this.positionTitleView = view.findViewById(R.id.reader2_position_title)
    this.positionPercentView = view.findViewById(R.id.reader2_position_percent)

    return view
  }

  override fun onCreateOptionsMenu(
    menu: Menu,
    inflater: MenuInflater
  ) {
    inflater.inflate(R.menu.sr2_reader_menu, menu)
    this.menu = menu
    this.menuBookmarkItem = menu.findItem(R.id.readerMenuAddBookmark)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    val controllerNow = this.controller
    return when (item.itemId) {
      R.id.readerMenuSettings -> {
        Toast.makeText(this.requireContext(), "Settings!", Toast.LENGTH_SHORT).show()
        true
      }
      R.id.readerMenuTOC -> {
        if (controllerNow != null) {
          this.controllerHost.onNavigationOpenTableOfContents()
        }
        true
      }
      R.id.readerMenuAddBookmark -> {
        if (controllerNow != null) {
          if (this.findBookmarkForCurrentPage(controllerNow, controllerNow.positionNow()) == null) {
            controllerNow.submitCommand(SR2Command.BookmarkCreate)
          }
        }
        true
      }
      else ->
        super.onOptionsItemSelected(item)
    }
  }

  override fun onStart() {
    super.onStart()

    val activity = this.requireActivity()
    this.controllerHost = activity as SR2ControllerHostType
    this.readerModel =
      ViewModelProviders.of(activity)
        .get(SR2ReaderViewModel::class.java)

    /*
     * Fetch or create a controller with the given EPUB loaded into it.
     */

    val controllerFuture: ListenableFuture<SR2ControllerReference> =
      this.readerModel.createOrGet(
        configuration = SR2ControllerConfiguration(
          bookFile = this.parameters.bookFile,
          context = activity,
          streamer = this.parameters.streamer,
          ioExecutor = this.readerModel.ioExecutor,
          uiExecutor = UIThread::runOnUIThread
        ),
        controllers = this.controllerHost.onControllerRequired()
      )

    FluentFuture.from(controllerFuture)
      .transform(
        Function<SR2ControllerReference, Unit> { reference ->
          this.onBookOpenSucceeded(reference!!.controller, reference.isFirstStartup)
        },
        MoreExecutors.directExecutor()
      )
      .catching(
        Throwable::class.java,
        Function<Throwable, Unit> { e ->
          this.onBookOpenFailed(e!!)
        },
        MoreExecutors.directExecutor()
      )
  }

  override fun onStop() {
    super.onStop()
    this.controllerSubscription?.dispose()
    this.controller?.viewDisconnect()
  }

  private fun onBookOpenSucceeded(
    controller: SR2ControllerType,
    isFirstStartup: Boolean
  ) {
    this.logger.debug("onBookOpenSucceeded: first startup {}", isFirstStartup)
    UIThread.runOnUIThread { this.onBookOpenSucceededUI(controller, isFirstStartup) }
  }

  @UiThread
  private fun onBookOpenSucceededUI(
    controller: SR2ControllerType,
    isFirstStartup: Boolean
  ) {
    this.controller = controller
    controller.viewConnect(this.webView)
    this.controllerSubscription = controller.events.subscribe(this::onControllerEvent)
    this.controllerHost.onControllerBecameAvailable(controller, isFirstStartup)
  }

  private fun onBookOpenFailed(e: Throwable) {
    this.logger.error("onBookOpenFailed: ", e)
    UIThread.runOnUIThread { this.onBookOpenFailedUI(e) }
  }

  @UiThread
  private fun onBookOpenFailedUI(e: Throwable) {
    TODO()
  }

  @UiThread
  private fun onReadingPositionChanged(event: SR2ReadingPositionChanged) {
    this.logger.debug("chapterTitle=${event.chapterTitle}")
    this.progressView.apply { this.max = 100; this.progress = event.bookProgressPercent }
    this.positionPageView.text = this.getString(R.string.progress_page, event.currentPage, event.pageCount)
    this.positionTitleView.text = event.chapterTitle
    this.positionPercentView.text = this.getString(R.string.progress_percent, event.bookProgressPercent)
    this.reconfigureBookmarkMenuItem(event.locator)
  }

  @UiThread
  private fun reconfigureBookmarkMenuItem(currentPosition: SR2Locator) {
    UIThread.checkIsUIThread()

    val controllerNow = this.controller
    if (controllerNow != null) {
      val bookmark = this.findBookmarkForCurrentPage(controllerNow, currentPosition)
      if (bookmark != null) {
        this.menuBookmarkItem.setIcon(R.drawable.sr2_bookmark_active)
      } else {
        this.menuBookmarkItem.setIcon(R.drawable.sr2_bookmark_inactive)
      }
    }
  }

  private fun findBookmarkForCurrentPage(
    controllerNow: SR2ControllerType,
    currentPosition: SR2Locator
  ): SR2Bookmark? {
    return controllerNow.bookmarksNow()
      .find { bookmark -> this.locationMatchesBookmark(bookmark, currentPosition) }
  }

  private fun locationMatchesBookmark(
    bookmark: SR2Bookmark,
    location: SR2Locator
  ): Boolean {
    return bookmark.type == EXPLICIT && location.compareTo(bookmark.locator) == 0
  }

  private fun onControllerEvent(event: SR2Event) {
    return when (event) {
      is SR2ReadingPositionChanged -> {
        UIThread.runOnUIThread {
          if (!this.isDetached) {
            this.onReadingPositionChanged(event)
          }
        }
      }

      SR2BookmarksLoaded,
      is SR2Event.SR2BookmarkEvent.SR2BookmarkDeleted,
      is SR2BookmarkCreated -> {
        this.onBookmarksChanged()
      }

      is SR2ChapterNonexistent,
      is SR2WebViewInaccessible,
      is SR2OnCenterTapped -> {
        // Nothing
      }
    }
  }

  private fun onBookmarksChanged() {
    UIThread.runOnUIThread {
      val controllerNow = this.controller
      if (controllerNow != null) {
        if (!this.isDetached) {
          this.reconfigureBookmarkMenuItem(controllerNow.positionNow())
        }
      }
    }
  }
}
