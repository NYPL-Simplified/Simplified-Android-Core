package org.nypl.simplified.app.catalog

import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.MoreExecutors.directExecutor
import com.io7m.jfunctional.None
import com.io7m.jfunctional.OptionType
import com.io7m.jfunctional.Some
import com.io7m.jfunctional.Unit
import com.io7m.jnull.NullCheck
import com.io7m.junreachable.UnreachableCodeException
import org.joda.time.DateTime
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.accounts.database.api.AccountsDatabaseNonexistentException
import org.nypl.simplified.analytics.api.AnalyticsType
import org.nypl.simplified.app.NetworkConnectivityType
import org.nypl.simplified.app.R
import org.nypl.simplified.app.ScreenSizeInformationType
import org.nypl.simplified.app.login.LoginDialog
import org.nypl.simplified.app.utilities.UIThread
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.book_database.api.BookAcquisitionSelection
import org.nypl.simplified.books.book_registry.BookRegistryReadableType
import org.nypl.simplified.books.book_registry.BookStatusDownloadFailed
import org.nypl.simplified.books.book_registry.BookStatusDownloadInProgress
import org.nypl.simplified.books.book_registry.BookStatusDownloaded
import org.nypl.simplified.books.book_registry.BookStatusDownloadingMatcherType
import org.nypl.simplified.books.book_registry.BookStatusDownloadingType
import org.nypl.simplified.books.book_registry.BookStatusHeld
import org.nypl.simplified.books.book_registry.BookStatusHeldReady
import org.nypl.simplified.books.book_registry.BookStatusHoldable
import org.nypl.simplified.books.book_registry.BookStatusLoanable
import org.nypl.simplified.books.book_registry.BookStatusLoaned
import org.nypl.simplified.books.book_registry.BookStatusLoanedMatcherType
import org.nypl.simplified.books.book_registry.BookStatusLoanedType
import org.nypl.simplified.books.book_registry.BookStatusMatcherType
import org.nypl.simplified.books.book_registry.BookStatusRequestingDownload
import org.nypl.simplified.books.book_registry.BookStatusRequestingLoan
import org.nypl.simplified.books.book_registry.BookStatusRequestingRevoke
import org.nypl.simplified.books.book_registry.BookStatusRevokeFailed
import org.nypl.simplified.books.book_registry.BookStatusRevoked
import org.nypl.simplified.books.book_registry.BookStatusType
import org.nypl.simplified.books.controller.api.BooksControllerType
import org.nypl.simplified.books.covers.BookCoverProviderType
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.feeds.api.FeedEntry.FeedEntryCorrupt
import org.nypl.simplified.feeds.api.FeedEntry.FeedEntryOPDS
import org.nypl.simplified.opds.core.OPDSAcquisition
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.profiles.api.ProfileNoneCurrentException
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.slf4j.LoggerFactory
import java.util.Calendar
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
  private val screenSizeInformation: ScreenSizeInformationType) :
  FrameLayout(activity),
  BookStatusMatcherType<Unit, UnreachableCodeException>,
  BookStatusLoanedMatcherType<Unit, UnreachableCodeException>,
  BookStatusDownloadingMatcherType<Unit, UnreachableCodeException> {

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

  override fun onBookStatusDownloaded(d: BookStatusDownloaded): Unit {

    val bookId = d.id
    LOG.debug("{}: downloaded", bookId)

    this.cellBook.visibility = View.VISIBLE
    this.cellCorrupt.visibility = View.INVISIBLE
    this.cellDownloading.visibility = View.INVISIBLE
    this.cellDownloadingFailed.visibility = View.INVISIBLE
    this.setDebugCellText("downloaded")

    val feedEntry = this.entry.get()
    this.loadImageAndSetVisibility(feedEntry)

    this.cellButtons.visibility = View.VISIBLE
    this.cellButtons.removeAllViews()
    this.cellButtons.addView(
      CatalogBookReadButton(
        this.activity,
        this.analytics,
        this.profilesController.profileCurrent(),
        this.account(feedEntry.bookID),
        bookId,
        feedEntry),
      0)

    CatalogBookDetailView.configureButtonMargins(this.screenSizeInformation, this.cellButtons)
    return Unit.unit()
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

  override fun onBookStatusDownloadFailed(status: BookStatusDownloadFailed): Unit {
    LOG.debug("{}: download failed", status.id)

    /*
     * If the download failed because there is no wifi, then mark the book as being
     * loaned. It can be downloaded later.
     */

    if (!this.networkConnectivity.isWifiAvailable) {
      this.onBookStatusLoaned(BookStatusLoaned(status.id, None.none<DateTime>(), false))
      return Unit.unit()
    }

    /*
     * Unset the content description so that the screen reader reads the error message.
     */

    this.contentDescription = null

    this.cellBook.visibility = View.INVISIBLE
    this.cellCorrupt.visibility = View.INVISIBLE
    this.cellDownloading.visibility = View.INVISIBLE
    this.cellDownloadingFailed.visibility = View.VISIBLE
    this.setDebugCellText("download-failed")

    val feedEntry = this.entry.get()
    val oe = feedEntry.feedEntry

    this.cellDownloadingFailedLabel.setText(R.string.catalog_download_failed)
    this.cellDownloadingFailedTitle.text = oe.title

    val account =
      this.profilesController.profileAccountForBook(feedEntry.bookID)

    /*
     * Manually construct a dismiss button.
     */

    val dismiss = Button(this.activity)
    dismiss.text =
      this.activity.resources.getString(R.string.catalog_book_error_dismiss)
    dismiss.contentDescription =
      this.activity.resources.getString(R.string.catalog_accessibility_book_error_dismiss)
    dismiss.setOnClickListener {
      this.booksController.bookBorrowFailedDismiss(account, status.id)
    }

    /*
     * Manually construct a retry button.
     */

    val acquisitionOpt =
      BookAcquisitionSelection.preferredAcquisition(feedEntry.feedEntry.acquisitions)

    /*
     * Theoretically, if the book has ever been downloaded, then the
     * acquisition list must have contained one usable acquisition relation...
     */

    if (!(acquisitionOpt is Some<OPDSAcquisition>)) {
      throw UnreachableCodeException()
    }

    val retry =
      CatalogAcquisitionButton.retryButton(
        acquisition = acquisitionOpt.get(),
        context = this.activity,
        books = this.booksController,
        entry = feedEntry,
        profiles = this.profilesController,
        bookRegistry = this.booksRegistry,
        account = account,
        onWantOpenLoginDialog = this::showLoginDialog)

    this.cellDownloadingFailedButtons.visibility = View.VISIBLE
    this.cellDownloadingFailedButtons.removeAllViews()
    this.cellDownloadingFailedButtons.addView(dismiss)
    this.cellDownloadingFailedButtons.addView(retry)

    CatalogBookDetailView.configureButtonMargins(
      this.screenSizeInformation, this.cellDownloadingFailedButtons)
    return Unit.unit()
  }

  override fun onBookStatusDownloading(o: BookStatusDownloadingType): Unit {
    return o.matchBookDownloadingStatus(this)
  }

  override fun onBookStatusDownloadInProgress(d: BookStatusDownloadInProgress): Unit {
    LOG.debug("{}: downloading", d.id)

    LOG.debug("{}: downloading", d.id)
    this.cellBook.visibility = View.INVISIBLE
    this.cellCorrupt.visibility = View.INVISIBLE
    this.cellDownloading.visibility = View.VISIBLE
    this.cellDownloadingFailed.visibility = View.INVISIBLE
    this.setDebugCellText("download-in-progress")

    val fe = this.entry.get()
    val bookId = d.id
    val oe = fe.feedEntry
    this.cellDownloadingLabel.setText(R.string.catalog_downloading)
    this.cellDownloadingTitle.text = oe.title
    this.cellDownloadingAuthors.text = makeAuthorText(oe)

    CatalogDownloadProgressBar.setProgressBar(
      d.currentTotalBytes,
      d.expectedTotalBytes,
      this.cellDownloadingPercentText,
      this.cellDownloadingProgress)

    this.cellDownloadingCancel.visibility = View.VISIBLE
    this.cellDownloadingCancel.isEnabled = true
    this.cellDownloadingCancel.setOnClickListener { view -> this.booksController.bookDownloadCancel(this.account(fe.bookID), d.id) }

    return Unit.unit()
  }

  override fun onBookStatusHeld(s: BookStatusHeld): Unit {
    LOG.debug("{}: held", s.id)

    this.cellBook.visibility = View.VISIBLE
    this.cellCorrupt.visibility = View.INVISIBLE
    this.cellDownloading.visibility = View.INVISIBLE
    this.cellDownloadingFailed.visibility = View.INVISIBLE
    this.setDebugCellText("held")

    val feedEntry = this.entry.get()
    this.loadImageAndSetVisibility(feedEntry)

    this.cellButtons.visibility = View.VISIBLE
    this.cellButtons.removeAllViews()

    if (s.isRevocable) {
      val revoke = CatalogBookRevokeButton(
        this.activity,
        this.booksController,
        this.account(feedEntry.bookID),
        s.id,
        CatalogBookRevokeType.REVOKE_HOLD)
      this.cellButtons.addView(revoke, 0)
    }

    CatalogBookDetailView.configureButtonMargins(this.screenSizeInformation, this.cellButtons)
    return Unit.unit()
  }

  override fun onBookStatusHeldReady(s: BookStatusHeldReady): Unit {
    LOG.debug("{}: reserved", s.id)
    this.cellBook.visibility = View.VISIBLE
    this.cellCorrupt.visibility = View.INVISIBLE
    this.cellDownloading.visibility = View.INVISIBLE
    this.cellDownloadingFailed.visibility = View.INVISIBLE
    this.setDebugCellText("reserved")

    val feedEntry = this.entry.get()
    this.loadImageAndSetVisibility(feedEntry)
    val account = this.profilesController.profileAccountForBook(feedEntry.bookID)

    this.cellButtons.visibility = View.VISIBLE
    this.cellButtons.removeAllViews()
    CatalogAcquisitionButton.addButtonsToViewGroup(
      context = this.activity,
      books = this.booksController,
      viewGroup = this.cellButtons,
      profiles = this.profilesController,
      bookRegistry = this.booksRegistry,
      account = account,
      entry = feedEntry,
      onWantOpenLoginDialog = this@CatalogFeedBookCellView::showLoginDialog)

    if (s.isRevocable) {
      val revoke = CatalogBookRevokeButton(
        this.activity,
        this.booksController,
        this.account(feedEntry.bookID),
        s.id,
        CatalogBookRevokeType.REVOKE_HOLD)
      this.cellButtons.addView(revoke, 0)
    }

    CatalogBookDetailView.configureButtonMargins(this.screenSizeInformation, this.cellButtons)
    return Unit.unit()
  }

  private fun showLoginDialog() {
    val loginDialog = LoginDialog()
    loginDialog.show(this.activity.supportFragmentManager, "login-dialog")
  }

  override fun onBookStatusHoldable(s: BookStatusHoldable): Unit {
    LOG.debug("{}: holdable", s.id)

    this.cellBook.visibility = View.VISIBLE
    this.cellCorrupt.visibility = View.INVISIBLE
    this.cellDownloading.visibility = View.INVISIBLE
    this.cellDownloadingFailed.visibility = View.INVISIBLE
    this.setDebugCellText("holdable")

    val feedEntry = this.entry.get()
    this.loadImageAndSetVisibility(feedEntry)
    val account = this.profilesController.profileAccountForBook(feedEntry.bookID)

    this.cellButtons.visibility = View.VISIBLE
    this.cellButtons.removeAllViews()
    CatalogAcquisitionButton.addButtonsToViewGroup(
      context = this.activity,
      books = this.booksController,
      viewGroup = this.cellButtons,
      profiles = this.profilesController,
      bookRegistry = this.booksRegistry,
      account = account,
      entry = feedEntry,
      onWantOpenLoginDialog = this@CatalogFeedBookCellView::showLoginDialog)

    CatalogBookDetailView.configureButtonMargins(this.screenSizeInformation, this.cellButtons)
    return Unit.unit()
  }

  override fun onBookStatusLoanable(s: BookStatusLoanable): Unit {
    val fe = this.entry.get()
    this.onBookStatusNone(fe, s.id)
    return Unit.unit()
  }

  override fun onBookStatusRevokeFailed(status: BookStatusRevokeFailed): Unit {
    LOG.debug("{}: revoke failed", status.id)

    /*
     * Unset the content description so that the screen reader reads the error message.
     */

    this.contentDescription = null

    this.cellBook.visibility = View.INVISIBLE
    this.cellCorrupt.visibility = View.INVISIBLE
    this.cellDownloading.visibility = View.INVISIBLE
    this.cellDownloadingFailed.visibility = View.VISIBLE
    this.setDebugCellText("revoke-failed")

    val feedEntry = this.entry.get()

    this.cellDownloadingFailedLabel.setText(R.string.catalog_revoke_failed)
    this.cellDownloadingFailedTitle.text = feedEntry.feedEntry.title

    val account =
      this.profilesController.profileAccountForBook(feedEntry.bookID)

    /*
     * Manually construct a dismiss button.
     */

    val dismiss = Button(this.activity)
    dismiss.text =
      this.activity.resources.getString(R.string.catalog_book_error_dismiss)
    dismiss.contentDescription =
      this.activity.resources.getString(R.string.catalog_accessibility_book_error_dismiss)
    dismiss.setOnClickListener {
      this.booksController.bookBorrowFailedDismiss(account, status.id)
    }

    this.cellDownloadingFailedButtons.visibility = View.VISIBLE
    this.cellDownloadingFailedButtons.removeAllViews()
    this.cellDownloadingFailedButtons.addView(dismiss)

    CatalogBookDetailView.configureButtonMargins(
      this.screenSizeInformation, this.cellDownloadingFailedButtons)
    return Unit.unit()
  }

  override fun onBookStatusRevoked(o: BookStatusRevoked): Unit {
    LOG.debug("{}: revoked", o.id)
    this.cellBook.visibility = View.VISIBLE
    this.cellCorrupt.visibility = View.INVISIBLE
    this.cellDownloading.visibility = View.INVISIBLE
    this.cellDownloadingFailed.visibility = View.INVISIBLE
    this.setDebugCellText("revoked")

    val fe = this.entry.get()
    this.loadImageAndSetVisibility(fe)
    return Unit.unit()
  }

  override fun onBookStatusLoaned(o: BookStatusLoaned): Unit {
    LOG.debug("{}: loaned", o.id)
    this.cellBook.visibility = View.VISIBLE
    this.cellCorrupt.visibility = View.INVISIBLE
    this.cellDownloading.visibility = View.INVISIBLE
    this.cellDownloadingFailed.visibility = View.INVISIBLE
    this.setDebugCellText("loaned")

    val feedEntry = this.entry.get()
    this.loadImageAndSetVisibility(feedEntry)
    val account = this.profilesController.profileAccountForBook(feedEntry.bookID)

    this.cellButtons.visibility = View.VISIBLE
    this.cellButtons.removeAllViews()
    CatalogAcquisitionButton.addButtonsToViewGroup(
      context = this.activity,
      books = this.booksController,
      viewGroup = this.cellButtons,
      profiles = this.profilesController,
      bookRegistry = this.booksRegistry,
      account = account,
      entry = feedEntry,
      onWantOpenLoginDialog = this@CatalogFeedBookCellView::showLoginDialog)

    CatalogBookDetailView.configureButtonMargins(this.screenSizeInformation, this.cellButtons)
    return Unit.unit()
  }

  override fun onBookStatusLoanedType(o: BookStatusLoanedType): Unit {
    return o.matchBookLoanedStatus(this)
  }

  private fun onBookStatusNone(
    newEntry: FeedEntryOPDS,
    id: BookID) {
    LOG.debug("{}: none", id)
    this.cellBook.visibility = View.VISIBLE
    this.cellCorrupt.visibility = View.INVISIBLE
    this.cellDownloading.visibility = View.INVISIBLE
    this.cellDownloadingFailed.visibility = View.INVISIBLE
    this.setDebugCellText("none")

    this.loadImageAndSetVisibility(newEntry)
    val account = this.profilesController.profileAccountForBook(newEntry.bookID)

    this.cellButtons.visibility = View.VISIBLE
    this.cellButtons.removeAllViews()
    CatalogAcquisitionButton.addButtonsToViewGroup(
      context = this.activity,
      books = this.booksController,
      viewGroup = this.cellButtons,
      profiles = this.profilesController,
      bookRegistry = this.booksRegistry,
      account = account,
      entry = newEntry,
      onWantOpenLoginDialog = this@CatalogFeedBookCellView::showLoginDialog)

    CatalogBookDetailView.configureButtonMargins(this.screenSizeInformation, this.cellButtons)
  }

  override fun onBookStatusRequestingDownload(d: BookStatusRequestingDownload): Unit {
    LOG.debug("{}: requesting download", d.id)
    this.cellBook.visibility = View.VISIBLE
    this.cellCorrupt.visibility = View.INVISIBLE
    this.cellDownloading.visibility = View.INVISIBLE
    this.cellDownloadingFailed.visibility = View.INVISIBLE
    this.setDebugCellText("requesting-download")

    val fe = this.entry.get()
    this.loadImageAndSetVisibility(fe)

    this.cellDownloadingLabel.setText(R.string.catalog_downloading)
    this.cellButtons.visibility = View.VISIBLE
    this.cellButtons.removeAllViews()
    return Unit.unit()
  }

  override fun onBookStatusRequestingLoan(s: BookStatusRequestingLoan): Unit {
    LOG.debug("{}: requesting loan", s.id)
    this.cellBook.visibility = View.INVISIBLE
    this.cellCorrupt.visibility = View.INVISIBLE
    this.cellDownloading.visibility = View.VISIBLE
    this.cellDownloadingFailed.visibility = View.INVISIBLE
    this.setDebugCellText("requesting-loan")

    val (oe) = this.entry.get()

    this.cellDownloadingLabel.setText(R.string.catalog_requesting_loan)
    this.cellDownloadingTitle.text = oe.title
    this.cellDownloadingAuthors.text = makeAuthorText(oe)

    CatalogDownloadProgressBar.setProgressBar(
      0,
      100,
      this.cellDownloadingPercentText,
      this.cellDownloadingProgress)

    this.cellDownloadingCancel.visibility = View.INVISIBLE
    this.cellDownloadingCancel.isEnabled = false
    this.cellDownloadingCancel.setOnClickListener(null)
    return Unit.unit()
  }

  override fun onBookStatusRequestingRevoke(s: BookStatusRequestingRevoke): Unit {
    LOG.debug("{}: requesting revoke", s.id)
    this.cellBook.visibility = View.INVISIBLE
    this.cellCorrupt.visibility = View.INVISIBLE
    this.cellDownloading.visibility = View.VISIBLE
    this.cellDownloadingFailed.visibility = View.INVISIBLE
    this.setDebugCellText("requesting-revoke")

    val (oe) = this.entry.get()

    this.cellDownloadingLabel.setText(R.string.catalog_requesting_revoke)
    this.cellDownloadingTitle.text = oe.title
    this.cellDownloadingAuthors.text = makeAuthorText(oe)

    CatalogDownloadProgressBar.setProgressBar(
      0,
      100,
      this.cellDownloadingPercentText,
      this.cellDownloadingProgress)

    this.cellDownloadingCancel.visibility = View.INVISIBLE
    this.cellDownloadingCancel.isEnabled = false
    this.cellDownloadingCancel.setOnClickListener(null)
    return Unit.unit()
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
    this.onStatus(feedE, bookId, booksRegistry.bookStatus(bookId))
    return Unit.unit()
  }

  private fun onStatus(
    inEntry: FeedEntryOPDS,
    id: BookID,
    statusOpt: OptionType<BookStatusType>) {
    if (statusOpt.isSome) {
      val some = statusOpt as Some<BookStatusType>
      UIThread.runOnUIThread { some.get().matchBookStatus(this@CatalogFeedBookCellView) }
    } else {
      UIThread.runOnUIThread { this@CatalogFeedBookCellView.onBookStatusNone(inEntry, id) }
    }
  }

  private fun setDebugCellText(
    text: String) {
    if (this.debugCellState) {
      this.cellDebug.text = text
    }
  }

  /**
   * Configure the overall status of the cell. The cell displays a number of
   * different layouts depending on whether the current book is loaned, fully
   * downloaded, currently downloading, not loaned, etc.
   *
   * @param entry        The new feed entry
   * @param listener A selection listener
   */

  fun viewConfigure(
    entry: FeedEntry,
    listener: CatalogBookSelectionListenerType) {
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
