package org.nypl.simplified.app.catalog

import android.app.Activity
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.webkit.WebView
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.TextView
import com.io7m.jfunctional.OptionType
import com.io7m.jfunctional.Some
import com.io7m.jfunctional.Unit
import com.io7m.jnull.Nullable
import com.io7m.junreachable.UnreachableCodeException
import com.squareup.picasso.Callback
import org.nypl.simplified.app.LoginActivity
import org.nypl.simplified.app.R
import org.nypl.simplified.app.Simplified
import org.nypl.simplified.app.ThemeMatcher
import org.nypl.simplified.app.utilities.UIThread
import org.nypl.simplified.assertions.Assertions
import org.nypl.simplified.books.core.BookAcquisitionSelection
import org.nypl.simplified.books.core.BookDatabaseEntrySnapshot
import org.nypl.simplified.books.core.BookDatabaseReadableType
import org.nypl.simplified.books.core.BookFormats.BookFormatDefinition
import org.nypl.simplified.books.core.BookFormats.BookFormatDefinition.BOOK_FORMAT_AUDIO
import org.nypl.simplified.books.core.BookFormats.BookFormatDefinition.BOOK_FORMAT_EPUB
import org.nypl.simplified.books.core.BookID
import org.nypl.simplified.books.core.BookStatusDownloadFailed
import org.nypl.simplified.books.core.BookStatusDownloadInProgress
import org.nypl.simplified.books.core.BookStatusDownloaded
import org.nypl.simplified.books.core.BookStatusDownloadingMatcherType
import org.nypl.simplified.books.core.BookStatusDownloadingType
import org.nypl.simplified.books.core.BookStatusHeld
import org.nypl.simplified.books.core.BookStatusHeldReady
import org.nypl.simplified.books.core.BookStatusHoldable
import org.nypl.simplified.books.core.BookStatusLoanable
import org.nypl.simplified.books.core.BookStatusLoaned
import org.nypl.simplified.books.core.BookStatusLoanedMatcherType
import org.nypl.simplified.books.core.BookStatusLoanedType
import org.nypl.simplified.books.core.BookStatusMatcherType
import org.nypl.simplified.books.core.BookStatusRequestingDownload
import org.nypl.simplified.books.core.BookStatusRequestingLoan
import org.nypl.simplified.books.core.BookStatusRequestingRevoke
import org.nypl.simplified.books.core.BookStatusRevokeFailed
import org.nypl.simplified.books.core.BookStatusRevoked
import org.nypl.simplified.books.core.BookStatusType
import org.nypl.simplified.books.core.BooksStatusCacheType
import org.nypl.simplified.books.core.BooksType
import org.nypl.simplified.books.core.FeedEntryCorrupt
import org.nypl.simplified.books.core.FeedEntryMatcherType
import org.nypl.simplified.books.core.FeedEntryOPDS
import org.nypl.simplified.books.core.FeedEntryType
import org.nypl.simplified.books.core.LogUtilities
import org.nypl.simplified.opds.core.OPDSAcquisition
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.opds.core.OPDSAvailabilityOpenAccess
import org.nypl.simplified.stack.ImmutableStack
import org.slf4j.Logger
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Observable
import java.util.Observer
import java.util.concurrent.atomic.AtomicReference

/**
 * A book detail view.
 */

class CatalogBookDetailView(
  private val activity: Activity,
  private val inflater: LayoutInflater,
  entryInitial: FeedEntryOPDS)
  : Observer,
  BookStatusMatcherType<Unit, UnreachableCodeException>,
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
  private val books: BooksType
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
  private val bookHeaderCover: ImageView
  private val bookHeaderAuthors: TextView
  private val bookHeaderCoverBadge: ImageView

  init {
    val sv = ScrollView(this.activity)
    this.scrollView = sv

    val p = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    sv.layoutParams = p
    sv.addOnLayoutChangeListener { v, left, top, right, bottom, old_left, old_top, old_right, old_bottom -> sv.scrollY = 0 }

    val layout = this.inflater.inflate(R.layout.book_dialog, sv, false)
    sv.addView(layout)

    val catalogServices = Simplified.getCatalogAppServices()
    this.books = catalogServices.books

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
      bookHeader.findViewById<View>(R.id.book_header_left) as ViewGroup
    this.bookHeaderTitle =
      bookHeader.findViewById<View>(R.id.book_header_title) as TextView
    this.bookHeaderCover =
      bookHeader.findViewById<View>(R.id.book_header_cover) as ImageView
    this.bookHeaderCoverBadge =
      bookHeader.findViewById<View>(R.id.book_header_cover_badge) as ImageView
    this.bookHeaderAuthors =
      bookHeader.findViewById<View>(R.id.book_header_authors) as TextView
    this.bookDownloadButtons =
      bookHeader.findViewById<View>(R.id.book_dialog_download_buttons) as LinearLayout
    this.bookDownloadingCancel =
      bookHeader.findViewById<View>(R.id.book_dialog_downloading_cancel) as Button
    this.bookDownloadingFailedButtons =
      bookHeader.findViewById<View>(R.id.book_dialog_downloading_failed_buttons) as LinearLayout
    this.bookDownloadingFailedDismiss =
      bookHeader.findViewById<View>(R.id.book_dialog_downloading_failed_dismiss) as Button
    this.bookDownloadingFailedRetry =
      bookHeader.findViewById<View>(R.id.book_dialog_downloading_failed_retry) as Button

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

    val resID =
      ThemeMatcher.color(Simplified.getCurrentAccount().mainColor)
    val mainTextColor =
      ContextCompat.getColor(this.activity.baseContext, resID)

    val readMoreButton = summary.findViewById<Button>(R.id.book_summary_read_more_button)
    readMoreButton.setTextColor(mainTextColor)

    readMoreButton.setOnClickListener { view ->
      CatalogBookDetailView.configureSummaryWebViewHeight(summaryText)
      readMoreButton.visibility = View.INVISIBLE
    }

    this.relatedLayout = layout.findViewById(R.id.book_related_layout)
    this.relatedBooksButton = this.relatedLayout.findViewById(R.id.related_books_button)
    this.relatedBooksButton.setTextColor(mainTextColor)
    this.bookDownloadReportButton = layout.findViewById(R.id.book_dialog_report_button)
    this.bookDownloadReportButton.setTextColor(mainTextColor)

    /* Assuming a roughly fixed height for cover images, assume a 4:3 aspect
     * ratio and set the width of the cover layout. */

    val coverHeight = this.bookHeaderCover.layoutParams.height
    val coverWidth = (coverHeight.toDouble() / 4.0 * 3.0).toInt()
    this.bookHeaderLeft.layoutParams = LinearLayout.LayoutParams(coverWidth, LayoutParams.WRAP_CONTENT)

    /* Configure detail texts. */
    val entryNow = this.entry.get()
    val opdsEntry = entryNow.feedEntry
    CatalogBookDetailView.configureSummarySectionTitle(summarySectionTitle)

    val bookID = entryNow.bookID
    val statusCache = this.books.bookGetStatusCache()
    val statusOpt = statusCache.booksStatusGet(bookID)
    this.onStatus(entryNow, statusOpt)

    CatalogBookDetailView.configureSummaryWebView(opdsEntry, summaryText)
    this.bookHeaderTitle.text = opdsEntry.title
    CatalogBookDetailView.configureViewTextAuthor(opdsEntry, this.bookHeaderAuthors)
    CatalogBookDetailView.configureViewTextMeta(this.activity.resources, opdsEntry, headerMeta)

    this.bookHeaderCoverBadge.visibility = GONE
    catalogServices.coverProvider.loadCoverIntoWithCallback(
      entryNow,
      this.bookHeaderCover,
      coverWidth,
      coverHeight,
      object : Callback {
        override fun onSuccess() {
          CatalogCoverBadges.configureBadgeForEntry(entryNow, bookHeaderCoverBadge, 32)
        }

        override fun onError() {

        }
      })
  }

  override fun onBookStatusDownloaded(downloaded: BookStatusDownloaded): Unit {
    this.bookDebugStatus.text = "downloaded"
    this.bookDownloadButtons.removeAllViews()

    val text =
      CatalogBookAvailabilityStrings.getAvailabilityString(this.activity.resources, downloaded)

    this.bookDownloadText.text = text
    this.bookDownloadButtons.addView(
      CatalogBookReadButton(this.activity, downloaded.id, this.entry.get(), this.books), 0)

    if (downloaded.isReturnable) {
      val revoke = CatalogBookRevokeButton(
        this.activity, downloaded.id, CatalogBookRevokeType.REVOKE_LOAN, this.books)
      this.bookDownloadButtons.addView(revoke, 1)
    } else if (this.entry.get().feedEntry.availability is OPDSAvailabilityOpenAccess) {
      this.bookDownloadButtons.addView(
        CatalogBookDeleteButton(
          this.activity, downloaded.id), 1)
    }

    this.bookDownloadButtons.visibility = View.VISIBLE
    CatalogBookDetailView.configureButtonsHeight(this.activity.resources, this.bookDownloadButtons)

    this.bookDownload.visibility = View.VISIBLE
    this.bookDownloadButtons.visibility = View.VISIBLE
    this.bookDownloading.visibility = View.INVISIBLE
    this.bookDownloadingCancel.visibility = View.INVISIBLE
    this.bookDownloadingFailed.visibility = View.INVISIBLE
    this.bookDownloadingFailedButtons.visibility = View.INVISIBLE
    return Unit.unit()
  }

  override fun onBookStatusDownloadFailed(status: BookStatusDownloadFailed): Unit {
    if (CatalogBookUnauthorized.isUnAuthorized(status)) {
      this.books.accountRemoveCredentials()
    }

    this.bookDebugStatus.text = "download failed"

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

    dismiss.setOnClickListener { v -> this.books.bookDownloadAcknowledge(status.id) }

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
    val acquisitionController = CatalogAcquisitionButtonController(
      this.activity, this.books, currentEntry.bookID, acquisition, currentEntry)

    retry.isEnabled = true
    retry.visibility = View.VISIBLE
    retry.setOnClickListener(acquisitionController)
    return Unit.unit()
  }

  override fun onBookStatusDownloading(status: BookStatusDownloadingType): Unit {
    return status.matchBookDownloadingStatus(this)
  }

  override fun onBookStatusDownloadInProgress(status: BookStatusDownloadInProgress): Unit {
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
    dc.setOnClickListener { this.books.bookDownloadCancel(status.id) }
    return Unit.unit()
  }

  override fun onBookStatusHeld(status: BookStatusHeld): Unit {
    this.bookDebugStatus.text = "held"

    this.bookDownloadButtons.removeAllViews()
    this.bookDownloadButtons.visibility = View.VISIBLE
    this.bookDownload.visibility = View.VISIBLE
    this.bookDownloading.visibility = View.INVISIBLE
    this.bookDownloadingCancel.visibility = View.INVISIBLE
    this.bookDownloadingFailed.visibility = View.INVISIBLE
    this.bookDownloadingFailedButtons.visibility = View.INVISIBLE

    if (status.isRevocable) {
      val revoke = CatalogBookRevokeButton(
        this.activity, status.id, CatalogBookRevokeType.REVOKE_HOLD, this.books)
      this.bookDownloadButtons.addView(revoke, 0)
    }

    CatalogBookDetailView.configureButtonsHeight(
      this.activity.resources,
      this.bookDownloadButtons)

    val text =
      CatalogBookAvailabilityStrings.getAvailabilityString(this.activity.resources, status)

    this.bookDownloadText.text = text
    return Unit.unit()
  }

  override fun onBookStatusHeldReady(status: BookStatusHeldReady): Unit {
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
      this.bookDownloadButtons,
      this.books,
      this.entry.get())

    if (status.isRevocable) {
      val revoke = CatalogBookRevokeButton(
        this.activity, status.id, CatalogBookRevokeType.REVOKE_HOLD, this.books)
      this.bookDownloadButtons.addView(revoke, 0)
    }

    CatalogBookDetailView.configureButtonsHeight(
      this.activity.resources, this.bookDownloadButtons)
    return Unit.unit()
  }

  override fun onBookStatusHoldable(status: BookStatusHoldable): Unit {
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
      this.bookDownloadButtons,
      this.books,
      this.entry.get())

    CatalogBookDetailView.configureButtonsHeight(this.activity.resources, this.bookDownloadButtons)
    return Unit.unit()
  }

  override fun onBookStatusLoanable(status: BookStatusLoanable): Unit {
    this.onBookStatusNone(this.entry.get())
    this.bookDebugStatus.text = "loanable"
    return Unit.unit()
  }

  override fun onBookStatusRevokeFailed(status: BookStatusRevokeFailed): Unit {
    if (CatalogBookUnauthorized.isUnAuthorized(status)) {
      this.books.accountRemoveCredentials()
      UIThread.runOnUIThread {
        val i = Intent(this.activity, LoginActivity::class.java)
        this.activity.startActivity(i)
        this.activity.overridePendingTransition(0, 0)
        this.activity.finish()
      }
    } else {
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

      dismiss.setOnClickListener { this.books.bookGetLatestStatusFromDisk(status.id) }
      retry.isEnabled = false
      retry.visibility = View.GONE
    }
    return Unit.unit()
  }

  override fun onBookStatusRevoked(status: BookStatusRevoked): Unit {
    this.bookDebugStatus.text = "revoked"

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

    val revoke = CatalogBookRevokeButton(
      this.activity, status.id, CatalogBookRevokeType.REVOKE_LOAN, this.books)
    this.bookDownloadButtons.addView(revoke, 0)

    CatalogBookDetailView.configureButtonsHeight(this.activity.resources, this.bookDownloadButtons)
    return Unit.unit()
  }

  override fun onBookStatusLoaned(status: BookStatusLoaned): Unit {
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
      this.bookDownloadButtons,
      this.books,
      this.entry.get())

    if (status.isReturnable) {
      val revoke = CatalogBookRevokeButton(
        this.activity, status.id, CatalogBookRevokeType.REVOKE_LOAN, this.books)
      this.bookDownloadButtons.addView(revoke, 1)
    }

    CatalogBookDetailView.configureButtonsHeight(this.activity.resources, this.bookDownloadButtons)
    return Unit.unit()
  }

  override fun onBookStatusLoanedType(status: BookStatusLoanedType): Unit {
    return status.matchBookLoanedStatus(this)
  }

  private fun onBookStatusNone(entry: FeedEntryOPDS) {
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
      this.activity, this.bookDownloadButtons, this.books, entry)
    CatalogBookDetailView.configureButtonsHeight(
      this.activity.resources, this.bookDownloadButtons)
  }

  override fun onBookStatusRequestingDownload(status: BookStatusRequestingDownload): Unit {
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

  override fun update(
    observable: Observable,
    @Nullable data: Any) {

    LOG.debug("update: {} {}", observable, data)

    val updateID = data as BookID
    val currentEntry = this.entry.get()
    val currentID = currentEntry.bookID

    /*
     * If the book for this view has been updated in some way, first check
     * the book database and replace the current feed entry with any snapshot
     * that exists. Then, check to see if a revocation feed entry has been
     * posted and use that.
     */

    if (currentID == updateID) {
      val statusCache = this.books.bookGetStatusCache()
      val statusOpt = statusCache.booksStatusGet(currentID)

      LOG.debug("received status update {}", statusOpt)
      this.updateFetchDatabaseSnapshot(currentID, this.books.bookGetDatabase())
      this.updateCheckForRevocationEntry(currentID, statusCache)
      this.onStatus(this.entry.get(), statusOpt)
    }
  }

  private fun updateFetchDatabaseSnapshot(
    currentID: BookID,
    database: BookDatabaseReadableType) {
    val snapOpt = database.databaseGetEntrySnapshot(currentID)

    LOG.debug("database snapshot is {}", snapOpt)

    if (snapOpt is Some<BookDatabaseEntrySnapshot>) {
      val (_, _, entry1) = snapOpt.get()
      this.entry.set(FeedEntryOPDS.fromOPDSAcquisitionFeedEntry(entry1) as FeedEntryOPDS)
    }
  }

  private fun updateCheckForRevocationEntry(
    currentID: BookID,
    statusCache: BooksStatusCacheType) {
    val revokeOpt = statusCache.booksRevocationFeedEntryGet(currentID)

    LOG.debug("revocation entry update is {}", revokeOpt)

    if (revokeOpt is Some<FeedEntryType>) {
      revokeOpt.get().matchFeedEntry(
        object : FeedEntryMatcherType<Unit, UnreachableCodeException> {
          override fun onFeedEntryOPDS(e: FeedEntryOPDS): Unit {
            this@CatalogBookDetailView.entry.set(e)
            return Unit.unit()
          }

          override fun onFeedEntryCorrupt(e: FeedEntryCorrupt): Unit {
            LOG.debug("cannot render an entry of type {}", e)
            return Unit.unit()
          }
        })
    }
  }

  companion object {
    private val GENRES_URI: URI = URI.create("http://librarysimplified.org/terms/genres/Simplified/")
    private val GENRES_URI_TEXT: String = this.GENRES_URI.toString()
    private val LOG: Logger

    init {
      this.LOG = LogUtilities.getLog(CatalogBookDetailView::class.java)
    }

    private fun configureButtonsHeight(
      resources: Resources,
      layout: LinearLayout) {

      val app = Simplified.getCatalogAppServices()
      val dp35 = resources.getDimension(R.dimen.button_standard_height).toInt()
      val dp8 = app.screenDPToPixels(8).toInt()
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

      val summary_text_settings = summaryText.settings
      summary_text_settings.allowContentAccess = false
      summary_text_settings.allowFileAccess = false
      summary_text_settings.allowFileAccessFromFileURLs = false
      summary_text_settings.allowUniversalAccessFromFileURLs = false
      summary_text_settings.blockNetworkLoads = true
      summary_text_settings.blockNetworkImage = true
      summary_text_settings.defaultTextEncodingName = "UTF-8"
      summary_text_settings.defaultFixedFontSize = 14
      summary_text_settings.defaultFontSize = 14
      summaryText.loadDataWithBaseURL(null, text.toString(), "text/html", "UTF-8", null)
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
  }
}
