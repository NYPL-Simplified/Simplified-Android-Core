package org.nypl.simplified.app.catalog

import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.webkit.WebView
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
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
import org.nypl.simplified.app.ApplicationColorScheme
import org.nypl.simplified.app.NetworkConnectivityType
import org.nypl.simplified.app.R
import org.nypl.simplified.app.ScreenSizeInformationType
import org.nypl.simplified.app.utilities.UIThread
import org.nypl.simplified.assertions.Assertions
import org.nypl.simplified.books.accounts.AccountType
import org.nypl.simplified.books.book_database.BookFormats.BookFormatDefinition.BOOK_FORMAT_AUDIO
import org.nypl.simplified.books.book_database.BookFormats.BookFormatDefinition.BOOK_FORMAT_EPUB
import org.nypl.simplified.books.book_database.BookFormats.BookFormatDefinition.BOOK_FORMAT_PDF
import org.nypl.simplified.books.book_registry.BookRegistryReadableType
import org.nypl.simplified.books.book_registry.BookStatusDownloadFailed
import org.nypl.simplified.books.book_registry.BookStatusDownloadInProgress
import org.nypl.simplified.books.book_registry.BookStatusDownloaded
import org.nypl.simplified.books.book_registry.BookStatusDownloadingMatcherType
import org.nypl.simplified.books.book_registry.BookStatusDownloadingType
import org.nypl.simplified.books.book_registry.BookStatusEvent
import org.nypl.simplified.books.book_registry.BookStatusEvent.Type.BOOK_CHANGED
import org.nypl.simplified.books.book_registry.BookStatusEvent.Type.BOOK_REMOVED
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
import org.nypl.simplified.books.controller.BooksControllerType
import org.nypl.simplified.books.controller.ProfilesControllerType
import org.nypl.simplified.books.core.BookAcquisitionSelection
import org.nypl.simplified.books.covers.BookCoverProviderType
import org.nypl.simplified.books.feeds.FeedEntry.FeedEntryOPDS
import org.nypl.simplified.opds.core.OPDSAcquisition
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.opds.core.OPDSAvailabilityOpenAccess
import org.nypl.simplified.stack.ImmutableStack
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.concurrent.atomic.AtomicReference

/**
 * A book detail view.
 */

class CatalogBookDetailView(
  private val activity: AppCompatActivity,
  private val inflater: LayoutInflater,
  private val account: AccountType,
  private val coverProvider: BookCoverProviderType,
  private val booksRegistry: BookRegistryReadableType,
  private val profilesController: ProfilesControllerType,
  private val booksController: BooksControllerType,
  private val screenSizeInformation: ScreenSizeInformationType,
  private val networkConnectivity: NetworkConnectivityType,
  private val applicationColorScheme: ApplicationColorScheme,
  entryInitial: FeedEntryOPDS)
  : BookStatusMatcherType<Unit, UnreachableCodeException>,
  BookStatusLoanedMatcherType<Unit, UnreachableCodeException>,
  BookStatusDownloadingMatcherType<Unit, UnreachableCodeException> {

  private val bookDownload: ViewGroup
  private val bookDownloadButtons: LinearLayout
  private val bookDownloadReportButton: Button
  private val relatedLayout: ViewGroup
  private val relatedBooksButton: Button
  private val bookDownloadText: TextView
  private val bookDownloading: ViewGroup
  private val bookDownloadingCancel: Button
  private val bookDownloadingFailed: ViewGroup
  private val bookDownloadingFailedButtons: LinearLayout
  private val bookDownloadingFailedDismiss: Button
  private val bookDownloadingFailedRetry: Button
  private val bookDownloadingPercentText: TextView
  private val bookDownloadingProgress: ProgressBar
  private val entry: AtomicReference<FeedEntryOPDS> = AtomicReference(entryInitial)

  /**
   * @return The scrolling view containing the book details
   */

  val scrollView: ScrollView

  private val bookDownloadingLabel: TextView
  private val bookDownloadingFailedText: TextView
  private val bookDebugStatus: TextView
  private val bookHeader: ViewGroup
  private val bookHeaderLeft: ViewGroup
  private val bookHeaderTitle: TextView
  private val bookHeaderFormat: TextView
  private val bookHeaderCover: ImageView
  private val bookHeaderAuthors: TextView

  init {
    val sv = ScrollView(this.activity)
    this.scrollView = sv

    val p = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    sv.layoutParams = p
    sv.addOnLayoutChangeListener { v, left, top, right, bottom, old_left, old_top, old_right, old_bottom -> sv.scrollY = 0 }

    val layout = this.inflater.inflate(R.layout.book_dialog, sv, false)
    sv.addView(layout)

    /*
     * Show the book status if status debugging is enabled.
     */

    this.bookDebugStatus = layout.findViewById<View>(R.id.book_debug_status) as TextView
    if (this.activity.resources.getBoolean(R.bool.debug_catalog_cell_view_states)) {
      this.bookDebugStatus.visibility = View.VISIBLE
    } else {
      this.bookDebugStatus.visibility = View.GONE
    }

    this.bookHeader =
      layout.findViewById<View>(R.id.book_header) as ViewGroup
    this.bookHeaderLeft =
      this.bookHeader.findViewById<View>(R.id.book_header_left) as ViewGroup
    this.bookHeaderTitle =
      this.bookHeader.findViewById<View>(R.id.book_header_title) as TextView
    this.bookHeaderFormat =
      this.bookHeader.findViewById<View>(R.id.book_header_format) as TextView
    this.bookHeaderCover =
      this.bookHeader.findViewById<View>(R.id.book_header_cover) as ImageView
    this.bookHeaderAuthors =
      this.bookHeader.findViewById<View>(R.id.book_header_authors) as TextView
    this.bookDownloadButtons =
      this.bookHeader.findViewById<View>(R.id.book_dialog_download_buttons) as LinearLayout
    this.bookDownloadingCancel =
      this.bookHeader.findViewById<View>(R.id.book_dialog_downloading_cancel) as Button
    this.bookDownloadingFailedButtons =
      this.bookHeader.findViewById<View>(R.id.book_dialog_downloading_failed_buttons) as LinearLayout
    this.bookDownloadingFailedDismiss =
      this.bookHeader.findViewById<View>(R.id.book_dialog_downloading_failed_dismiss) as Button
    this.bookDownloadingFailedRetry =
      this.bookHeader.findViewById<View>(R.id.book_dialog_downloading_failed_retry) as Button

    this.bookDownloading =
      layout.findViewById<View>(R.id.book_dialog_downloading) as ViewGroup
    this.bookDownloadingLabel =
      this.bookDownloading.findViewById<View>(R.id.book_dialog_downloading_label) as TextView
    this.bookDownloadingPercentText =
      this.bookDownloading.findViewById<View>(R.id.book_dialog_downloading_percent_text) as TextView
    this.bookDownloadingProgress =
      this.bookDownloading.findViewById<View>(R.id.book_dialog_downloading_progress) as ProgressBar

    this.bookDownloadingFailed =
      layout.findViewById<View>(R.id.book_dialog_downloading_failed) as ViewGroup
    this.bookDownloadingFailedText =
      this.bookDownloadingFailed.findViewById<View>(R.id.book_dialog_downloading_failed_text) as TextView

    this.bookDownload =
      layout.findViewById<View>(R.id.book_dialog_download) as ViewGroup
    this.bookDownloadText =
      this.bookDownload.findViewById<View>(R.id.book_dialog_download_text) as TextView

    val summary =
      layout.findViewById<View>(R.id.book_summary_layout) as ViewGroup
    val summarySectionTitle =
      summary.findViewById<View>(R.id.book_summary_section_title) as TextView
    val summaryText =
      summary.findViewById<View>(R.id.book_summary_text) as WebView
    val headerMeta =
      summary.findViewById<View>(R.id.book_header_meta) as TextView

    val readMoreButton = summary.findViewById<Button>(R.id.book_summary_read_more_button)
    readMoreButton.setTextColor(this.applicationColorScheme.colorRGBA)

    readMoreButton.setOnClickListener { view ->
      CatalogBookDetailView.configureSummaryWebViewHeight(summaryText)
      readMoreButton.visibility = View.INVISIBLE
    }

    this.relatedLayout = layout.findViewById(R.id.book_related_layout)
    this.relatedBooksButton = this.relatedLayout.findViewById(R.id.related_books_button)
    this.relatedBooksButton.setTextColor(this.applicationColorScheme.colorRGBA)
    this.bookDownloadReportButton = layout.findViewById(R.id.book_dialog_report_button)
    this.bookDownloadReportButton.setTextColor(this.applicationColorScheme.colorRGBA)

    /*
     * Assuming a roughly fixed height for cover images, assume a 4:3 aspect
     * ratio and set the width of the cover layout.
     */

    val coverHeight = this.bookHeaderCover.layoutParams.height
    val coverWidth = (coverHeight.toDouble() / 4.0 * 3.0).toInt()
    this.bookHeaderLeft.layoutParams = LinearLayout.LayoutParams(coverWidth, LayoutParams.WRAP_CONTENT)

    /* Configure detail texts. */
    val entryNow = this.entry.get()
    val opdsEntry = entryNow.feedEntry
    CatalogBookDetailView.configureSummarySectionTitle(summarySectionTitle)

    val bookID = entryNow.bookID
    this.onStatus(entryInitial, this.booksRegistry.bookStatus(bookID))

    CatalogBookDetailView.configureSummaryWebView(opdsEntry, summaryText)
    this.bookHeaderTitle.text = opdsEntry.title
    CatalogBookDetailView.configureViewTextFormat(this.activity.resources, entryInitial, this.bookHeaderFormat)
    CatalogBookDetailView.configureViewTextAuthor(opdsEntry, this.bookHeaderAuthors)
    CatalogBookDetailView.configureViewTextMeta(this.activity.resources, opdsEntry, headerMeta)

    val future =
      this.coverProvider.loadCoverInto(
        entryNow,
        this.bookHeaderCover,
        coverWidth,
        coverHeight)

    Futures.addCallback(future, object : FutureCallback<kotlin.Unit?> {
      override fun onSuccess(result: kotlin.Unit?) {

      }

      override fun onFailure(exception: Throwable) {
        org.nypl.simplified.app.catalog.CatalogBookDetailView.Companion.LOG.error("could not load cover: ", exception)
      }
    }, directExecutor())
  }

  override fun onBookStatusDownloaded(downloaded: BookStatusDownloaded): Unit {
    UIThread.checkIsUIThread()

    this.bookDebugStatus.text = "downloaded"
    this.bookDownloadButtons.removeAllViews()

    val text =
      CatalogBookAvailabilityStrings.getAvailabilityString(this.activity.resources, downloaded)

    this.bookDownloadText.text = text
    this.bookDownloadButtons.addView(
      CatalogBookReadButton(
        this.activity,
        this.account,
        downloaded.id,
        this.entry.get(),
        this.applicationColorScheme),
      0)

    if (downloaded.isReturnable) {
      val revoke =
        CatalogBookRevokeButton(
          this.activity,
          this.booksController,
          this.account,
          downloaded.id,
          CatalogBookRevokeType.REVOKE_LOAN)

      this.bookDownloadButtons.addView(revoke, 1)
    } else if (this.entry.get().feedEntry.availability is OPDSAvailabilityOpenAccess) {
      this.bookDownloadButtons.addView(CatalogBookDeleteButton(
        this.activity, this.booksController, this.account, downloaded.id), 1)
    }

    this.bookDownloadButtons.visibility = View.VISIBLE
    CatalogBookDetailView.configureButtonsHeight(
      this.screenSizeInformation,
      this.activity.resources,
      this.bookDownloadButtons)

    this.bookDownload.visibility = View.VISIBLE
    this.bookDownloadButtons.visibility = View.VISIBLE
    this.bookDownloading.visibility = View.INVISIBLE
    this.bookDownloadingCancel.visibility = View.INVISIBLE
    this.bookDownloadingFailed.visibility = View.INVISIBLE
    this.bookDownloadingFailedButtons.visibility = View.INVISIBLE
    return Unit.unit()
  }

  override fun onBookStatusDownloadFailed(status: BookStatusDownloadFailed): Unit {
    UIThread.checkIsUIThread()

    this.bookDebugStatus.text = "download failed"

    /*
     * If the download failed because there was no network, act as if the book was loaned.
     */

    if (!this.networkConnectivity.isWifiAvailable()) {
      this.onBookStatusLoaned(BookStatusLoaned(status.getID(), None.none(), false))
      return Unit.unit()
    }

    this.bookDownload.visibility = View.INVISIBLE
    this.bookDownloadButtons.visibility = View.INVISIBLE
    this.bookDownloading.visibility = View.INVISIBLE
    this.bookDownloadingCancel.visibility = View.INVISIBLE
    this.bookDownloadingFailed.visibility = View.VISIBLE
    this.bookDownloadingFailedButtons.visibility = View.VISIBLE

    val currentEntry = this.entry.get()

    val failed = this.bookDownloadingFailedText
    failed.text = CatalogBookErrorStrings.getFailureString(this.activity.resources, status)

    val dismiss = this.bookDownloadingFailedDismiss
    val retry = this.bookDownloadingFailedRetry

    dismiss.setOnClickListener {
      this.booksController.bookBorrowFailedDismiss(this.account, status.id)
    }

    /*
     * Manually construct an acquisition controller for the retry button.
     */

    val opdsEntry =
      currentEntry.feedEntry
    val acquisitionOpt =
      BookAcquisitionSelection.preferredAcquisition(opdsEntry.acquisitions)

    /*
     * Theoretically, if the book has ever been downloaded, then the
     * acquisition list must have contained one usable acquisition relation...
     */

    if (!(acquisitionOpt is Some<OPDSAcquisition>)) {
      throw UnreachableCodeException()
    }

    val acquisition = acquisitionOpt.get()
    val acquisitionController =
      CatalogAcquisitionButtonController(
        this.activity,
        this.profilesController,
        this.booksController,
        this.booksRegistry,
        this.networkConnectivity,
        currentEntry.bookID,
        acquisition,
        currentEntry)

    retry.isEnabled = true
    retry.visibility = View.VISIBLE
    retry.setOnClickListener(acquisitionController)
    return Unit.unit()
  }

  override fun onBookStatusDownloading(status: BookStatusDownloadingType): Unit {
    return status.matchBookDownloadingStatus(this)
  }

  override fun onBookStatusDownloadInProgress(status: BookStatusDownloadInProgress): Unit {
    UIThread.checkIsUIThread()

    this.bookDebugStatus.text = "download in progress"
    this.bookDownload.visibility = View.INVISIBLE
    this.bookDownloadButtons.visibility = View.INVISIBLE
    this.bookDownloading.visibility = View.VISIBLE
    this.bookDownloadingCancel.visibility = View.VISIBLE
    this.bookDownloadingFailed.visibility = View.INVISIBLE
    this.bookDownloadingFailedButtons.visibility = View.INVISIBLE

    this.bookDownloadingLabel.setText(R.string.catalog_downloading)
    CatalogDownloadProgressBar.setProgressBar(
      status.currentTotalBytes,
      status.expectedTotalBytes,
      this.bookDownloadingPercentText,
      this.bookDownloadingProgress)

    val dc = this.bookDownloadingCancel
    dc.visibility = View.VISIBLE
    dc.isEnabled = true
    dc.setOnClickListener { this.booksController.bookDownloadCancel(this.account, status.id) }
    return Unit.unit()
  }

  override fun onBookStatusHeld(status: BookStatusHeld): Unit {
    UIThread.checkIsUIThread()

    this.bookDebugStatus.text = "held"

    this.bookDownloadButtons.removeAllViews()
    this.bookDownloadButtons.visibility = View.VISIBLE
    this.bookDownload.visibility = View.VISIBLE
    this.bookDownloading.visibility = View.INVISIBLE
    this.bookDownloadingCancel.visibility = View.INVISIBLE
    this.bookDownloadingFailed.visibility = View.INVISIBLE
    this.bookDownloadingFailedButtons.visibility = View.INVISIBLE

    if (status.isRevocable) {
      val revoke =
        CatalogBookRevokeButton(
          this.activity,
          this.booksController,
          this.account,
          status.id,
          CatalogBookRevokeType.REVOKE_HOLD)
      this.bookDownloadButtons.addView(revoke, 0)
    }

    CatalogBookDetailView.configureButtonsHeight(
      this.screenSizeInformation,
      this.activity.resources,
      this.bookDownloadButtons)

    val text =
      CatalogBookAvailabilityStrings.getAvailabilityString(this.activity.resources, status)

    this.bookDownloadText.text = text
    return Unit.unit()
  }

  override fun onBookStatusHeldReady(status: BookStatusHeldReady): Unit {
    UIThread.checkIsUIThread()

    this.bookDebugStatus.text = "held-ready"

    this.bookDownloadButtons.removeAllViews()
    this.bookDownloadButtons.visibility = View.VISIBLE
    this.bookDownload.visibility = View.VISIBLE
    this.bookDownloading.visibility = View.INVISIBLE
    this.bookDownloadingCancel.visibility = View.INVISIBLE
    this.bookDownloadingFailed.visibility = View.INVISIBLE
    this.bookDownloadingFailedButtons.visibility = View.INVISIBLE

    val text =
      CatalogBookAvailabilityStrings.getAvailabilityString(this.activity.resources, status)
    this.bookDownloadText.text = text

    CatalogAcquisitionButtons.addButtons(
      this.activity,
      this.account,
      this.bookDownloadButtons,
      this.booksController,
      this.profilesController,
      this.booksRegistry,
      this.entry.get())

    if (status.isRevocable) {
      val revoke =
        CatalogBookRevokeButton(
          this.activity,
          this.booksController,
          this.account,
          status.id,
          CatalogBookRevokeType.REVOKE_HOLD)
      this.bookDownloadButtons.addView(revoke, 0)
    }

    CatalogBookDetailView.configureButtonsHeight(
      this.screenSizeInformation,
      this.activity.resources,
      this.bookDownloadButtons)
    return Unit.unit()
  }

  override fun onBookStatusHoldable(status: BookStatusHoldable): Unit {
    UIThread.checkIsUIThread()

    this.bookDebugStatus.text = "holdable"

    this.bookDownloadButtons.removeAllViews()
    this.bookDownloadButtons.visibility = View.VISIBLE
    this.bookDownload.visibility = View.VISIBLE
    this.bookDownloading.visibility = View.INVISIBLE
    this.bookDownloadingCancel.visibility = View.INVISIBLE
    this.bookDownloadingFailed.visibility = View.INVISIBLE
    this.bookDownloadingFailedButtons.visibility = View.INVISIBLE

    val text =
      CatalogBookAvailabilityStrings.getAvailabilityString(this.activity.resources, status)
    this.bookDownloadText.text = text

    CatalogAcquisitionButtons.addButtons(
      this.activity,
      this.account,
      this.bookDownloadButtons,
      this.booksController,
      this.profilesController,
      this.booksRegistry,
      this.entry.get())

    CatalogBookDetailView.configureButtonsHeight(
      this.screenSizeInformation,
      this.activity.resources,
      this.bookDownloadButtons)
    return Unit.unit()
  }

  override fun onBookStatusLoanable(status: BookStatusLoanable): Unit {
    UIThread.checkIsUIThread()

    this.onBookStatusNone(this.entry.get())
    this.bookDebugStatus.text = "loanable"
    return Unit.unit()
  }

  override fun onBookStatusRevokeFailed(status: BookStatusRevokeFailed): Unit {
    UIThread.checkIsUIThread()

    this.bookDebugStatus.text = "revoke failed"
    this.bookDownload.visibility = View.INVISIBLE
    this.bookDownloading.visibility = View.INVISIBLE
    this.bookDownloadingCancel.visibility = View.INVISIBLE
    this.bookDownloadingFailed.visibility = View.VISIBLE
    this.bookDownloadingFailedButtons.visibility = View.VISIBLE

    val failed = this.bookDownloadingFailedText
    failed.setText(R.string.catalog_revoke_failed)

    val dismiss = this.bookDownloadingFailedDismiss
    val retry = this.bookDownloadingFailedRetry

    dismiss.setOnClickListener {
      this.booksController.bookBorrowFailedDismiss(this.account, status.id)
    }

    retry.isEnabled = false
    retry.visibility = View.GONE
    return Unit.unit()
  }

  override fun onBookStatusRevoked(status: BookStatusRevoked): Unit {
    UIThread.checkIsUIThread()

    this.bookDebugStatus.text = "revoked"

    this.bookDownloadButtons.removeAllViews()
    this.bookDownloadButtons.visibility = View.VISIBLE
    this.bookDownload.visibility = View.VISIBLE
    this.bookDownloading.visibility = View.INVISIBLE
    this.bookDownloadingCancel.visibility = View.INVISIBLE
    this.bookDownloadingFailed.visibility = View.INVISIBLE
    this.bookDownloadingFailedButtons.visibility = View.INVISIBLE

    val text = CatalogBookAvailabilityStrings.getAvailabilityString(this.activity.resources, status)
    this.bookDownloadText.text = text

    val revoke =
      CatalogBookRevokeButton(
        this.activity,
        this.booksController,
        this.account,
        status.id,
        CatalogBookRevokeType.REVOKE_LOAN)

    this.bookDownloadButtons.addView(revoke, 0)

    CatalogBookDetailView.configureButtonsHeight(
      this.screenSizeInformation,
      this.activity.resources,
      this.bookDownloadButtons)

    return Unit.unit()
  }

  override fun onBookStatusLoaned(status: BookStatusLoaned): Unit {
    UIThread.checkIsUIThread()

    this.bookDebugStatus.text = "loaned"

    this.bookDownloadButtons.removeAllViews()
    this.bookDownloadButtons.visibility = View.VISIBLE
    this.bookDownload.visibility = View.VISIBLE
    this.bookDownloading.visibility = View.INVISIBLE
    this.bookDownloadingCancel.visibility = View.INVISIBLE
    this.bookDownloadingFailed.visibility = View.INVISIBLE
    this.bookDownloadingFailedButtons.visibility = View.INVISIBLE

    val text =
      CatalogBookAvailabilityStrings.getAvailabilityString(this.activity.resources, status)
    this.bookDownloadText.text = text

    CatalogAcquisitionButtons.addButtons(
      this.activity,
      this.account,
      this.bookDownloadButtons,
      this.booksController,
      this.profilesController,
      this.booksRegistry,
      this.entry.get())

    if (status.isReturnable) {
      val revoke =
        CatalogBookRevokeButton(
          this.activity,
          this.booksController,
          this.account,
          status.id,
          CatalogBookRevokeType.REVOKE_LOAN)
      this.bookDownloadButtons.addView(revoke, 1)
    }

    CatalogBookDetailView.configureButtonsHeight(
      this.screenSizeInformation,
      this.activity.resources,
      this.bookDownloadButtons)

    return Unit.unit()
  }

  override fun onBookStatusLoanedType(status: BookStatusLoanedType): Unit {
    return status.matchBookLoanedStatus(this)
  }

  private fun onBookStatusNone(entry: FeedEntryOPDS) {
    UIThread.checkIsUIThread()

    this.bookDebugStatus.text = "none"

    this.bookDownloadButtons.removeAllViews()
    this.bookDownloadButtons.visibility = View.VISIBLE
    this.bookDownload.visibility = View.VISIBLE
    this.bookDownloading.visibility = View.INVISIBLE
    this.bookDownloadingCancel.visibility = View.INVISIBLE
    this.bookDownloadingFailed.visibility = View.INVISIBLE
    this.bookDownloadingFailedButtons.visibility = View.INVISIBLE

    val opdsEntry = entry.feedEntry
    val availability = opdsEntry.availability
    val text =
      CatalogBookAvailabilityStrings.getOPDSAvailabilityString(
        this.activity.resources,
        availability)
    this.bookDownloadText.text = text

    CatalogAcquisitionButtons.addButtons(
      this.activity,
      this.account,
      this.bookDownloadButtons,
      this.booksController,
      this.profilesController,
      this.booksRegistry,
      this.entry.get())

    CatalogBookDetailView.configureButtonsHeight(
      this.screenSizeInformation,
      this.activity.resources,
      this.bookDownloadButtons)
  }

  override fun onBookStatusRequestingDownload(status: BookStatusRequestingDownload): Unit {
    UIThread.checkIsUIThread()

    this.bookDebugStatus.text = "requesting download"

    this.bookDownload.visibility = View.INVISIBLE
    this.bookDownloadButtons.visibility = View.INVISIBLE
    this.bookDownloading.visibility = View.VISIBLE
    this.bookDownloadingCancel.visibility = View.VISIBLE
    this.bookDownloadingFailed.visibility = View.INVISIBLE
    this.bookDownloadingFailedButtons.visibility = View.INVISIBLE

    this.bookDownloadingLabel.setText(R.string.catalog_downloading)

    CatalogDownloadProgressBar.setProgressBar(
      0,
      100,
      this.bookDownloadingPercentText,
      this.bookDownloadingProgress)

    val dc = this.bookDownloadingCancel
    dc.isEnabled = false
    dc.visibility = View.INVISIBLE
    dc.setOnClickListener(null)
    return Unit.unit()
  }

  override fun onBookStatusRequestingLoan(status: BookStatusRequestingLoan): Unit {
    UIThread.checkIsUIThread()

    this.bookDebugStatus.text = "requesting loan"

    this.bookDownload.visibility = View.INVISIBLE
    this.bookDownloadButtons.visibility = View.INVISIBLE
    this.bookDownloading.visibility = View.VISIBLE
    this.bookDownloadingCancel.visibility = View.VISIBLE
    this.bookDownloadingFailed.visibility = View.INVISIBLE
    this.bookDownloadingFailedButtons.visibility = View.INVISIBLE

    this.bookDownloadingLabel.setText(R.string.catalog_requesting_loan)

    CatalogDownloadProgressBar.setProgressBar(
      0,
      100,
      this.bookDownloadingPercentText,
      this.bookDownloadingProgress)

    val dc = this.bookDownloadingCancel
    dc.isEnabled = false
    dc.visibility = View.INVISIBLE
    dc.setOnClickListener(null)
    return Unit.unit()
  }

  override fun onBookStatusRequestingRevoke(status: BookStatusRequestingRevoke): Unit {
    UIThread.checkIsUIThread()

    this.bookDebugStatus.text = "requesting revoke"

    this.bookDownload.visibility = View.INVISIBLE
    this.bookDownloadButtons.visibility = View.INVISIBLE
    this.bookDownloading.visibility = View.VISIBLE
    this.bookDownloadingCancel.visibility = View.VISIBLE
    this.bookDownloadingFailed.visibility = View.INVISIBLE
    this.bookDownloadingFailedButtons.visibility = View.INVISIBLE

    this.bookDownloadingLabel.setText(R.string.catalog_requesting_loan)

    CatalogDownloadProgressBar.setProgressBar(
      0,
      100,
      this.bookDownloadingPercentText,
      this.bookDownloadingProgress)

    val dc = this.bookDownloadingCancel
    dc.isEnabled = false
    dc.visibility = View.INVISIBLE
    dc.setOnClickListener(null)
    return Unit.unit()
  }

  private fun onStatus(
    entry: FeedEntryOPDS,
    status_opt: OptionType<BookStatusType>) {
    if (status_opt is Some<BookStatusType>) {
      UIThread.runOnUIThread { status_opt.get().matchBookStatus(this) }
    } else {
      UIThread.runOnUIThread { this.onBookStatusNone(entry) }
    }

    val relatedBookLink = this.entry.get().feedEntry.related
    val relatedBookListener = OnClickListener {
      if (relatedBookLink is Some<URI>) {
        val empty =
          ImmutableStack.empty<CatalogFeedArgumentsType>()

        val remoteArgs =
          CatalogFeedArgumentsRemote(
            false,
            empty,
            "Related Books",
            relatedBookLink.get(),
            false)

        val b = Bundle()
        CatalogFeedActivity.setActivityArguments(b, remoteArgs)
        val i = Intent(this@CatalogBookDetailView.activity, MainCatalogActivity::class.java)
        i.putExtras(b)

        this.activity.startActivity(i, null)
      }
    }

    if (relatedBookLink is Some<URI>) {
      val booksButton = this.relatedBooksButton
      UIThread.runOnUIThread {
        this.relatedLayout.visibility = View.VISIBLE
        booksButton.setOnClickListener(relatedBookListener)
      }
    }

    UIThread.runOnUIThread {
      this.bookDownloadReportButton.setOnClickListener(CatalogBookReport(this.activity, entry))
    }
  }

  fun onBookEvent(event: BookStatusEvent) {
    NullCheck.notNull(event, "Event")

    val updateID = event.book()
    val currentEntry = this.entry.get()
    val currentID = currentEntry.bookID

    if (currentID == updateID) {
      when (event.type()) {
        BOOK_CHANGED -> {
          val bookWithStatus = this.booksRegistry.books().get(updateID)
          if (bookWithStatus != null) {
            this.entry.set(FeedEntryOPDS(bookWithStatus.book().entry))
            UIThread.runOnUIThread { bookWithStatus.status().matchBookStatus(this) }
            return
          }

          UIThread.runOnUIThread { this.onBookStatusNone(currentEntry) }
        }

        BOOK_REMOVED -> {
          UIThread.runOnUIThread { this.onBookStatusNone(currentEntry) }
        }
      }
    }
  }

  companion object {

    private val GENRES_URI: URI =
      URI.create("http://librarysimplified.org/terms/genres/Simplified/")
    private val GENRES_URI_TEXT: String =
      this.GENRES_URI.toString()
    private val LOG: Logger =
      LoggerFactory.getLogger(CatalogBookDetailView::class.java)

    private fun configureButtonsHeight(
      screen: ScreenSizeInformationType,
      resources: Resources,
      layout: LinearLayout) {

      val dp35 = resources.getDimension(R.dimen.button_standard_height).toInt()
      val dp8 = screen.screenDPToPixels(8).toInt()
      val buttonCount = layout.childCount

      for (index in 0 until buttonCount) {
        val v = layout.getChildAt(index)

        Assertions.checkPrecondition(
          v is CatalogBookButtonType,
          "view %s is an instance of CatalogBookButtonType",
          v)

        val lp = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, dp35)
        lp.leftMargin = dp8
        v.layoutParams = lp
      }
    }

    private fun configureSummarySectionTitle(summarySectionTitle: TextView) {
      summarySectionTitle.text = "Description"
    }

    private fun configureSummaryWebView(
      entry: OPDSAcquisitionFeedEntry,
      summaryText: WebView) {
      val text = StringBuilder()
      text.append("<html>")
      text.append("<head>")
      text.append("<style>body {")
      text.append("padding: 0;")
      text.append("padding-right: 2em;")
      text.append("margin: 0;")
      text.append("}</style>")
      text.append("</head>")
      text.append("<body>")
      text.append(entry.summary)
      text.append("</body>")
      text.append("</html>")

      val summaryTextSettings = summaryText.settings
      summaryTextSettings.allowContentAccess = false
      summaryTextSettings.allowFileAccess = false
      summaryTextSettings.allowFileAccessFromFileURLs = false
      summaryTextSettings.allowUniversalAccessFromFileURLs = false
      summaryTextSettings.blockNetworkLoads = true
      summaryTextSettings.blockNetworkImage = true
      summaryTextSettings.defaultTextEncodingName = "UTF-8"
      summaryTextSettings.defaultFixedFontSize = 14
      summaryTextSettings.defaultFontSize = 14
      summaryText.loadDataWithBaseURL(
        null,
        text.toString(),
        "text/html",
        "UTF-8",
        null)
    }

    /**
     * Configure the given web view to match the height of the rendered content.
     */

    private fun configureSummaryWebViewHeight(summaryText: WebView) {
      summaryText.layoutParams =
        LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    private fun configureViewTextAuthor(
      entry: OPDSAcquisitionFeedEntry,
      authors: TextView) {
      val buffer = StringBuilder()
      val `as` = entry.authors
      for (index in `as`.indices) {
        val a = `as`[index]
        if (index > 0) {
          buffer.append("\n")
        }
        buffer.append(a)
      }
      authors.text = buffer.toString()
    }

    private fun configureViewTextMeta(
      resources: Resources,
      entry: OPDSAcquisitionFeedEntry,
      meta: TextView) {
      val buffer = StringBuilder()
      CatalogBookDetailView.createViewTextPublicationDate(resources, entry, buffer)
      CatalogBookDetailView.createViewTextPublisher(resources, entry, buffer)
      CatalogBookDetailView.createViewTextCategories(resources, entry, buffer)
      CatalogBookDetailView.createViewTextDistributor(resources, entry, buffer)
      meta.text = buffer.toString()
    }

    private fun createViewTextCategories(
      resources: Resources,
      entry: OPDSAcquisitionFeedEntry,
      buffer: StringBuilder) {

      val cats = entry.categories
      var hasGenres = false
      for (index in cats.indices) {
        val c = cats[index]
        if (CatalogBookDetailView.GENRES_URI_TEXT == c.scheme) {
          hasGenres = true
        }
      }

      if (hasGenres) {
        if (buffer.length > 0) {
          buffer.append("\n")
        }

        buffer.append(resources.getString(R.string.catalog_categories))
        buffer.append(": ")

        for (index in cats.indices) {
          val c = cats[index]
          if (CatalogBookDetailView.GENRES_URI_TEXT == c.scheme) {
            buffer.append(c.effectiveLabel)
            if (index + 1 < cats.size) {
              buffer.append(", ")
            }
          }
        }
      }
    }

    private fun createViewTextPublicationDate(
      resources: Resources,
      entry: OPDSAcquisitionFeedEntry,
      buffer: StringBuilder): String {
      if (buffer.length > 0) {
        buffer.append("\n")
      }

      val publishedOpt = entry.published
      if (publishedOpt is Some<Calendar>) {
        val published = publishedOpt.get()
        val fmt = SimpleDateFormat("yyyy-MM-dd")
        buffer.append(resources.getString(R.string.catalog_publication_date))
        buffer.append(": ")
        buffer.append(fmt.format(published.time))
        return buffer.toString()
      }

      return ""
    }

    private fun createViewTextPublisher(
      resources: Resources,
      entry: OPDSAcquisitionFeedEntry,
      buffer: StringBuilder) {
      val publisher = entry.publisher
      if (publisher is Some<String>) {
        if (buffer.length > 0) {
          buffer.append("\n")
        }

        buffer.append(resources.getString(R.string.catalog_publisher))
        buffer.append(": ")
        buffer.append(publisher.get())
      }
    }

    private fun createViewTextDistributor(
      resources: Resources,
      entry: OPDSAcquisitionFeedEntry,
      buffer: StringBuilder) {
      if (buffer.length > 0) {
        buffer.append("\n")
      }

      buffer.append(
        String.format(resources.getString(R.string.catalog_book_distribution),
          entry.distribution))
    }

    fun configureViewTextFormat(
      resources: Resources,
      entry: FeedEntryOPDS,
      bookHeaderFormat: TextView) {
      when (entry.probableFormat) {
        BOOK_FORMAT_EPUB -> {
          // Not showing the text for epub format books is deliberate!
          bookHeaderFormat.visibility = View.INVISIBLE
          bookHeaderFormat.text = resources.getText(R.string.book_format_epub)
        }
        BOOK_FORMAT_AUDIO -> {
          bookHeaderFormat.visibility = View.VISIBLE
          bookHeaderFormat.text = resources.getText(R.string.book_format_audiobook)
        }
        BOOK_FORMAT_PDF -> {
          bookHeaderFormat.visibility = View.INVISIBLE
        }
        null -> {
          bookHeaderFormat.visibility = View.INVISIBLE
        }
      }
    }
  }
}
