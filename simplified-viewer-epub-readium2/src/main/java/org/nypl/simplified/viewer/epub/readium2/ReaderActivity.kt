package org.nypl.simplified.viewer.epub.readium2

import android.app.Activity
import android.content.Intent
import android.os.Bundle
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
import java.io.File
import java.util.concurrent.Executors

/** Stub */
class ReaderActivity : AppCompatActivity(), SR2ControllerHostType {
  companion object {

    private const val ARG_BOOK_ID = "org.nypl.simplified.app.ReaderActivity.book"
    private const val ARG_FILE = "org.nypl.simplified.app.ReaderActivity.file"
    private const val ARG_ENTRY = "org.nypl.simplified.app.ReaderActivity.entry"

    /** Stub */
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


  private var controllerSubscription: Disposable? = null
  private var controller: SR2ControllerType? = null

  private val ioExecutor =
    MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(1) { runnable ->
      val thread = Thread(runnable)
      thread.name = "org.librarysimplified.r2.demo.io"
      thread
    })


  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val file = intent?.extras?.getSerializable(ARG_FILE) as File

    if (savedInstanceState == null) {
      val fragment = SR2ReaderFragment.create(
        SR2ReaderFragmentParameters(
          bookFile = file
        )
      )

      supportFragmentManager
        .beginTransaction()
        .replace(android.R.id.content, fragment)
        .addToBackStack(null)
        .commit()
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

      is SR2Event.SR2ReadingPositionChanged -> {
        UIThread.runOnUIThread {
          val percent = event.progress * 100.0
          val percentText = String.format("%.2f", percent)
          Toast.makeText(this, "Chapter ${event.chapterIndex}, ${percentText}%", Toast.LENGTH_SHORT).show()
        }
      }
    }
  }
}
