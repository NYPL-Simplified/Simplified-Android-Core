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
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import io.reactivex.disposables.Disposable
import org.librarysimplified.r2.api.SR2Command
import org.librarysimplified.r2.api.SR2ControllerProviderType
import org.librarysimplified.r2.api.SR2ControllerType
import org.librarysimplified.r2.api.SR2Event
import org.librarysimplified.r2.vanilla.SR2Controllers
import org.librarysimplified.r2.vanilla.UIThread
import org.librarysimplified.r2.views.SR2ControllerHostType
import org.librarysimplified.r2.views.SR2ReaderFragment
import org.librarysimplified.r2.views.SR2ReaderFragmentParameters
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.feeds.api.FeedEntry
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.Executors


/**
 * The main reader activity for reading an EPUB using Readium 2.
 */
class ReaderActivity : AppCompatActivity(), SR2ControllerHostType {

  companion object {
    private const val ARG_BOOK_ID = "org.nypl.simplified.app.ReaderActivity.book"
    private const val ARG_FILE = "org.nypl.simplified.app.ReaderActivity.file"
    private const val ARG_ENTRY = "org.nypl.simplified.app.ReaderActivity.entry"
    private const val SYSTEM_UI_DELAY_MILLIS = 5000L

    /**
     * Start a new reader for the given book.
     */
    fun startActivity(
      context: Activity,
      bookId: BookID,
      file: File,
      entry: FeedEntry.FeedEntryOPDS
    ) {
      val intent = Intent(context, ReaderActivity::class.java)

      val bundle = Bundle().apply {
        putSerializable(ARG_BOOK_ID, bookId)
        putSerializable(ARG_FILE, file)
        putSerializable(ARG_ENTRY, entry)
      }
      intent.putExtras(bundle)

      context.startActivity(intent)
    }
  }

  private val logger = LoggerFactory.getLogger(ReaderActivity::class.java)
  private val handler = Handler(Looper.getMainLooper())
  private val ioExecutor =
    MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(1) { runnable ->
      val thread = Thread(runnable)
      thread.name = "org.librarysimplified.r2.demo.io"
      thread
    })

  private var controllerSubscription: Disposable? = null
  private var controller: SR2ControllerType? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val file = intent?.extras?.getSerializable(ARG_FILE) as File
    val entry = intent?.extras?.getSerializable(ARG_ENTRY) as FeedEntry.FeedEntryOPDS

    if (savedInstanceState == null) {
      setContentView(R.layout.reader2)

      if (!isScreenReaderEnabled()) {
        // Init the window with the proper flags
        showSystemUi()
      }

      supportActionBar?.apply {
        title = entry.feedEntry.title
        setDisplayHomeAsUpEnabled(true)
      }

      val fragment = SR2ReaderFragment.create(
        SR2ReaderFragmentParameters(
          bookFile = file
        )
      )

      supportFragmentManager
        .beginTransaction()
        .replace(R.id.reader_container, fragment)
        .commit()
    }

    // Enable webview debugging for debug builds
    if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
      WebView.setWebContentsDebuggingEnabled(true)
    }
  }

  override fun onResumeFragments() {
    super.onResumeFragments()
    if (!isScreenReaderEnabled()) {
      hideSystemUiDelayed()
    }
  }

  override fun onStop() {
    super.onStop()
    controllerSubscription?.dispose()
  }

  override fun onControllerBecameAvailable(controller: SR2ControllerType) {
    this.controller = controller

    controllerSubscription =
      controller.events.subscribe { onControllerEvent(it) }

    /*
     * We simply open the first chapter when the book is loaded. A real application
     * might track the last reading position and provide other bookmark functionality.
     */

    controller.submitCommand(SR2Command.OpenChapter(0))
  }

  override fun onControllerRequired(): SR2ControllerProviderType {
    return SR2Controllers()
  }

  override fun onControllerWantsIOExecutor(): ListeningExecutorService {
    return ioExecutor
  }

  override fun onNavigationClose() {
    return supportFragmentManager.popBackStack()
  }

  override fun onNavigationOpenTableOfContents() {
    TODO("not implemented")
  }

  private fun hideSystemUiDelayed() {
    handler.removeCallbacksAndMessages(null)
    handler.postDelayed({ hideSystemUi() }, SYSTEM_UI_DELAY_MILLIS)
  }

  private fun onControllerEvent(event: SR2Event) {
    when (event) {
      is SR2Event.SR2Error.SR2ChapterNonexistent -> {
        UIThread.runOnUIThread {
          Toast.makeText(this, "Chapter nonexistent: ${event.chapterIndex}", Toast.LENGTH_SHORT).show()
        }
      }

      is SR2Event.SR2Error.SR2WebViewInaccessible -> {
        UIThread.runOnUIThread {
          Toast.makeText(this, "Web view inaccessible!", Toast.LENGTH_SHORT).show()
        }
      }

      is SR2Event.SR2OnCenterTapped -> {
        UIThread.runOnUIThread {
          if (!isScreenReaderEnabled()) {
            toggleSystemUi()
          }
        }
      }

      is SR2Event.SR2ReadingPositionChanged -> {
        logger.debug("SR2ReadingPositionChanged")
      }
    }
  }
}

/** Returns `true` if accessibility services are enabled. */
private fun Activity.isScreenReaderEnabled(): Boolean {
  val am = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
  return am.isEnabled || am.isTouchExplorationEnabled
}

/** Returns `true` if fullscreen or immersive mode is not set. */
private fun Activity.isSystemUiVisible(): Boolean {
  return window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0
}

/** Enable fullscreen or immersive mode. */
private fun Activity.hideSystemUi() {
  window.decorView.systemUiVisibility = (
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
  window.decorView.systemUiVisibility = (
    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
      or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
      or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    )
}

/** Toggle fullscreen or immersive mode. */
private fun Activity.toggleSystemUi() {
  if (isSystemUiVisible()) {
    hideSystemUi()
  } else {
    showSystemUi()
  }
}
