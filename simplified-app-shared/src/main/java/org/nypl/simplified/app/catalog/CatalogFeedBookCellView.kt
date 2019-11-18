package org.nypl.simplified.app.catalog

import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.MoreExecutors.directExecutor
import com.io7m.jfunctional.Unit
import com.io7m.jnull.NullCheck
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.accounts.database.api.AccountsDatabaseNonexistentException
import org.nypl.simplified.analytics.api.AnalyticsType
import org.nypl.simplified.app.NetworkConnectivityType
import org.nypl.simplified.app.R
import org.nypl.simplified.app.ScreenSizeInformationType
import org.nypl.simplified.app.login.LoginDialog
import org.nypl.simplified.app.utilities.UIThread
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.book_registry.BookRegistryReadableType
import org.nypl.simplified.books.controller.api.BooksControllerType
import org.nypl.simplified.books.covers.BookCoverProviderType
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.feeds.api.FeedEntry.FeedEntryCorrupt
import org.nypl.simplified.feeds.api.FeedEntry.FeedEntryOPDS
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.profiles.api.ProfileNoneCurrentException
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicReference

/**
 * A single cell in feed (list or grid).
 */

class CatalogFeedBookCellView(
  private val activity: AppCompatActivity,
  private val analytics: AnalyticsType,
  private val coverProvider: BookCoverProviderType,
  private val booksController: BooksControllerType,
  private val profilesController: ProfilesControllerType,
  private val booksRegistry: BookRegistryReadableType,
  private val networkConnectivity: NetworkConnectivityType,
  private val screenSizeInformation: ScreenSizeInformationType
) :
  FrameLayout(activity) {

  private val cellAuthors: TextView
  private val cellBook: ViewGroup
  private val cellButtons: ViewGroup
  private val cellCorrupt: ViewGroup
  private val cellCorruptText: TextView
  private val cellCoverImage: ImageView
  private val cellCoverLayout: ViewGroup
  private val cellCoverProgress: ProgressBar
  private val cellDebug: TextView
  private val cellDownloading: ViewGroup
  private val cellDownloadingAuthors: TextView
  private val cellDownloadingCancel: Button
  private val cellDownloadingFailed: ViewGroup
  private val cellDownloadingFailedTitle: TextView
  private val cellDownloadingPercentText: TextView
  private val cellDownloadingProgress: ProgressBar
  private val cellDownloadingTitle: TextView
  private val cellDownloadingFailedButtons: ViewGroup
  private val cellTextLayout: ViewGroup
  private val cellTitle: TextView
  private val debugCellState: Boolean
  private val entry: AtomicReference<FeedEntryOPDS>
  private val cellDownloadingLabel: TextView
  private val cellDownloadingFailedLabel: TextView
  private var bookSelectionListener: CatalogBookSelectionListenerType? = null

  init {
    this.bookSelectionListener =
      CatalogBookSelectionListenerType { v, e -> LOG.debug("doing nothing for {}", e) }

    val resources = this.activity.resources

    val inflater = this.activity.layoutInflater
    inflater.inflate(R.layout.catalog_book_cell, this, true)

    /*
     * Receive book status updates.
     */

    this.cellDownloading = this.findViewById(R.id.cell_downloading)

    this.cellDownloadingProgress =
      this.cellDownloading.findViewById(R.id.cell_downloading_progress)
    this.cellDownloadingPercentText =
      this.cellDownloading.findViewById(R.id.cell_downloading_percent_text)
    this.cellDownloadingLabel =
      this.cellDownloading.findViewById(R.id.cell_downloading_label)
    this.cellDownloadingTitle =
      this.cellDownloading.findViewById(R.id.cell_downloading_title)
    this.cellDownloadingAuthors =
      this.cellDownloading.findViewById(R.id.cell_downloading_authors)
    this.cellDownloadingCancel =
      this.cellDownloading.findViewById(R.id.cell_downloading_cancel)

    this.cellDownloadingFailed =
      this.findViewById(R.id.cell_downloading_failed)
    this.cellDownloadingFailedTitle =
      this.cellDownloadingFailed.findViewById(R.id.cell_downloading_failed_title)
    this.cellDownloadingFailedLabel =
      this.cellDownloadingFailed.findViewById(R.id.cell_downloading_failed_static_text)
    this.cellDownloadingFailedButtons =
      this.cellDownloadingFailed.findViewById(R.id.cell_downloading_failed_buttons)

    this.cellCorrupt =
      this.findViewById(R.id.cell_corrupt)
    this.cellCorruptText =
      this.cellCorrupt.findViewById(R.id.cell_corrupt_text)

    this.cellBook =
      this.findViewById(R.id.cell_book)

    this.cellDebug = this.findViewById(R.id.cell_debug)
    this.debugCellState = resources.getBoolean(R.bool.debug_catalog_cell_view_states)
    if (this.debugCellState == false) {
      this.cellDebug.visibility = View.GONE
    }

    this.cellTextLayout =
      this.cellBook.findViewById(R.id.cell_text_layout)
    this.cellTitle =
      this.cellTextLayout.findViewById(R.id.cell_title)
    this.cellAuthors =
      this.cellTextLayout.findViewById(R.id.cell_authors)
    this.cellButtons =
      this.cellTextLayout.findViewById(R.id.cell_buttons)
    this.cellCoverLayout =
      this.findViewById(R.id.cell_cover_layout)
    this.cellCoverImage =
      this.cellCoverLayout.findViewById(R.id.cell_cover_image)
    this.cellCoverProgress =
      this.cellCoverLayout.findViewById(R.id.cell_cover_loading)

    /*
     * The height of the row is known, so assume a roughly 4:3 aspect ratio
     * for cover images and calculate the width of the cover layout in pixels.
     */

    val coverHeight = this.cellCoverLayout.layoutParams.height
    val coverWidth = (coverHeight.toDouble() / 4.0 * 3.0).toInt()
    val cclP = LinearLayout.LayoutParams(coverWidth, coverHeight)
    this.cellCoverLayout.layoutParams = cclP

    this.entry = AtomicReference()

    this.cellBook.visibility = View.INVISIBLE
    this.cellCorrupt.visibility = View.INVISIBLE
    this.cellDownloading.visibility = View.INVISIBLE
    this.cellDownloadingFailed.visibility = View.INVISIBLE
  }

  private fun loadImageAndSetVisibility(inE: FeedEntryOPDS) {
    val inImageHeight = this.cellCoverLayout.layoutParams.height

    val coverImage = this.cellCoverImage
    val coverProgress = this.cellCoverProgress

    coverImage.visibility = View.INVISIBLE
    coverProgress.visibility = View.VISIBLE

    val callback = object : FutureCallback<kotlin.Unit> {
      override fun onSuccess(result: kotlin.Unit?) {
        UIThread.runOnUIThread {
          coverImage.visibility = View.VISIBLE
          coverProgress.visibility = View.INVISIBLE
        }
      }

      override fun onFailure(t: Throwable) {
        UIThread.runOnUIThread {
          LOG.error("unable to load image")
          coverImage.visibility = View.INVISIBLE
          coverProgress.visibility = View.INVISIBLE
        }
      }
    }

    val future =
      this.coverProvider.loadThumbnailInto(
        inE,
        this.cellCoverImage,
        (inImageHeight.toDouble() * 0.75).toInt(),
        inImageHeight)

    Futures.addCallback<kotlin.Unit>(future, callback, directExecutor())
  }

  private fun account(bookId: BookID): AccountType {
    try {
      return this.profilesController.profileAccountForBook(bookId)
    } catch (e: ProfileNoneCurrentException) {
      throw IllegalStateException(e)
    } catch (e: AccountsDatabaseNonexistentException) {
      throw IllegalStateException(e)
    }
  }

  private fun showLoginDialog() {
    val loginDialog = LoginDialog()
    loginDialog.show(this.activity.supportFragmentManager, "login-dialog")
  }

  fun onFeedEntryCorrupt(e: FeedEntryCorrupt): Unit {
    LOG.debug("{}: feed entry corrupt: ", e.bookID, e.error)

    this.cellDownloading.visibility = View.INVISIBLE
    this.cellBook.visibility = View.INVISIBLE
    this.cellDownloadingFailed.visibility = View.INVISIBLE
    this.cellCorrupt.visibility = View.VISIBLE
    this.setDebugCellText("entry-corrupt")

    val text =
      String.format("%s (%s)", resources.getString(R.string.catalog_meta_corrupt), e.bookID)
    this.cellCorruptText.text = text
    return Unit.unit()
  }

  fun onFeedEntryOPDS(feedE: FeedEntryOPDS): Unit {
    val oe = feedE.feedEntry
    this.cellTitle.text = oe.title
    this.cellAuthors.text = makeAuthorText(oe)

    this.contentDescription = CatalogBookFormats.contentDescriptionOfEntry(this.resources, feedE)

    val bookListener = this.bookSelectionListener
    this.setOnClickListener { v -> bookListener!!.onSelectBook(this, feedE) }

    this.entry.set(feedE)

    val bookId = feedE.bookID
    return Unit.unit()
  }

  private fun setDebugCellText(
    text: String
  ) {
    if (this.debugCellState) {
      this.cellDebug.text = text
    }
  }

  /**
   * Configure the overall status of the cell. The cell displays a number of
   * different layouts depending on whether the current book is loaned, fully
   * downloaded, currently downloading, not loaned, etc.
   *
   * @param entry The new feed entry
   * @param listener A selection listener
   */

  fun viewConfigure(
    entry: FeedEntry,
    listener: CatalogBookSelectionListenerType
  ) {
    UIThread.checkIsUIThread()
    this.bookSelectionListener = listener

    return when (entry) {
      is FeedEntryCorrupt -> {
        this.onFeedEntryCorrupt(entry)
        Unit
      }
      is FeedEntryOPDS -> {
        this.onFeedEntryOPDS(entry)
        Unit
      }
    }
  }

  companion object {

    private val LOG = LoggerFactory.getLogger(CatalogFeedBookCellView::class.java)

    private fun makeAuthorText(inE: OPDSAcquisitionFeedEntry): String {
      val sb = StringBuilder()
      val `as` = inE.authors
      val max = `as`.size
      for (index in 0 until max) {
        val a = NullCheck.notNull(`as`[index])
        sb.append(a)
        if (index + 1 < max) {
          sb.append(", ")
        }
      }
      return NullCheck.notNull(sb.toString())
    }
  }
}
