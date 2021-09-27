package org.nypl.simplified.ui.catalog

import android.os.Bundle
import android.text.Html
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TableLayout
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.common.base.Preconditions
import com.google.common.util.concurrent.FluentFuture
import com.io7m.jfunctional.Some
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormatterBuilder
import org.librarysimplified.services.api.Services
import org.nypl.simplified.android.ktx.supportActionBar
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookFormat
import org.nypl.simplified.books.book_database.api.BookFormats
import org.nypl.simplified.books.book_registry.BookStatus
import org.nypl.simplified.books.book_registry.BookWithStatus
import org.nypl.simplified.books.covers.BookCoverProviderType
import org.nypl.simplified.buildconfig.api.BuildConfigurationServiceType
import org.nypl.simplified.feeds.api.FeedEntry.FeedEntryOPDS
import org.nypl.simplified.listeners.api.FragmentListenerType
import org.nypl.simplified.listeners.api.fragmentListeners
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.ui.screen.ScreenSizeInformationType
import java.net.URI

/**
 * A book detail page.
 */

class CatalogBookDetailFragment : Fragment(R.layout.book_detail) {

  companion object {

    private const val PARAMETERS_ID =
      "org.nypl.simplified.ui.catalog.CatalogFragmentBookDetail.parameters"

    /**
     * Create a book detail fragment for the given parameters.
     */

    fun create(parameters: CatalogBookDetailFragmentParameters): CatalogBookDetailFragment {
      val fragment = CatalogBookDetailFragment()
      fragment.arguments = bundleOf(this.PARAMETERS_ID to parameters)
      return fragment
    }
  }

  private val services =
    Services.serviceDirectory()

  private val parameters: CatalogBookDetailFragmentParameters by lazy {
    this.requireArguments()[PARAMETERS_ID] as CatalogBookDetailFragmentParameters
  }

  private val listener: FragmentListenerType<CatalogBookDetailEvent> by fragmentListeners()

  private val borrowViewModel: CatalogBorrowViewModel by viewModels(
    factoryProducer = {
      CatalogBorrowViewModelFactory(services)
    }
  )

  private val viewModel: CatalogBookDetailViewModel by viewModels(
    factoryProducer = {
      CatalogBookDetailViewModelFactory(
        requireActivity().application,
        this.services,
        this.borrowViewModel,
        this.listener,
        this.parameters
      )
    }
  )

  private var thumbnailLoading: FluentFuture<Unit>? = null

  private lateinit var authors: TextView
  private lateinit var buildConfig: BuildConfigurationServiceType
  private lateinit var buttonCreator: CatalogButtons
  private lateinit var buttons: LinearLayout
  private lateinit var cover: ImageView
  private lateinit var covers: BookCoverProviderType
  private lateinit var debugStatus: TextView
  private lateinit var format: TextView
  private lateinit var metadata: TableLayout
  private lateinit var related: TextView
  private lateinit var report: TextView
  private lateinit var screenSize: ScreenSizeInformationType
  private lateinit var status: ViewGroup
  private lateinit var statusFailed: ViewGroup
  private lateinit var statusFailedText: TextView
  private lateinit var statusIdle: ViewGroup
  private lateinit var statusIdleText: TextView
  private lateinit var statusInProgress: ViewGroup
  private lateinit var statusInProgressBar: ProgressBar
  private lateinit var statusInProgressText: TextView
  private lateinit var summary: TextView
  private lateinit var title: TextView

  private val dateFormatter =
    DateTimeFormatterBuilder()
      .appendYear(4, 5)
      .appendLiteral('-')
      .appendMonthOfYear(2)
      .appendLiteral('-')
      .appendDayOfMonth(2)
      .toFormatter()

  private val dateTimeFormatter =
    DateTimeFormatterBuilder()
      .appendYear(4, 5)
      .appendLiteral('-')
      .appendMonthOfYear(2)
      .appendLiteral('-')
      .appendDayOfMonth(2)
      .appendLiteral(' ')
      .appendHourOfDay(2)
      .appendLiteral(':')
      .appendMinuteOfHour(2)
      .appendLiteral(':')
      .appendSecondOfMinute(2)
      .toFormatter()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    this.screenSize =
      services.requireService(ScreenSizeInformationType::class.java)
    this.covers =
      services.requireService(BookCoverProviderType::class.java)
    this.buildConfig =
      services.requireService(BuildConfigurationServiceType::class.java)

    this.buttonCreator =
      CatalogButtons(this.requireContext(), this.screenSize)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    this.viewModel.bookWithStatusLive.observe(this.viewLifecycleOwner, this::reconfigureUI)

    this.cover =
      view.findViewById(R.id.bookDetailCoverImage)
    this.title =
      view.findViewById(R.id.bookDetailTitle)
    this.format =
      view.findViewById(R.id.bookDetailFormat)
    this.authors =
      view.findViewById(R.id.bookDetailAuthors)
    this.status =
      view.findViewById(R.id.bookDetailStatus)
    this.summary =
      view.findViewById(R.id.bookDetailDescriptionText)
    this.metadata =
      view.findViewById(R.id.bookDetailMetadataTable)
    this.buttons =
      view.findViewById(R.id.bookDetailButtons)
    this.related =
      view.findViewById(R.id.bookDetailRelated)
    this.report =
      view.findViewById(R.id.bookDetailReport)

    this.debugStatus =
      view.findViewById(R.id.bookDetailDebugStatus)

    this.statusIdle =
      this.status.findViewById(R.id.bookDetailStatusIdle)
    this.statusIdleText =
      this.statusIdle.findViewById(R.id.idleText)

    this.statusInProgress =
      this.status.findViewById(R.id.bookDetailStatusInProgress)
    this.statusInProgressBar =
      this.statusInProgress.findViewById(R.id.inProgressBar)
    this.statusInProgressText =
      this.statusInProgress.findViewById(R.id.inProgressText)

    this.statusInProgressText.text = "100%"

    this.statusFailed =
      this.status.findViewById(R.id.bookDetailStatusFailed)
    this.statusFailedText =
      this.status.findViewById(R.id.failedText)

    this.statusIdle.visibility = View.VISIBLE
    this.statusInProgress.visibility = View.INVISIBLE
    this.statusFailed.visibility = View.INVISIBLE

    this.debugStatus.visibility =
      if (this.viewModel.showDebugBookDetailStatus) {
        View.VISIBLE
      } else {
        View.INVISIBLE
      }

    val targetHeight =
      this.resources.getDimensionPixelSize(R.dimen.cover_detail_height)
    this.covers.loadCoverInto(
      this.parameters.feedEntry, this.cover, 0, targetHeight
    )

    this.configureOPDSEntry(this.parameters.feedEntry)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    this.thumbnailLoading?.cancel(true)
    this.thumbnailLoading = null
  }

  override fun onResume() {
    super.onResume()
    this.configureToolbar()
  }

  private fun configureToolbar() {
    val feedTitle = this.parameters.feedArguments.title
    this.supportActionBar?.apply {
      title = feedTitle
      subtitle = this@CatalogBookDetailFragment.viewModel.accountProvider?.displayName
    }
  }

  private fun configureOPDSEntry(feedEntry: FeedEntryOPDS) {
    val context = this.requireContext()

    val opds = feedEntry.feedEntry
    this.title.text = opds.title
    this.authors.text = opds.authorsCommaSeparated

    this.format.text = when (feedEntry.probableFormat) {
      BookFormats.BookFormatDefinition.BOOK_FORMAT_EPUB ->
        context.getString(R.string.catalogBookFormatEPUB)
      BookFormats.BookFormatDefinition.BOOK_FORMAT_AUDIO ->
        context.getString(R.string.catalogBookFormatAudioBook)
      BookFormats.BookFormatDefinition.BOOK_FORMAT_PDF ->
        context.getString(R.string.catalogBookFormatPDF)
      null -> ""
    }

    this.cover.contentDescription =
      CatalogBookAccessibilityStrings.coverDescription(this.resources, feedEntry)

    /*
     * Render the HTML present in the summary and insert it into the text view.
     */

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
      this.summary.text = Html.fromHtml(opds.summary, Html.FROM_HTML_MODE_LEGACY)
    } else {
      @Suppress("DEPRECATION")
      this.summary.text = Html.fromHtml(opds.summary)
    }

    this.configureMetadataTable(opds)

    /*
     * If there's a related feed, enable the "Related books..." item and open the feed
     * on demand.
     */

    val feedRelatedOpt = feedEntry.feedEntry.related
    if (feedRelatedOpt is Some<URI>) {
      val feedRelated = feedRelatedOpt.get()
      this.related.setOnClickListener {
        this.viewModel.openRelatedFeed(feedRelated)
      }
      this.related.isEnabled = true
    } else {
      this.related.isEnabled = false
    }
  }

  private val genreUriScheme =
    "http://librarysimplified.org/terms/genres/Simplified/"

  private fun configureMetadataTable(entry: OPDSAcquisitionFeedEntry) {
    this.metadata.removeAllViews()

    val publishedOpt = entry.published
    if (publishedOpt is Some<DateTime>) {
      val (row, rowKey, rowVal) = this.tableRowOf()
      rowKey.text = this.getString(R.string.catalogMetaPublicationDate)
      rowVal.text = this.dateFormatter.print(publishedOpt.get())
      this.metadata.addView(row)
    }

    val publisherOpt = entry.publisher
    if (publisherOpt is Some<String>) {
      val (row, rowKey, rowVal) = this.tableRowOf()
      rowKey.text = this.getString(R.string.catalogMetaPublisher)
      rowVal.text = publisherOpt.get()
      this.metadata.addView(row)
    }

    if (entry.distribution.isNotBlank()) {
      val (row, rowKey, rowVal) = this.tableRowOf()
      rowKey.text = this.getString(R.string.catalogMetaDistributor)
      rowVal.text = entry.distribution
      this.metadata.addView(row)
    }

    val categories =
      entry.categories.filter { opdsCategory -> opdsCategory.scheme == this.genreUriScheme }
    if (categories.isNotEmpty()) {
      val (row, rowKey, rowVal) = this.tableRowOf()
      rowKey.text = this.getString(R.string.catalogMetaCategories)
      rowVal.text = categories.joinToString(", ") { opdsCategory -> opdsCategory.effectiveLabel }
      this.metadata.addView(row)
    }

    this.run {
      val (row, rowKey, rowVal) = this.tableRowOf()
      rowKey.text = this.getString(R.string.catalogMetaUpdatedDate)
      rowVal.text = this.dateTimeFormatter.print(entry.updated)
      this.metadata.addView(row)
    }
  }

  private fun tableRowOf(): Triple<View, TextView, TextView> {
    val row = this.layoutInflater.inflate(R.layout.book_detail_metadata_item, this.metadata, false)
    val rowKey = row.findViewById<TextView>(R.id.key)
    val rowVal = row.findViewById<TextView>(R.id.value)
    return Triple(row, rowKey, rowVal)
  }

  private fun reconfigureUI(book: BookWithStatus) {
    this.debugStatus.text = book.javaClass.simpleName

    return when (val status = book.status) {
      is BookStatus.Held ->
        this.onBookStatusHeld(status, book.book)
      is BookStatus.Loaned ->
        this.onBookStatusLoaned(status, book.book)
      is BookStatus.Holdable ->
        this.onBookStatusHoldable(status)
      is BookStatus.Loanable ->
        this.onBookStatusLoanable(status)
      is BookStatus.RequestingLoan ->
        this.onBookStatusRequestingLoan()
      is BookStatus.Revoked ->
        this.onBookStatusRevoked(status)
      is BookStatus.FailedLoan ->
        this.onBookStatusFailedLoan(status)
      is BookStatus.FailedRevoke ->
        this.onBookStatusFailedRevoke(status)
      is BookStatus.FailedDownload ->
        this.onBookStatusFailedDownload(status)
      is BookStatus.RequestingRevoke ->
        this.onBookStatusRequestingRevoke()
      is BookStatus.RequestingDownload ->
        this.onBookStatusRequestingDownload()
      is BookStatus.Downloading ->
        this.onBookStatusDownloading(status)
      is BookStatus.DownloadWaitingForExternalAuthentication ->
        this.onBookStatusDownloadWaitingForExternalAuthentication()
      is BookStatus.DownloadExternalAuthenticationInProgress ->
        this.onBookStatusDownloadExternalAuthenticationInProgress()
    }
  }

  private fun onBookStatusFailedLoan(
    bookStatus: BookStatus.FailedLoan,
  ) {
    this.buttons.removeAllViews()
    this.buttons.addView(
      this.buttonCreator.createDismissButton {
        this.viewModel.dismissBorrowError()
      }
    )
    this.buttons.addView(this.buttonCreator.createButtonSpace())
    this.buttons.addView(
      this.buttonCreator.createDetailsButton {
        this.viewModel.showError(bookStatus.result)
      }
    )
    this.buttons.addView(this.buttonCreator.createButtonSpace())
    this.buttons.addView(
      this.buttonCreator.createRetryButton {
        this.viewModel.borrowMaybeAuthenticated()
      }
    )
    this.checkButtonViewCount()

    this.statusInProgress.visibility = View.INVISIBLE
    this.statusIdle.visibility = View.INVISIBLE
    this.statusFailed.visibility = View.VISIBLE
    this.statusFailedText.text = this.resources.getText(R.string.catalogOperationFailed)
  }

  private fun onBookStatusRevoked(
    bookStatus: BookStatus.Revoked
  ) {
    this.buttons.removeAllViews()
    this.buttons.addView(this.buttonCreator.createCenteredTextForButtons(R.string.catalogRequesting))
    this.checkButtonViewCount()

    this.statusInProgress.visibility = View.INVISIBLE
    this.statusIdle.visibility = View.VISIBLE
    this.statusFailed.visibility = View.INVISIBLE

    this.statusIdleText.text =
      CatalogBookAvailabilityStrings.statusString(this.resources, bookStatus)
  }

  private fun onBookStatusRequestingLoan() {
    this.buttons.removeAllViews()
    this.buttons.addView(this.buttonCreator.createCenteredTextForButtons(R.string.catalogRequesting))
    this.checkButtonViewCount()

    this.statusInProgress.visibility = View.VISIBLE
    this.statusIdle.visibility = View.INVISIBLE
    this.statusFailed.visibility = View.INVISIBLE

    this.statusInProgressText.visibility = View.GONE
    this.statusInProgressBar.isIndeterminate = true
  }

  private fun onBookStatusLoanable(
    bookStatus: BookStatus.Loanable,
  ) {
    this.buttons.removeAllViews()
    this.buttons.addView(this.buttonCreator.createButtonSizedSpace())
    this.buttons.addView(
      this.buttonCreator.createGetButton {
        this.viewModel.borrowMaybeAuthenticated()
      }
    )
    this.buttons.addView(this.buttonCreator.createButtonSizedSpace())
    this.checkButtonViewCount()

    this.statusInProgress.visibility = View.INVISIBLE
    this.statusIdle.visibility = View.VISIBLE
    this.statusFailed.visibility = View.INVISIBLE

    this.statusIdleText.text =
      CatalogBookAvailabilityStrings.statusString(this.resources, bookStatus)
  }

  private fun onBookStatusHoldable(
    bookStatus: BookStatus.Holdable,
  ) {
    this.buttons.removeAllViews()
    this.buttons.addView(this.buttonCreator.createButtonSizedSpace())
    this.buttons.addView(
      this.buttonCreator.createReserveButton {
        this.viewModel.reserveMaybeAuthenticated()
      }
    )
    this.buttons.addView(this.buttonCreator.createButtonSizedSpace())
    this.checkButtonViewCount()

    this.statusInProgress.visibility = View.INVISIBLE
    this.statusIdle.visibility = View.VISIBLE
    this.statusFailed.visibility = View.INVISIBLE

    this.statusIdleText.text =
      CatalogBookAvailabilityStrings.statusString(this.resources, bookStatus)
  }

  private fun onBookStatusHeld(
    bookStatus: BookStatus.Held,
    book: Book
  ) {
    this.buttons.removeAllViews()
    when (bookStatus) {
      is BookStatus.Held.HeldInQueue ->
        if (bookStatus.isRevocable) {
          this.buttons.addView(this.buttonCreator.createButtonSizedSpace())
          this.buttons.addView(
            this.buttonCreator.createRevokeHoldButton {
              this.viewModel.revokeMaybeAuthenticated()
            }
          )
          this.buttons.addView(this.buttonCreator.createButtonSizedSpace())
        } else {
          this.buttons.addView(
            this.buttonCreator.createCenteredTextForButtons(R.string.catalogHoldCannotCancel)
          )
        }

      is BookStatus.Held.HeldReady -> {
        if (bookStatus.isRevocable) {
          this.buttons.addView(
            this.buttonCreator.createRevokeHoldButton {
              this.viewModel.revokeMaybeAuthenticated()
            }
          )
        }
        this.buttons.addView(
          this.buttonCreator.createGetButton {
            this.viewModel.borrowMaybeAuthenticated()
          }
        )
      }
    }
    this.checkButtonViewCount()

    this.statusInProgress.visibility = View.INVISIBLE
    this.statusIdle.visibility = View.VISIBLE
    this.statusFailed.visibility = View.INVISIBLE
    this.statusIdleText.text =
      CatalogBookAvailabilityStrings.statusString(this.resources, bookStatus)
  }

  private fun onBookStatusLoaned(
    bookStatus: BookStatus.Loaned,
    book: Book
  ) {
    this.buttons.removeAllViews()
    when (bookStatus) {
      is BookStatus.Loaned.LoanedNotDownloaded ->
        this.buttons.addView(
          this.buttonCreator.createDownloadButton {
            this.viewModel.borrowMaybeAuthenticated()
          }
        )

      is BookStatus.Loaned.LoanedDownloaded ->
        when (val format = book.findPreferredFormat()) {
          is BookFormat.BookFormatPDF,
          is BookFormat.BookFormatEPUB -> {
            this.buttons.addView(
              this.buttonCreator.createReadButton {
                this.viewModel.openViewer(format)
              }
            )
          }
          is BookFormat.BookFormatAudioBook -> {
            this.buttons.addView(
              this.buttonCreator.createListenButton {
                this.viewModel.openViewer(format)
              }
            )
          }
        }
    }

    if (bookStatus.returnable && this.buildConfig.allowReturns()) {
      this.buttons.addView(this.buttonCreator.createButtonSpace())
      this.buttons.addView(
        this.buttonCreator.createRevokeLoanButton {
          this.viewModel.revokeMaybeAuthenticated()
        }
      )
    }

    if (this.viewModel.bookCanBeDeleted) {
      this.buttons.addView(this.buttonCreator.createButtonSpace())
      this.buttons.addView(
        this.buttonCreator.createDeleteButton {
          this.viewModel.delete()
        }
      )
    }

    this.checkButtonViewCount()

    this.statusInProgress.visibility = View.INVISIBLE
    this.statusIdle.visibility = View.VISIBLE
    this.statusFailed.visibility = View.INVISIBLE
    this.statusIdleText.text =
      CatalogBookAvailabilityStrings.statusString(this.resources, bookStatus)
  }

  private fun onBookStatusDownloading(
    bookStatus: BookStatus.Downloading,
  ) {
    /*
     * XXX: https://jira.nypl.org/browse/SIMPLY-3444
     *
     * Avoid creating a cancel button until we can reliably support cancellation for *all* books.
     * That is, when the Adobe DRM is dead and buried.
     */

    this.buttons.removeAllViews()

    this.statusInProgress.visibility = View.VISIBLE
    this.statusIdle.visibility = View.INVISIBLE
    this.statusFailed.visibility = View.INVISIBLE

    val progressPercent = bookStatus.progressPercent?.toInt()
    if (progressPercent != null) {
      this.statusInProgressText.visibility = View.VISIBLE
      this.statusInProgressText.text = "$progressPercent%"
      this.statusInProgressBar.isIndeterminate = false
      this.statusInProgressBar.progress = progressPercent
    } else {
      this.statusInProgressText.visibility = View.GONE
      this.statusInProgressBar.isIndeterminate = true
      this.buttons.addView(this.buttonCreator.createCenteredTextForButtons(R.string.catalogDownloading))
      this.checkButtonViewCount()
    }
  }

  private fun onBookStatusDownloadWaitingForExternalAuthentication() {
    this.buttons.removeAllViews()
    this.buttons.addView(this.buttonCreator.createCenteredTextForButtons(R.string.catalogLoginRequired))
    this.checkButtonViewCount()

    this.statusInProgress.visibility = View.VISIBLE
    this.statusIdle.visibility = View.INVISIBLE
    this.statusFailed.visibility = View.INVISIBLE
    this.statusInProgressText.visibility = View.GONE
    this.statusInProgressBar.isIndeterminate = true
  }

  private fun onBookStatusDownloadExternalAuthenticationInProgress() {
    this.buttons.removeAllViews()
    this.buttons.addView(this.buttonCreator.createCenteredTextForButtons(R.string.catalogLoginRequired))
    this.checkButtonViewCount()

    this.statusInProgress.visibility = View.VISIBLE
    this.statusIdle.visibility = View.INVISIBLE
    this.statusFailed.visibility = View.INVISIBLE
    this.statusInProgressText.visibility = View.GONE
    this.statusInProgressBar.isIndeterminate = true
  }

  private fun onBookStatusRequestingDownload() {
    this.buttons.removeAllViews()
    this.buttons.addView(this.buttonCreator.createCenteredTextForButtons(R.string.catalogRequesting))
    this.checkButtonViewCount()

    this.statusInProgress.visibility = View.VISIBLE
    this.statusIdle.visibility = View.INVISIBLE
    this.statusFailed.visibility = View.INVISIBLE
    this.statusInProgressText.visibility = View.GONE
    this.statusInProgressBar.isIndeterminate = true
  }

  private fun onBookStatusRequestingRevoke() {
    this.buttons.removeAllViews()
    this.buttons.addView(this.buttonCreator.createCenteredTextForButtons(R.string.catalogRequesting))
    this.checkButtonViewCount()

    this.statusInProgress.visibility = View.VISIBLE
    this.statusIdle.visibility = View.INVISIBLE
    this.statusFailed.visibility = View.INVISIBLE
    this.statusInProgressText.visibility = View.GONE
    this.statusInProgressBar.isIndeterminate = true
  }

  private fun onBookStatusFailedDownload(
    bookStatus: BookStatus.FailedDownload,
  ) {
    this.buttons.removeAllViews()
    this.buttons.addView(
      this.buttonCreator.createDismissButton {
        this.viewModel.dismissBorrowError()
      }
    )
    this.buttons.addView(this.buttonCreator.createButtonSpace())
    this.buttons.addView(
      this.buttonCreator.createDetailsButton {
        this.viewModel.showError(bookStatus.result)
      }
    )
    this.buttons.addView(this.buttonCreator.createButtonSpace())
    this.buttons.addView(
      this.buttonCreator.createRetryButton {
        this.viewModel.borrowMaybeAuthenticated()
      }
    )
    this.checkButtonViewCount()

    this.statusInProgress.visibility = View.INVISIBLE
    this.statusIdle.visibility = View.INVISIBLE
    this.statusFailed.visibility = View.VISIBLE
    this.statusFailedText.text = this.getString(R.string.catalogOperationFailed)
  }

  private fun onBookStatusFailedRevoke(
    bookStatus: BookStatus.FailedRevoke,
  ) {
    this.buttons.removeAllViews()
    this.buttons.addView(
      this.buttonCreator.createDismissButton {
        this.viewModel.dismissRevokeError()
      }
    )
    this.buttons.addView(this.buttonCreator.createButtonSpace())
    this.buttons.addView(
      this.buttonCreator.createDetailsButton {
        this.viewModel.showError(bookStatus.result)
      }
    )
    this.buttons.addView(this.buttonCreator.createButtonSpace())
    this.buttons.addView(
      this.buttonCreator.createRetryButton {
        this.viewModel.revokeMaybeAuthenticated()
      }
    )
    this.checkButtonViewCount()

    this.statusInProgress.visibility = View.INVISIBLE
    this.statusIdle.visibility = View.INVISIBLE
    this.statusFailed.visibility = View.VISIBLE
    this.statusFailedText.text = this.resources.getText(R.string.catalogOperationFailed)
  }

  private fun checkButtonViewCount() {
    Preconditions.checkState(
      this.buttons.childCount > 0,
      "At least one button must be present (existing ${this.buttons.childCount})"
    )
  }
}
