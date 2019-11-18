package org.nypl.simplified.ui.catalog

import android.content.Context
import android.os.Bundle
import android.text.Html
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Space
import android.widget.TableLayout
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.annotation.UiThread
import androidx.fragment.app.Fragment
import com.google.common.base.Preconditions
import com.io7m.jfunctional.Some
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormatterBuilder
import org.librarysimplified.services.api.ServiceDirectoryProviderType
import org.nypl.simplified.accounts.api.AccountLoginState
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.book_database.api.BookFormats
import org.nypl.simplified.books.book_registry.BookRegistryReadableType
import org.nypl.simplified.books.book_registry.BookStatus
import org.nypl.simplified.books.book_registry.BookStatusEvent
import org.nypl.simplified.books.book_registry.BookWithStatus
import org.nypl.simplified.books.controller.api.BooksControllerType
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.observable.ObservableSubscriptionType
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.ui.screen.ScreenSizeInformationType
import org.nypl.simplified.ui.thread.api.UIThreadServiceType
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicReference

/**
 * A book detail page.
 */

class CatalogFragmentBookDetail : Fragment(), CatalogFragmentLoginDialogListenerType {

  private val logger = LoggerFactory.getLogger(CatalogFragmentBookDetail::class.java)

  companion object {

    private const val PARAMETERS_ID =
      "org.nypl.simplified.ui.catalog.CatalogFragmentBookDetail.parameters"

    /**
     * Create a book detail fragment for the given parameters.
     */

    fun create(parameters: CatalogFragmentBookDetailParameters): CatalogFragmentBookDetail {
      val arguments = Bundle()
      arguments.putSerializable(this.PARAMETERS_ID, parameters)
      val fragment = CatalogFragmentBookDetail()
      fragment.arguments = arguments
      return fragment
    }
  }

  private lateinit var authors: TextView
  private lateinit var bookRegistry: BookRegistryReadableType
  private lateinit var booksController: BooksControllerType
  private lateinit var buttons: LinearLayout
  private lateinit var cover: ImageView
  private lateinit var debugStatus: TextView
  private lateinit var format: TextView
  private lateinit var host: ServiceDirectoryProviderType
  private lateinit var metadata: TableLayout
  private lateinit var parameters: CatalogFragmentBookDetailParameters
  private lateinit var profilesController: ProfilesControllerType
  private lateinit var screenSize: ScreenSizeInformationType
  private lateinit var status: ViewGroup
  private lateinit var statusFailed: ViewGroup
  private lateinit var statusIdle: ViewGroup
  private lateinit var statusIdleText: TextView
  private lateinit var statusInProgress: ViewGroup
  private lateinit var statusInProgressBar: ProgressBar
  private lateinit var statusInProgressText: TextView
  private lateinit var summary: TextView
  private lateinit var title: TextView
  private lateinit var uiThread: UIThreadServiceType
  private val parametersId = PARAMETERS_ID
  private var bookRegistrySubscription: ObservableSubscriptionType<BookStatusEvent>? = null
  private val runOnLoginDialogClosed: AtomicReference<() -> Unit> = AtomicReference()

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

    val context = this.requireContext()
    if (context is ServiceDirectoryProviderType) {
      this.host = context
    } else {
      throw IllegalStateException(
        "The context hosting this fragment must implement ${ServiceDirectoryProviderType::class.java}")
    }

    this.parameters =
      this.arguments!![this.parametersId] as CatalogFragmentBookDetailParameters

    this.bookRegistry =
      this.host.serviceDirectory.requireService(BookRegistryReadableType::class.java)
    this.uiThread =
      this.host.serviceDirectory.requireService(UIThreadServiceType::class.java)
    this.screenSize =
      this.host.serviceDirectory.requireService(ScreenSizeInformationType::class.java)
    this.profilesController =
      this.host.serviceDirectory.requireService(ProfilesControllerType::class.java)
    this.booksController =
      this.host.serviceDirectory.requireService(BooksControllerType::class.java)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    val layout =
      inflater.inflate(R.layout.book_detail, container, false)

    this.cover =
      layout.findViewById(R.id.bookDetailCover)
    this.title =
      layout.findViewById(R.id.bookDetailTitle)
    this.format =
      layout.findViewById(R.id.bookDetailFormat)
    this.authors =
      layout.findViewById(R.id.bookDetailAuthors)
    this.status =
      layout.findViewById(R.id.bookDetailStatus)
    this.summary =
      layout.findViewById(R.id.bookDetailDescriptionText)
    this.metadata =
      layout.findViewById(R.id.bookDetailMetadataTable)
    this.buttons =
      layout.findViewById(R.id.bookDetailButtons)

    this.debugStatus =
      layout.findViewById(R.id.bookDetailDebugStatus)

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

    this.statusIdle.visibility = View.VISIBLE
    this.statusInProgress.visibility = View.INVISIBLE
    this.statusFailed.visibility = View.INVISIBLE

    this.debugStatus.visibility =
      if (this.parameters.debugShowStatus) {
        View.VISIBLE
      } else {
        View.INVISIBLE
      }

    return layout
  }

  override fun onStart() {
    super.onStart()

    /*
     * Retrieve the current status of the book, or synthesize a status value based on the
     * OPDS feed entry if the book is not in the registry. The book will only be in the
     * registry if the user has ever tried to borrow it (as per the registry spec).
     */

    val status =
      this.bookRegistry.bookStatusOrNull(this.parameters.bookID)
        ?: BookStatus.fromBook(
          Book(
            id = this.parameters.bookID,
            account = this.parameters.accountId,
            cover = null,
            thumbnail = null,
            entry = this.parameters.feedEntry.feedEntry,
            formats = listOf()
          )
        )

    this.onBookStatusUI(status)
    this.onOPDSFeedEntryUI(this.parameters.feedEntry)

    this.bookRegistrySubscription =
      this.bookRegistry.bookEvents()
        .subscribe(this::onBookStatusEvent)
  }

  private fun onBookStatusEvent(event: BookStatusEvent) {
    if (event.book() != this.parameters.bookID) {
      return
    }

    val bookStatusOpt = this.bookRegistry.book(this.parameters.bookID)
    if (bookStatusOpt is Some<BookWithStatus>) {
      val bookWithStatus = bookStatusOpt.get()
      this.uiThread.runOnUIThread {
        this.onBookStatusUI(bookWithStatus.status)
      }

      this.onOPDSFeedEntry(FeedEntry.FeedEntryOPDS(bookWithStatus.book.entry))
    }
  }

  private fun onOPDSFeedEntry(entry: FeedEntry.FeedEntryOPDS) {
    this.uiThread.runOnUIThread {
      this.parameters = this.parameters.copy(feedEntry = entry)
      this.onOPDSFeedEntryUI(entry)
    }
  }

  @UiThread
  private fun onOPDSFeedEntryUI(feedEntry: FeedEntry.FeedEntryOPDS) {
    this.uiThread.checkIsUIThread()

    val opds = feedEntry.feedEntry
    this.title.text = opds.title
    this.authors.text = opds.authorsCommaSeparated

    val context = this.requireContext()
    this.format.text = when (feedEntry.probableFormat) {
      null,
      BookFormats.BookFormatDefinition.BOOK_FORMAT_EPUB ->
        context.getString(R.string.catalogBookFormatEPUB)
      BookFormats.BookFormatDefinition.BOOK_FORMAT_AUDIO ->
        context.getString(R.string.catalogBookFormatAudioBook)
      BookFormats.BookFormatDefinition.BOOK_FORMAT_PDF ->
        context.getString(R.string.catalogBookFormatPDF)
    }

    this.cover.contentDescription =
      CatalogBookAccessibilityStrings.coverDescription(this.resources, feedEntry)

    /*
     * Render the HTML present in the summary and insert it into the text view.
     */

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
      this.summary.text = Html.fromHtml(opds.summary, Html.FROM_HTML_MODE_LEGACY)
    } else {
      this.summary.text = Html.fromHtml(opds.summary)
    }

    this.configureMetadataTable(opds)
  }

  private val genreUriScheme =
    "http://librarysimplified.org/terms/genres/Simplified/"

  @UiThread
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

  @UiThread
  private fun onBookStatusUI(bookStatus: BookStatus) {
    this.uiThread.checkIsUIThread()
    this.debugStatus.text = bookStatus.javaClass.simpleName

    return when (bookStatus) {
      is BookStatus.Held ->
        this.onBookStatusHeldUI(bookStatus)
      is BookStatus.Loaned ->
        this.onBookStatusLoanedUI(bookStatus)
      is BookStatus.Holdable ->
        this.onBookStatusHoldableUI(bookStatus)
      is BookStatus.Loanable ->
        this.onBookStatusLoanableUI(bookStatus)
      is BookStatus.RequestingLoan ->
        this.onBookStatusRequestingLoanUI(bookStatus)
      is BookStatus.Revoked ->
        this.onBookStatusRevokedUI(bookStatus)
      is BookStatus.FailedLoan ->
        this.onBookStatusFailedLoanUI(bookStatus)
      is BookStatus.FailedRevoke ->
        this.onBookStatusFailedRevokeUI(bookStatus)
      is BookStatus.FailedDownload ->
        this.onBookStatusFailedDownloadUI(bookStatus)
      is BookStatus.RequestingRevoke ->
        this.onBookStatusRequestingRevokeUI(bookStatus)
      is BookStatus.RequestingDownload ->
        this.onBookStatusRequestingDownloadUI(bookStatus)
      is BookStatus.Downloading ->
        this.onBookStatusDownloadingUI(bookStatus)
    }
  }

  @UiThread
  private fun onBookStatusFailedLoanUI(bookStatus: BookStatus.FailedLoan) {
    this.uiThread.checkIsUIThread()

    val context = this.requireContext()
    this.buttons.removeAllViews()
    this.buttons.addView(this.createDismissButton(context) {
      this.tryDismissError()
    })
    this.buttons.addView(this.createButtonSpace(context))
    this.buttons.addView(this.createDetailsButton(context) {
      this.tryShowError()
    })
    this.buttons.addView(this.createButtonSpace(context))
    this.buttons.addView(this.createRetryButton(context, this::tryBorrowMaybeAuthenticated))
    this.checkButtonViewCount()

    this.statusInProgress.visibility = View.INVISIBLE
    this.statusIdle.visibility = View.INVISIBLE
    this.statusFailed.visibility = View.VISIBLE
  }

  @UiThread
  private fun onBookStatusRevokedUI(bookStatus: BookStatus.Revoked) {
    this.uiThread.checkIsUIThread()

    val context = this.requireContext()
    this.buttons.removeAllViews()
    this.buttons.addView(this.createCenteredTextForButtons(context, R.string.catalogRequesting))
    this.checkButtonViewCount()

    this.statusInProgress.visibility = View.INVISIBLE
    this.statusIdle.visibility = View.VISIBLE
    this.statusFailed.visibility = View.INVISIBLE

    this.statusIdleText.text =
      CatalogBookAvailabilityStrings.statusString(this.resources, bookStatus)
  }

  @UiThread
  private fun onBookStatusRequestingLoanUI(bookStatus: BookStatus.RequestingLoan) {
    this.uiThread.checkIsUIThread()

    val context = this.requireContext()
    this.buttons.removeAllViews()
    this.buttons.addView(this.createCenteredTextForButtons(context, R.string.catalogRequesting))
    this.checkButtonViewCount()

    this.statusInProgress.visibility = View.VISIBLE
    this.statusIdle.visibility = View.INVISIBLE
    this.statusFailed.visibility = View.INVISIBLE

    this.statusInProgressText.visibility = View.GONE
    this.statusInProgressBar.isIndeterminate = true
  }

  @UiThread
  private fun onBookStatusLoanableUI(bookStatus: BookStatus.Loanable) {
    this.uiThread.checkIsUIThread()

    val context = this.requireContext()
    this.buttons.removeAllViews()
    this.buttons.addView(this.createGetButton(context, this::tryBorrowMaybeAuthenticated))
    this.checkButtonViewCount()

    this.statusInProgress.visibility = View.INVISIBLE
    this.statusIdle.visibility = View.VISIBLE
    this.statusFailed.visibility = View.INVISIBLE

    this.statusIdleText.text =
      CatalogBookAvailabilityStrings.statusString(this.resources, bookStatus)
  }

  @UiThread
  private fun onBookStatusHoldableUI(bookStatus: BookStatus.Holdable) {
    this.uiThread.checkIsUIThread()

    val context = this.requireContext()
    this.buttons.removeAllViews()
    this.buttons.addView(this.createReserveButton(context, this::tryReserveMaybeAuthenticated))
    this.checkButtonViewCount()

    this.statusInProgress.visibility = View.INVISIBLE
    this.statusIdle.visibility = View.VISIBLE
    this.statusFailed.visibility = View.INVISIBLE

    this.statusIdleText.text =
      CatalogBookAvailabilityStrings.statusString(this.resources, bookStatus)
  }

  @UiThread
  private fun onBookStatusHeldUI(bookStatus: BookStatus.Held) {
    this.uiThread.checkIsUIThread()

    val context = this.requireContext()
    this.buttons.removeAllViews()
    when (bookStatus) {
      is BookStatus.Held.HeldInQueue ->
        if (bookStatus.isRevocable) {
          this.buttons.addView(
            this.createRevokeHoldButton(context, this::tryRevokeMaybeAuthenticated))
        } else {
          this.buttons.addView(
            this.createCenteredTextForButtons(context, R.string.catalogHoldCannotCancel))
        }

      is BookStatus.Held.HeldReady -> {
        if (bookStatus.isRevocable) {
          this.buttons.addView(
            this.createRevokeHoldButton(context, this::tryRevokeMaybeAuthenticated))
        }
        this.buttons.addView(this.createGetButton(context, this::tryBorrowMaybeAuthenticated))
      }
    }
    this.checkButtonViewCount()

    this.statusInProgress.visibility = View.INVISIBLE
    this.statusIdle.visibility = View.VISIBLE
    this.statusFailed.visibility = View.INVISIBLE
    this.statusIdleText.text =
      CatalogBookAvailabilityStrings.statusString(this.resources, bookStatus)
  }

  @UiThread
  private fun onBookStatusLoanedUI(bookStatus: BookStatus.Loaned) {
    this.uiThread.checkIsUIThread()

    val context = this.requireContext()
    this.buttons.removeAllViews()
    when (bookStatus) {
      is BookStatus.Loaned.LoanedNotDownloaded ->
        this.buttons.addView(this.createDownloadButton(context, this::tryBorrowMaybeAuthenticated))

      is BookStatus.Loaned.LoanedDownloaded ->
        when (this.parameters.feedEntry.probableFormat) {
          null,
          BookFormats.BookFormatDefinition.BOOK_FORMAT_EPUB -> {
            this.buttons.addView(this.createReadButton(context) {

            })
          }
          BookFormats.BookFormatDefinition.BOOK_FORMAT_AUDIO -> {
            this.buttons.addView(this.createListenButton(context) {

            })
          }
          BookFormats.BookFormatDefinition.BOOK_FORMAT_PDF -> {
            this.buttons.addView(this.createReadButton(context) {

            })
          }
        }
    }
    if (bookStatus.returnable) {
      this.buttons.addView(this.createButtonSpace(context))
      this.buttons.addView(this.createRevokeLoanButton(
        context, this@CatalogFragmentBookDetail::tryRevokeMaybeAuthenticated))
    }
    this.checkButtonViewCount()

    this.statusInProgress.visibility = View.INVISIBLE
    this.statusIdle.visibility = View.VISIBLE
    this.statusFailed.visibility = View.INVISIBLE
    this.statusIdleText.text =
      CatalogBookAvailabilityStrings.statusString(this.resources, bookStatus)
  }

  @UiThread
  private fun onBookStatusDownloadingUI(bookStatus: BookStatus.Downloading) {
    this.uiThread.checkIsUIThread()

    val context = this.requireContext()
    this.buttons.removeAllViews()
    this.buttons.addView(this.createCancelDownloadButton(context) {
      this.tryCancelDownload()
    })
    this.checkButtonViewCount()

    this.statusInProgress.visibility = View.VISIBLE
    this.statusIdle.visibility = View.INVISIBLE
    this.statusFailed.visibility = View.INVISIBLE
    this.statusInProgressText.visibility = View.VISIBLE
    this.statusInProgressText.text =
      this.progressPercentText(bookStatus.currentTotalBytes, bookStatus.expectedTotalBytes)
    this.statusInProgressBar.isIndeterminate = false
    this.statusInProgressBar.progress =
      this.progressPercent(bookStatus.currentTotalBytes, bookStatus.expectedTotalBytes)
  }

  @UiThread
  private fun onBookStatusRequestingDownloadUI(bookStatus: BookStatus.RequestingDownload) {
    this.uiThread.checkIsUIThread()

    val context = this.requireContext()
    this.buttons.removeAllViews()
    this.buttons.addView(this.createCenteredTextForButtons(context, R.string.catalogRequesting))
    this.checkButtonViewCount()

    this.statusInProgress.visibility = View.VISIBLE
    this.statusIdle.visibility = View.INVISIBLE
    this.statusFailed.visibility = View.INVISIBLE
    this.statusInProgressText.visibility = View.GONE
    this.statusInProgressBar.isIndeterminate = true
  }

  @UiThread
  private fun onBookStatusRequestingRevokeUI(bookStatus: BookStatus.RequestingRevoke) {
    this.uiThread.checkIsUIThread()

    val context = this.requireContext()
    this.buttons.removeAllViews()
    this.buttons.addView(this.createCenteredTextForButtons(context, R.string.catalogRequesting))
    this.checkButtonViewCount()

    this.statusInProgress.visibility = View.VISIBLE
    this.statusIdle.visibility = View.INVISIBLE
    this.statusFailed.visibility = View.INVISIBLE
    this.statusInProgressText.visibility = View.GONE
    this.statusInProgressBar.isIndeterminate = true
  }

  @UiThread
  private fun onBookStatusFailedDownloadUI(bookStatus: BookStatus.FailedDownload) {
    this.uiThread.checkIsUIThread()

    val context = this.requireContext()
    this.buttons.removeAllViews()
    this.buttons.addView(this.createDismissButton(context) {
      this.tryDismissError()
    })
    this.buttons.addView(this.createButtonSpace(context))
    this.buttons.addView(this.createDetailsButton(context) {
      this.tryShowError()
    })
    this.buttons.addView(this.createButtonSpace(context))
    this.buttons.addView(this.createRetryButton(context, this::tryBorrowMaybeAuthenticated))
    this.checkButtonViewCount()

    this.statusInProgress.visibility = View.INVISIBLE
    this.statusIdle.visibility = View.INVISIBLE
    this.statusFailed.visibility = View.VISIBLE
  }

  @UiThread
  private fun onBookStatusFailedRevokeUI(bookStatus: BookStatus.FailedRevoke) {
    this.uiThread.checkIsUIThread()

    val context = this.requireContext()
    this.buttons.removeAllViews()
    this.buttons.addView(this.createDismissButton(context) {
      this.tryDismissError()
    })
    this.buttons.addView(this.createButtonSpace(context))
    this.buttons.addView(this.createDetailsButton(context) {
      this.tryShowError()
    })
    this.buttons.addView(this.createButtonSpace(context))
    this.buttons.addView(this.createRetryButton(context, this::tryRevokeMaybeAuthenticated))
    this.checkButtonViewCount()

    this.statusInProgress.visibility = View.INVISIBLE
    this.statusIdle.visibility = View.INVISIBLE
    this.statusFailed.visibility = View.VISIBLE
  }

  private fun progressPercentText(
    currentTotalBytes: Long,
    expectedTotalBytes: Long
  ): String {
    val percentI = this.progressPercent(currentTotalBytes, expectedTotalBytes)
    return "$percentI%"
  }

  private fun progressPercent(
    currentTotalBytes: Long,
    expectedTotalBytes: Long
  ): Int {
    val percentF = (currentTotalBytes.toDouble() / expectedTotalBytes.toDouble()) * 100.0
    return percentF.toInt()
  }

  @UiThread
  private fun createButtonSpace(context: Context): Space {
    val space = Space(context)
    space.layoutParams = this.buttonSpaceLayoutParameters()
    return space
  }

  @UiThread
  private fun createCenteredTextForButtons(
    context: Context,
    @StringRes res: Int
  ): TextView {
    val text = TextView(context)
    text.gravity = Gravity.CENTER
    text.text = context.getString(res)
    return text
  }

  @UiThread
  private fun createButton(
    context: Context,
    text: Int,
    description: Int,
    onClick: (Button) -> Unit
  ): Button {
    val button = Button(context)
    button.text = context.getString(text)
    button.contentDescription = context.getString(description)
    button.layoutParams = this.buttonLayoutParameters()
    button.setOnClickListener {
      button.isEnabled = false
      onClick.invoke(button)
    }
    return button
  }

  @UiThread
  private fun createReadButton(
    context: Context,
    onClick: (Button) -> Unit
  ): Button {
    return this.createButton(
      context = context,
      text = R.string.catalogRead,
      description = R.string.catalogAccessibilityBookRead,
      onClick = onClick
    )
  }

  @UiThread
  private fun createListenButton(
    context: Context,
    onClick: (Button) -> Unit
  ): Button {
    return this.createButton(
      context = context,
      text = R.string.catalogListen,
      description = R.string.catalogAccessibilityBookListen,
      onClick = onClick
    )
  }

  @UiThread
  private fun createDownloadButton(
    context: Context,
    onClick: (Button) -> Unit
  ): Button {
    return this.createButton(
      context = context,
      text = R.string.catalogDownload,
      description = R.string.catalogAccessibilityBookDownload,
      onClick = onClick
    )
  }

  @UiThread
  private fun createRevokeHoldButton(
    context: Context,
    onClick: (Button) -> Unit
  ): Button {
    return this.createButton(
      context = context,
      text = R.string.catalogCancelHold,
      description = R.string.catalogAccessibilityBookRevokeHold,
      onClick = onClick
    )
  }

  @UiThread
  private fun createRevokeLoanButton(
    context: Context,
    onClick: (Button) -> Unit
  ): Button {
    return this.createButton(
      context = context,
      text = R.string.catalogReturn,
      description = R.string.catalogAccessibilityBookRevokeLoan,
      onClick = onClick
    )
  }

  @UiThread
  private fun createCancelDownloadButton(
    context: Context,
    onClick: (Button) -> Unit
  ): Button {
    return this.createButton(
      context = context,
      text = R.string.catalogCancel,
      description = R.string.catalogAccessibilityBookDownloadCancel,
      onClick = onClick
    )
  }

  @UiThread
  private fun createReserveButton(
    context: Context,
    onClick: (Button) -> Unit
  ): Button {
    return this.createButton(
      context = context,
      text = R.string.catalogReserve,
      description = R.string.catalogAccessibilityBookReserve,
      onClick = onClick
    )
  }

  @UiThread
  private fun createGetButton(
    context: Context,
    onClick: (Button) -> Unit
  ): Button {
    return this.createButton(
      context = context,
      text = R.string.catalogGet,
      description = R.string.catalogAccessibilityBookBorrow,
      onClick = onClick
    )
  }

  @UiThread
  private fun createRetryButton(
    context: Context,
    onClick: (Button) -> Unit
  ): Button {
    return this.createButton(
      context = context,
      text = R.string.catalogRetry,
      description = R.string.catalogAccessibilityBookErrorRetry,
      onClick = onClick
    )
  }

  @UiThread
  private fun createDetailsButton(
    context: Context,
    onClick: (Button) -> Unit
  ): Button {
    return this.createButton(
      context = context,
      text = R.string.catalogDetails,
      description = R.string.catalogAccessibilityBookErrorDetails,
      onClick = onClick
    )
  }

  @UiThread
  private fun createDismissButton(
    context: Context,
    onClick: (Button) -> Unit
  ): Button {
    return this.createButton(
      context = context,
      text = R.string.catalogDismiss,
      description = R.string.catalogAccessibilityBookErrorDismiss,
      onClick = onClick
    )
  }

  @UiThread
  private fun buttonSpaceLayoutParameters(): LinearLayout.LayoutParams {
    val spaceLayoutParams = LinearLayout.LayoutParams(0, 0)
    spaceLayoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
    spaceLayoutParams.width = this.screenSize.dpToPixels(16).toInt()
    return spaceLayoutParams
  }

  @UiThread
  private fun buttonLayoutParameters(): LinearLayout.LayoutParams {
    val buttonLayoutParams = LinearLayout.LayoutParams(0, 0)
    buttonLayoutParams.weight = 1.0f
    buttonLayoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
    buttonLayoutParams.width = 0
    return buttonLayoutParams
  }

  @UiThread
  private fun checkButtonViewCount() {
    Preconditions.checkState(
      this.buttons.childCount > 0,
      "At least one button must be present (existing ${this.buttons.childCount})")
  }

  override fun onStop() {
    super.onStop()
    this.bookRegistrySubscription?.unsubscribe()
  }

  @UiThread
  override fun onDialogClosed() {
    this.uiThread.checkIsUIThread()
    this.logger.debug("login dialog was closed")
    this.runOnLoginDialogClosed.getAndSet(null)?.invoke()
  }

  /**
   * Open a login dialog. The given `execute` callback will be executed when the dialog is
   * closed.
   *
   * @see [onDialogClosed]
   */

  private fun openLoginDialogAndThen(execute: () -> Unit) {
    val dialogParameters = CatalogFragmentLoginDialogParameters(this.parameters.accountId)
    val dialog = CatalogFragmentLoginDialog.create(dialogParameters)
    dialog.setTargetFragment(this, 0)
    dialog.show(this.requireFragmentManager(), "LOGIN")
    this.runOnLoginDialogClosed.set(execute)
  }

  /**
   * @return `true` if a login is required on the current account
   */

  private fun isLoginRequired(): Boolean {
    return try {
      val account =
        this.profilesController.profileCurrent()
          .account(this.parameters.accountId)
      val requiresLogin = account.requiresCredentials
      val isNotLoggedIn = !(account.loginState is AccountLoginState.AccountLoggedIn)
      requiresLogin && isNotLoggedIn
    } catch (e: Exception) {
      this.logger.error("could not retrieve account: ", e)
      false
    }
  }

  /*
   * Try borrowing, performing the authentication dialog step if necessary.
   */

  private fun tryBorrowMaybeAuthenticated(button: Button) {
    if (!this.isLoginRequired()) {
      this.tryBorrowAuthenticated()
      return
    }

    this.openLoginDialogAndThen {
      if (!this.isLoginRequired()) {
        this.tryBorrowAuthenticated()
      } else {
        this.logger.debug("authentication did not complete")
        button.isEnabled = true
      }
    }
  }

  /*
   * Try revoking, performing the authentication dialog step if necessary.
   */

  private fun tryRevokeMaybeAuthenticated(button: Button) {
    if (!this.isLoginRequired()) {
      this.tryRevokeAuthenticated()
      return
    }

    this.openLoginDialogAndThen {
      if (!this.isLoginRequired()) {
        this.tryRevokeAuthenticated()
      } else {
        this.logger.debug("authentication did not complete")
        button.isEnabled = true
      }
    }
  }

  /*
   * Try reserving, performing the authentication dialog step if necessary.
   */

  private fun tryReserveMaybeAuthenticated(button: Button) {
    if (!this.isLoginRequired()) {
      this.tryReserveAuthenticated()
      return
    }

    this.openLoginDialogAndThen {
      if (!this.isLoginRequired()) {
        this.tryReserveAuthenticated()
      } else {
        this.logger.debug("authentication did not complete")
        button.isEnabled = true
      }
    }
  }

  private fun tryReserveAuthenticated() {
    this.logger.debug("reserving: {}", this.parameters.bookID)
  }

  private fun tryRevokeAuthenticated() {
    this.logger.debug("revoking: {}", this.parameters.bookID)
  }

  private fun tryBorrowAuthenticated() {
    this.logger.debug("borrowing: {}", this.parameters.bookID)
  }

  private fun tryShowError() {
    this.logger.debug("showing error: {}", this.parameters.bookID)
  }

  private fun tryDismissError() {
    this.logger.debug("dismissing error: {}", this.parameters.bookID)
  }

  private fun tryCancelDownload() {
    this.logger.debug("cancelling: {}", this.parameters.bookID)
  }
}
