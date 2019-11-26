package org.nypl.simplified.ui.catalog

import android.content.Context
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TableLayout
import android.widget.TextView
import androidx.annotation.UiThread
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.google.common.base.Preconditions
import com.io7m.jfunctional.Some
import io.reactivex.disposables.Disposable
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormatterBuilder
import org.nypl.simplified.accounts.api.AccountLoginState
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookFormat
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.book_database.api.BookFormats
import org.nypl.simplified.books.book_registry.BookRegistryReadableType
import org.nypl.simplified.books.book_registry.BookStatus
import org.nypl.simplified.books.book_registry.BookStatusEvent
import org.nypl.simplified.books.book_registry.BookWithStatus
import org.nypl.simplified.books.controller.api.BooksControllerType
import org.nypl.simplified.books.covers.BookCoverProviderType
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.futures.FluentFutureExtensions.map
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.presentableerror.api.PresentableErrorType
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.nypl.simplified.taskrecorder.api.TaskStepResolution
import org.nypl.simplified.toolbar.ToolbarHostType
import org.nypl.simplified.ui.errorpage.ErrorPageParameters
import org.nypl.simplified.ui.host.HostViewModel
import org.nypl.simplified.ui.host.HostViewModelReadableType
import org.nypl.simplified.ui.screen.ScreenSizeInformationType
import org.nypl.simplified.ui.thread.api.UIThreadServiceType
import org.slf4j.LoggerFactory
import java.util.SortedMap
import java.util.concurrent.atomic.AtomicReference

/**
 * A book detail page.
 */

class CatalogFragmentBookDetail : Fragment() {

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
  private lateinit var buttonCreator: CatalogButtons
  private lateinit var buttons: LinearLayout
  private lateinit var configurationService: CatalogConfigurationServiceType
  private lateinit var cover: ImageView
  private lateinit var coverProgress: ProgressBar
  private lateinit var covers: BookCoverProviderType
  private lateinit var debugStatus: TextView
  private lateinit var format: TextView
  private lateinit var hostModel: HostViewModelReadableType
  private lateinit var loginDialogModel: CatalogLoginViewModel
  private lateinit var metadata: TableLayout
  private lateinit var navigation: CatalogNavigationControllerType
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
  private lateinit var toolbar: Toolbar
  private lateinit var uiThread: UIThreadServiceType
  private val parametersId = PARAMETERS_ID
  private val runOnLoginDialogClosed: AtomicReference<() -> Unit> = AtomicReference()
  private var bookRegistrySubscription: Disposable? = null
  private var debugService: CatalogDebuggingServiceType? = null
  private var loginDialogModelSubscription: Disposable? = null

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
    this.parameters = this.arguments!![this.parametersId] as CatalogFragmentBookDetailParameters
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {

    this.hostModel =
      ViewModelProviders.of(this.requireActivity())
        .get(HostViewModel::class.java)

    this.configurationService =
      this.hostModel.services.requireService(CatalogConfigurationServiceType::class.java)
    this.debugService =
      this.hostModel.services.optionalService(CatalogDebuggingServiceType::class.java)
    this.bookRegistry =
      this.hostModel.services.requireService(BookRegistryReadableType::class.java)
    this.uiThread =
      this.hostModel.services.requireService(UIThreadServiceType::class.java)
    this.screenSize =
      this.hostModel.services.requireService(ScreenSizeInformationType::class.java)
    this.profilesController =
      this.hostModel.services.requireService(ProfilesControllerType::class.java)
    this.booksController =
      this.hostModel.services.requireService(BooksControllerType::class.java)
    this.covers =
      this.hostModel.services.requireService(BookCoverProviderType::class.java)

    this.navigation =
      this.hostModel.navigationController(CatalogNavigationControllerType::class.java)

    this.loginDialogModel =
      ViewModelProviders.of(this.requireActivity())
        .get(CatalogLoginViewModel::class.java)

    this.loginDialogModelSubscription =
      this.loginDialogModel.loginDialogCompleted.subscribe {
        this.onDialogClosed()
      }

    this.buttonCreator =
      CatalogButtons(this.requireContext(), this.screenSize)

    val layout =
      inflater.inflate(R.layout.book_detail, container, false)

    this.cover =
      layout.findViewById(R.id.bookDetailCover)
    this.coverProgress =
      layout.findViewById(R.id.bookDetailCoverProgress)
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
      if (this.debugService?.showBookDetailStatus == true) {
        View.VISIBLE
      } else {
        View.INVISIBLE
      }

    return layout
  }

  override fun onStart() {
    super.onStart()

    if (this.activity is ToolbarHostType) {
      this.toolbar = (this.activity as ToolbarHostType).toolbar
    } else {
      throw IllegalStateException("The activity (${this.activity}) hosting this fragment must implement ${ToolbarHostType::class.java}")
    }

    val shortAnimationDuration =
      this.requireContext().resources.getInteger(android.R.integer.config_shortAnimTime)

    this.cover.visibility = View.INVISIBLE
    this.coverProgress.visibility = View.VISIBLE

    this.covers.loadCoverInto(
      this.parameters.feedEntry,
      this.cover,
      this.cover.layoutParams.width,
      this.cover.layoutParams.height
    ).map {
      this.uiThread.runOnUIThread {
        this.coverProgress.visibility = View.INVISIBLE

        this.cover.visibility = View.VISIBLE
        this.cover.alpha = 0.0f
        this.cover.animate()
          .alpha(1f)
          .setDuration(shortAnimationDuration.toLong())
          .setListener(null)
      }
    }

    /*
     * Retrieve the current status of the book, or synthesize a status value based on the
     * OPDS feed entry if the book is not in the registry. The book will only be in the
     * registry if the user has ever tried to borrow it (as per the registry spec).
     */

    val status =
      this.bookRegistry.bookOrNull(this.parameters.bookID)
        ?: run {
          val book = Book(
            id = this.parameters.bookID,
            account = this.parameters.accountId,
            cover = null,
            thumbnail = null,
            entry = this.parameters.feedEntry.feedEntry,
            formats = listOf()
          )
          BookWithStatus(book, BookStatus.fromBook(book))
        }

    this.onBookStatusUI(status)
    this.onOPDSFeedEntryUI(this.parameters.feedEntry)
    this.configureToolbar()

    this.bookRegistrySubscription =
      this.bookRegistry.bookEvents()
        .subscribe(this::onBookStatusEvent)
  }

  private fun configureToolbar() {
    val context = this.requireContext()
    this.configureToolbarTitles(context)
    this.configureToolbarMenu(context)
  }

  @UiThread
  private fun configureToolbarMenu(
    context: Context
  ) {
    this.toolbar.menu.clear()
  }

  private fun hasOtherAccounts(): Boolean {
    return try {
      this.profilesController.profileCurrent().accounts().size > 1
    } catch (e: Exception) {
      this.logger.error("could not fetch current account/profile: ", e)
      false
    }
  }

  @UiThread
  private fun configureToolbarTitles(
    context: Context
  ) {
    this.toolbar.title = this.parameters.feedEntry.feedEntry.title

    try {
      val accountProvider =
        this.profilesController.profileCurrent()
          .account(this.parameters.accountId)
          .provider

      this.toolbar.subtitle = accountProvider.displayName
    } catch (e: Exception) {
      this.toolbar.subtitle = ""
    } finally {
      val color = ContextCompat.getColor(context, R.color.simplifiedColorBackground)
      this.toolbar.setTitleTextColor(color)
      this.toolbar.setSubtitleTextColor(color)
    }
  }

  private fun onBookStatusEvent(event: BookStatusEvent) {
    if (event.book() != this.parameters.bookID) {
      return
    }

    val bookWithStatus = this.bookRegistry.bookOrNull(this.parameters.bookID)
    if (bookWithStatus != null) {
      this.uiThread.runOnUIThread {
        this.onBookStatusUI(bookWithStatus)
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
  private fun onBookStatusUI(book: BookWithStatus) {
    this.uiThread.checkIsUIThread()
    this.debugStatus.text = book.javaClass.simpleName

    return when (val status = book.status) {
      is BookStatus.Held ->
        this.onBookStatusHeldUI(status, book.book)
      is BookStatus.Loaned ->
        this.onBookStatusLoanedUI(status, book.book)
      is BookStatus.Holdable ->
        this.onBookStatusHoldableUI(status, book.book)
      is BookStatus.Loanable ->
        this.onBookStatusLoanableUI(status, book.book)
      is BookStatus.RequestingLoan ->
        this.onBookStatusRequestingLoanUI()
      is BookStatus.Revoked ->
        this.onBookStatusRevokedUI(status)
      is BookStatus.FailedLoan ->
        this.onBookStatusFailedLoanUI(status, book.book)
      is BookStatus.FailedRevoke ->
        this.onBookStatusFailedRevokeUI(status, book.book)
      is BookStatus.FailedDownload ->
        this.onBookStatusFailedDownloadUI(status, book.book)
      is BookStatus.RequestingRevoke ->
        this.onBookStatusRequestingRevokeUI()
      is BookStatus.RequestingDownload ->
        this.onBookStatusRequestingDownloadUI()
      is BookStatus.Downloading ->
        this.onBookStatusDownloadingUI(status, book.book)
    }
  }

  @UiThread
  private fun onBookStatusFailedLoanUI(
    bookStatus: BookStatus.FailedLoan,
    book: Book
  ) {
    this.uiThread.checkIsUIThread()

    this.buttons.removeAllViews()
    this.buttons.addView(this.buttonCreator.createDismissButton {
      this.tryDismissBorrowError(book.id)
    })
    this.buttons.addView(this.buttonCreator.createButtonSpace())
    this.buttons.addView(this.buttonCreator.createDetailsButton {
      this.tryShowError(book, bookStatus.result)
    })
    this.buttons.addView(this.buttonCreator.createButtonSpace())
    this.buttons.addView(this.buttonCreator.createRetryButton { button ->
      this.tryBorrowMaybeAuthenticated(button, book)
    })
    this.checkButtonViewCount()

    this.statusInProgress.visibility = View.INVISIBLE
    this.statusIdle.visibility = View.INVISIBLE
    this.statusFailed.visibility = View.VISIBLE
  }

  @UiThread
  private fun onBookStatusRevokedUI(
    bookStatus: BookStatus.Revoked
  ) {
    this.uiThread.checkIsUIThread()

    this.buttons.removeAllViews()
    this.buttons.addView(this.buttonCreator.createCenteredTextForButtons(R.string.catalogRequesting))
    this.checkButtonViewCount()

    this.statusInProgress.visibility = View.INVISIBLE
    this.statusIdle.visibility = View.VISIBLE
    this.statusFailed.visibility = View.INVISIBLE

    this.statusIdleText.text =
      CatalogBookAvailabilityStrings.statusString(this.resources, bookStatus)
  }

  @UiThread
  private fun onBookStatusRequestingLoanUI() {
    this.uiThread.checkIsUIThread()

    this.buttons.removeAllViews()
    this.buttons.addView(this.buttonCreator.createCenteredTextForButtons(R.string.catalogRequesting))
    this.checkButtonViewCount()

    this.statusInProgress.visibility = View.VISIBLE
    this.statusIdle.visibility = View.INVISIBLE
    this.statusFailed.visibility = View.INVISIBLE

    this.statusInProgressText.visibility = View.GONE
    this.statusInProgressBar.isIndeterminate = true
  }

  @UiThread
  private fun onBookStatusLoanableUI(
    bookStatus: BookStatus.Loanable,
    book: Book
  ) {
    this.uiThread.checkIsUIThread()

    this.buttons.removeAllViews()
    this.buttons.addView(this.buttonCreator.createButtonSizedSpace())
    this.buttons.addView(this.buttonCreator.createGetButton { button ->
      this.tryBorrowMaybeAuthenticated(button, book)
    })
    this.buttons.addView(this.buttonCreator.createButtonSizedSpace())
    this.checkButtonViewCount()

    this.statusInProgress.visibility = View.INVISIBLE
    this.statusIdle.visibility = View.VISIBLE
    this.statusFailed.visibility = View.INVISIBLE

    this.statusIdleText.text =
      CatalogBookAvailabilityStrings.statusString(this.resources, bookStatus)
  }

  @UiThread
  private fun onBookStatusHoldableUI(
    bookStatus: BookStatus.Holdable,
    book: Book
  ) {
    this.uiThread.checkIsUIThread()

    this.buttons.removeAllViews()
    this.buttons.addView(this.buttonCreator.createButtonSizedSpace())
    this.buttons.addView(this.buttonCreator.createReserveButton { button ->
      this.tryReserveMaybeAuthenticated(button, book)
    })
    this.buttons.addView(this.buttonCreator.createButtonSizedSpace())
    this.checkButtonViewCount()

    this.statusInProgress.visibility = View.INVISIBLE
    this.statusIdle.visibility = View.VISIBLE
    this.statusFailed.visibility = View.INVISIBLE

    this.statusIdleText.text =
      CatalogBookAvailabilityStrings.statusString(this.resources, bookStatus)
  }

  @UiThread
  private fun onBookStatusHeldUI(
    bookStatus: BookStatus.Held,
    book: Book
  ) {
    this.uiThread.checkIsUIThread()

    this.buttons.removeAllViews()
    when (bookStatus) {
      is BookStatus.Held.HeldInQueue ->
        if (bookStatus.isRevocable) {
          this.buttons.addView(this.buttonCreator.createButtonSizedSpace())
          this.buttons.addView(
            this.buttonCreator.createRevokeHoldButton { button ->
              this.tryRevokeMaybeAuthenticated(button, book)
            })
          this.buttons.addView(this.buttonCreator.createButtonSizedSpace())
        } else {
          this.buttons.addView(
            this.buttonCreator.createCenteredTextForButtons(R.string.catalogHoldCannotCancel))
        }

      is BookStatus.Held.HeldReady -> {
        if (bookStatus.isRevocable) {
          this.buttons.addView(
            this.buttonCreator.createRevokeHoldButton { button ->
              this.tryRevokeMaybeAuthenticated(button, book)
            })
        }
        this.buttons.addView(this.buttonCreator.createGetButton { button ->
          this.tryBorrowMaybeAuthenticated(button, book)
        })
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
  private fun onBookStatusLoanedUI(
    bookStatus: BookStatus.Loaned,
    book: Book
  ) {
    this.uiThread.checkIsUIThread()

    this.buttons.removeAllViews()
    when (bookStatus) {
      is BookStatus.Loaned.LoanedNotDownloaded ->
        this.buttons.addView(this.buttonCreator.createDownloadButton { button ->
          this.tryBorrowMaybeAuthenticated(button, book)
        })

      is BookStatus.Loaned.LoanedDownloaded ->
        when (val format = book.findPreferredFormat()) {
          is BookFormat.BookFormatEPUB -> {
            this.buttons.addView(this.buttonCreator.createReadButton {
              this.navigation.openEPUBReader(book, format)
            })
            this.buttons.addView(this.buttonCreator.createButtonSizedSpace())
          }
          is BookFormat.BookFormatAudioBook -> {
            this.buttons.addView(this.buttonCreator.createListenButton {
              this.navigation.openAudioBookListener(book, format)
            })
          }
          is BookFormat.BookFormatPDF -> {
            this.buttons.addView(this.buttonCreator.createReadButton {
              this.navigation.openPDFReader(book, format)
            })
          }
        }
    }
    if (bookStatus.returnable) {
      this.buttons.addView(this.buttonCreator.createButtonSpace())
      this.buttons.addView(this.buttonCreator.createRevokeLoanButton { button ->
        this.tryRevokeMaybeAuthenticated(button, book)
      })
    }
    this.checkButtonViewCount()

    this.statusInProgress.visibility = View.INVISIBLE
    this.statusIdle.visibility = View.VISIBLE
    this.statusFailed.visibility = View.INVISIBLE
    this.statusIdleText.text =
      CatalogBookAvailabilityStrings.statusString(this.resources, bookStatus)
  }

  @UiThread
  private fun onBookStatusDownloadingUI(
    bookStatus: BookStatus.Downloading,
    book: Book
  ) {
    this.uiThread.checkIsUIThread()

    this.buttons.removeAllViews()
    this.buttons.addView(this.buttonCreator.createButtonSizedSpace())
    this.buttons.addView(this.buttonCreator.createCancelDownloadButton {
      this.tryCancelDownload(book.id)
    })
    this.buttons.addView(this.buttonCreator.createButtonSizedSpace())
    this.checkButtonViewCount()

    this.statusInProgress.visibility = View.VISIBLE
    this.statusIdle.visibility = View.INVISIBLE
    this.statusFailed.visibility = View.INVISIBLE
    this.statusInProgressText.visibility = View.VISIBLE
    this.statusInProgressText.text = "${bookStatus.progressPercent.toInt()}%"
    this.statusInProgressBar.isIndeterminate = false
    this.statusInProgressBar.progress = bookStatus.progressPercent.toInt()
  }

  @UiThread
  private fun onBookStatusRequestingDownloadUI() {
    this.uiThread.checkIsUIThread()

    this.buttons.removeAllViews()
    this.buttons.addView(this.buttonCreator.createCenteredTextForButtons(R.string.catalogRequesting))
    this.checkButtonViewCount()

    this.statusInProgress.visibility = View.VISIBLE
    this.statusIdle.visibility = View.INVISIBLE
    this.statusFailed.visibility = View.INVISIBLE
    this.statusInProgressText.visibility = View.GONE
    this.statusInProgressBar.isIndeterminate = true
  }

  @UiThread
  private fun onBookStatusRequestingRevokeUI() {
    this.uiThread.checkIsUIThread()

    this.buttons.removeAllViews()
    this.buttons.addView(this.buttonCreator.createCenteredTextForButtons(R.string.catalogRequesting))
    this.checkButtonViewCount()

    this.statusInProgress.visibility = View.VISIBLE
    this.statusIdle.visibility = View.INVISIBLE
    this.statusFailed.visibility = View.INVISIBLE
    this.statusInProgressText.visibility = View.GONE
    this.statusInProgressBar.isIndeterminate = true
  }

  @UiThread
  private fun onBookStatusFailedDownloadUI(
    bookStatus: BookStatus.FailedDownload,
    book: Book
  ) {
    this.uiThread.checkIsUIThread()

    this.buttons.removeAllViews()
    this.buttons.addView(this.buttonCreator.createDismissButton {
      this.tryDismissBorrowError(book.id)
    })
    this.buttons.addView(this.buttonCreator.createButtonSpace())
    this.buttons.addView(this.buttonCreator.createDetailsButton {
      this.tryShowError(book, bookStatus.result)
    })
    this.buttons.addView(this.buttonCreator.createButtonSpace())
    this.buttons.addView(this.buttonCreator.createRetryButton { button ->
      this.tryBorrowMaybeAuthenticated(button, book)
    })
    this.checkButtonViewCount()

    this.statusInProgress.visibility = View.INVISIBLE
    this.statusIdle.visibility = View.INVISIBLE
    this.statusFailed.visibility = View.VISIBLE
  }

  @UiThread
  private fun onBookStatusFailedRevokeUI(
    bookStatus: BookStatus.FailedRevoke,
    book: Book
  ) {
    this.uiThread.checkIsUIThread()

    this.buttons.removeAllViews()
    this.buttons.addView(this.buttonCreator.createDismissButton {
      this.tryDismissRevokeError(book.id)
    })
    this.buttons.addView(this.buttonCreator.createButtonSpace())
    this.buttons.addView(this.buttonCreator.createDetailsButton {
      this.tryShowError(book, bookStatus.result)
    })
    this.buttons.addView(this.buttonCreator.createButtonSpace())
    this.buttons.addView(this.buttonCreator.createRetryButton { button ->
      this.tryRevokeMaybeAuthenticated(button, book)
    })
    this.checkButtonViewCount()

    this.statusInProgress.visibility = View.INVISIBLE
    this.statusIdle.visibility = View.INVISIBLE
    this.statusFailed.visibility = View.VISIBLE
  }

  @UiThread
  private fun checkButtonViewCount() {
    Preconditions.checkState(
      this.buttons.childCount > 0,
      "At least one button must be present (existing ${this.buttons.childCount})")
  }

  override fun onStop() {
    super.onStop()
    this.bookRegistrySubscription?.dispose()
    this.loginDialogModelSubscription?.dispose()
  }

  @UiThread
  fun onDialogClosed() {
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

  private fun tryBorrowMaybeAuthenticated(
    button: Button,
    book: Book
  ) {
    if (!this.isLoginRequired()) {
      this.tryBorrowAuthenticated(book)
      return
    }

    this.openLoginDialogAndThen {
      if (!this.isLoginRequired()) {
        this.tryBorrowAuthenticated(book)
      } else {
        this.logger.debug("authentication did not complete")
        button.isEnabled = true
      }
    }
  }

  /*
   * Try revoking, performing the authentication dialog step if necessary.
   */

  private fun tryRevokeMaybeAuthenticated(
    button: Button,
    book: Book
  ) {
    if (!this.isLoginRequired()) {
      this.tryRevokeAuthenticated(book)
      return
    }

    this.openLoginDialogAndThen {
      if (!this.isLoginRequired()) {
        this.tryRevokeAuthenticated(book)
      } else {
        this.logger.debug("authentication did not complete")
        button.isEnabled = true
      }
    }
  }

  /*
   * Try reserving, performing the authentication dialog step if necessary.
   */

  private fun tryReserveMaybeAuthenticated(
    button: Button,
    book: Book
  ) {
    if (!this.isLoginRequired()) {
      this.tryReserveAuthenticated(book)
      return
    }

    this.openLoginDialogAndThen {
      if (!this.isLoginRequired()) {
        this.tryReserveAuthenticated(book)
      } else {
        this.logger.debug("authentication did not complete")
        button.isEnabled = true
      }
    }
  }

  private fun tryReserveAuthenticated(book: Book) {
    this.logger.debug("reserving: {}", book.id)
    this.booksController.bookBorrowWithDefaultAcquisition(
      this.parameters.accountId, book.id, book.entry)
  }

  private fun tryRevokeAuthenticated(book: Book) {
    this.logger.debug("revoking: {}", book.id)
    this.booksController.bookRevoke(this.parameters.accountId, book.id)
  }

  private fun tryBorrowAuthenticated(book: Book) {
    this.logger.debug("borrowing: {}", book.id)
    this.booksController.bookBorrowWithDefaultAcquisition(
      this.parameters.accountId, book.id, book.entry)
  }

  private fun <E : PresentableErrorType> tryShowError(
    book: Book,
    result: TaskResult.Failure<E, *>
  ) {
    this.logger.debug("showing error: {}", book.id)

    val errorPageParameters = ErrorPageParameters(
      emailAddress = this.configurationService.supportErrorReportEmailAddress,
      body = "",
      subject = this.configurationService.supportErrorReportSubject,
      attributes = collectAttributes(result),
      taskSteps = result.steps
    )
    this.navigation.openErrorPage(errorPageParameters)
  }

  private fun <E : PresentableErrorType> collectAttributes(
    result: TaskResult.Failure<E, *>
  ): SortedMap<String, String> {
    val attributes = mutableMapOf<String, String>()
    for (step in result.steps) {
      when (val resolution = step.resolution) {
        is TaskStepResolution.TaskStepSucceeded -> {

        }
        is TaskStepResolution.TaskStepFailed -> {
          attributes.putAll(resolution.errorValue.attributes)
        }
      }
    }
    return attributes.toSortedMap()
  }

  private fun tryDismissBorrowError(id: BookID) {
    this.logger.debug("dismissing borrow error: {}", id)
    this.booksController.bookBorrowFailedDismiss(this.parameters.accountId, id)
  }

  private fun tryDismissRevokeError(id: BookID) {
    this.logger.debug("dismissing revoke error: {}", id)
    this.booksController.bookRevokeFailedDismiss(this.parameters.accountId, id)
  }

  private fun tryCancelDownload(id: BookID) {
    this.logger.debug("cancelling: {}", id)
    this.booksController.bookDownloadCancel(this.parameters.accountId, id)
  }
}
