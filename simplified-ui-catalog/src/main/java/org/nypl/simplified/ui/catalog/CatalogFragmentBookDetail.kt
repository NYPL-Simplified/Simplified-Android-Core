package org.nypl.simplified.ui.catalog

import android.content.Context
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import org.librarysimplified.services.api.Services
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookFormat
import org.nypl.simplified.books.book_database.api.BookFormats.BookFormatDefinition.BOOK_FORMAT_AUDIO
import org.nypl.simplified.books.book_database.api.BookFormats.BookFormatDefinition.BOOK_FORMAT_EPUB
import org.nypl.simplified.books.book_database.api.BookFormats.BookFormatDefinition.BOOK_FORMAT_PDF
import org.nypl.simplified.books.book_registry.BookRegistryReadableType
import org.nypl.simplified.books.book_registry.BookStatus
import org.nypl.simplified.books.book_registry.BookStatusEvent
import org.nypl.simplified.books.book_registry.BookWithStatus
import org.nypl.simplified.books.controller.api.BooksControllerType
import org.nypl.simplified.books.covers.BookCoverProviderType
import org.nypl.simplified.buildconfig.api.BuildConfigurationServiceType
import org.nypl.simplified.feeds.api.FeedEntry.FeedEntryOPDS
import org.nypl.simplified.navigation.api.NavigationControllers
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.opds.core.OPDSAvailabilityHeld
import org.nypl.simplified.opds.core.OPDSAvailabilityHeldReady
import org.nypl.simplified.opds.core.OPDSAvailabilityHoldable
import org.nypl.simplified.opds.core.OPDSAvailabilityLoanable
import org.nypl.simplified.opds.core.OPDSAvailabilityLoaned
import org.nypl.simplified.opds.core.OPDSAvailabilityMatcherType
import org.nypl.simplified.opds.core.OPDSAvailabilityOpenAccess
import org.nypl.simplified.opds.core.OPDSAvailabilityRevoked
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.nypl.simplified.ui.accounts.AccountFragmentParameters
import org.nypl.simplified.ui.catalog.R.string
import org.nypl.simplified.ui.errorpage.ErrorPageParameters
import org.nypl.simplified.ui.screen.ScreenSizeInformationType
import org.nypl.simplified.ui.thread.api.UIThreadServiceType
import org.nypl.simplified.ui.toolbar.ToolbarHostType
import org.slf4j.LoggerFactory
import java.net.URI

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
  private lateinit var borrowViewModel: CatalogBorrowViewModel
  private lateinit var buttonCreator: CatalogButtons
  private lateinit var buttons: LinearLayout
  private lateinit var configurationService: BuildConfigurationServiceType
  private lateinit var cover: ImageView
  private lateinit var covers: BookCoverProviderType
  private lateinit var debugStatus: TextView
  private lateinit var format: TextView
  private lateinit var metadata: TableLayout
  private lateinit var parameters: CatalogFragmentBookDetailParameters
  private lateinit var profilesController: ProfilesControllerType
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
  private lateinit var uiThread: UIThreadServiceType
  private val parametersId = PARAMETERS_ID
  private var bookRegistrySubscription: Disposable? = null

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
    this.parameters =
      this.requireArguments()[this.parametersId] as CatalogFragmentBookDetailParameters

    val services = Services.serviceDirectory()

    this.configurationService =
      services.requireService(BuildConfigurationServiceType::class.java)
    this.bookRegistry =
      services.requireService(BookRegistryReadableType::class.java)
    this.uiThread =
      services.requireService(UIThreadServiceType::class.java)
    this.screenSize =
      services.requireService(ScreenSizeInformationType::class.java)
    this.profilesController =
      services.requireService(ProfilesControllerType::class.java)
    this.booksController =
      services.requireService(BooksControllerType::class.java)
    this.covers =
      services.requireService(BookCoverProviderType::class.java)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    this.buttonCreator =
      CatalogButtons(this.requireContext(), this.screenSize)

    val layout =
      inflater.inflate(R.layout.book_detail, container, false)

    this.cover =
      layout.findViewById(R.id.bookDetailCoverImage)
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
    this.related =
      layout.findViewById(R.id.bookDetailRelated)
    this.report =
      layout.findViewById(R.id.bookDetailReport)

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
    this.statusFailedText =
      this.status.findViewById(R.id.failedText)

    this.statusIdle.visibility = View.VISIBLE
    this.statusInProgress.visibility = View.INVISIBLE
    this.statusFailed.visibility = View.INVISIBLE

    this.debugStatus.visibility =
      if (this.configurationService.showDebugBookDetailStatus) {
        View.VISIBLE
      } else {
        View.INVISIBLE
      }

    return layout
  }

  override fun onStart() {
    super.onStart()

    this.borrowViewModel =
      CatalogBorrowViewModelFactory.get(this)

    val targetHeight =
      this.resources.getDimensionPixelSize(R.dimen.cover_detail_height)
    this.covers.loadCoverInto(
      this.parameters.feedEntry, this.cover, 0, targetHeight
    )

    /*
     * Retrieve the current status of the book, or synthesize a status value based on the
     * OPDS feed entry if the book is not in the registry. The book will only be in the
     * registry if the user has ever tried to borrow it (as per the registry spec).
     */

    val status =
      this.bookRegistry.bookOrNull(this.parameters.bookID)
        ?: this.synthesizeBookWithStatus(this.parameters.feedEntry)

    this.onBookChangedUI(status)
    this.onOPDSFeedEntryUI(this.parameters.feedEntry)

    val toolbarHost = this.activity
    if (toolbarHost is ToolbarHostType) {
      val toolbar = (toolbarHost as ToolbarHostType).findToolbar()
      this.configureToolbar(toolbarHost, toolbar)
    } else {
      throw IllegalStateException("The activity ($toolbarHost) hosting this fragment must implement ${ToolbarHostType::class.java}")
    }

    this.bookRegistrySubscription =
      this.bookRegistry.bookEvents()
        .subscribe(this::onBookChanged)
  }

  private fun synthesizeBookWithStatus(
    item: FeedEntryOPDS
  ): BookWithStatus {
    val book = Book(
      id = item.bookID,
      account = item.accountID,
      cover = null,
      thumbnail = null,
      entry = item.feedEntry,
      formats = listOf()
    )
    val status = BookStatus.fromBook(book)
    this.logger.debug("Synthesizing {} with status {}", book.id, status)
    return BookWithStatus(book, status)
  }

  private fun configureToolbar(
    toolbarHost: ToolbarHostType,
    toolbar: Toolbar
  ) {
    val context = this.requireContext()
    toolbarHost.toolbarClearMenu()
    this.configureToolbarTitles(context, toolbar)
  }

  @UiThread
  private fun configureToolbarTitles(
    context: Context,
    toolbar: Toolbar
  ) {
    toolbar.title = this.parameters.feedEntry.feedEntry.title

    try {
      val accountProvider =
        this.profilesController.profileCurrent()
          .account(this.parameters.feedEntry.accountID)
          .provider

      toolbar.subtitle = accountProvider.displayName
    } catch (e: Exception) {
      toolbar.subtitle = ""
    } finally {
      val color = ContextCompat.getColor(context, R.color.simplifiedColorBackground)
      toolbar.setTitleTextColor(color)
      toolbar.setSubtitleTextColor(color)
    }
  }

  private fun onBookChanged(event: BookStatusEvent) {
    val bookWithStatus =
      this.bookRegistry.bookOrNull(event.book())
        ?: synthesizeBookWithStatus(this.parameters.feedEntry)

    // Update the cached parameters with the feed entry. We'll need this later if the availability
    // has changed but it's been removed from the registry (e.g. when revoking a hold).
    this.parameters = this.parameters.copy(
      feedEntry = FeedEntryOPDS(
        accountID = this.parameters.feedEntry.accountID,
        feedEntry = bookWithStatus.book.entry
      )
    )

    this.uiThread.runOnUIThread { this.onBookChangedUI(bookWithStatus) }
    this.onOPDSFeedEntry(
      FeedEntryOPDS(
        bookWithStatus.book.account,
        bookWithStatus.book.entry
      )
    )
  }

  private fun onOPDSFeedEntry(entry: FeedEntryOPDS) {
    this.uiThread.runOnUIThread {
      this.parameters = this.parameters.copy(feedEntry = entry)
      this.onOPDSFeedEntryUI(entry)
    }
  }

  @UiThread
  private fun onOPDSFeedEntryUI(feedEntry: FeedEntryOPDS) {
    this.uiThread.checkIsUIThread()

    // Sanity check; ensure we're attached to a valid context. We've seen some lifecycle related
    // crashes related to being detached when this method executes.
    val context = this.context ?: return

    val opds = feedEntry.feedEntry
    this.title.text = opds.title
    this.authors.text = opds.authorsCommaSeparated

    this.format.text = when (feedEntry.probableFormat) {
      BOOK_FORMAT_EPUB ->
        context.getString(string.catalogBookFormatEPUB)
      BOOK_FORMAT_AUDIO ->
        context.getString(string.catalogBookFormatAudioBook)
      BOOK_FORMAT_PDF ->
        context.getString(string.catalogBookFormatPDF)
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

    val feedRelatedOpt = this.parameters.feedEntry.feedEntry.related
    if (feedRelatedOpt is Some<URI>) {
      val feedRelated = feedRelatedOpt.get()
      this.related.setOnClickListener {
        this.openRelatedFeed(feedRelated)
      }
      this.related.isEnabled = true
    } else {
      this.related.isEnabled = false
    }
  }

  private fun openRelatedFeed(feedRelated: URI) {
    val context = this.requireContext()
    val feedModel = this.createOrGetFeedModel(context)
    val targetFeed =
      feedModel.resolveFeed(
        title = context.resources.getString(R.string.catalogRelatedBooks),
        uri = feedRelated,
        isSearchResults = false
      )
    this.findNavigationController().openFeed(targetFeed)
  }

  private fun createOrGetFeedModel(
    context: Context
  ): CatalogFeedViewModel {
    return ViewModelProviders.of(
      this,
      CatalogFeedViewModelFactory(
        context = context,
        services = Services.serviceDirectory(),
        feedArguments = this.parameters.feedArguments
      )
    ).get(CatalogFeedViewModel::class.java)
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
  private fun onBookChangedUI(book: BookWithStatus) {
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
    this.buttons.addView(
      this.buttonCreator.createDismissButton {
        this.borrowViewModel.tryDismissBorrowError(this.parameters.feedEntry.accountID, book.id)
      }
    )
    this.buttons.addView(this.buttonCreator.createButtonSpace())
    this.buttons.addView(
      this.buttonCreator.createDetailsButton {
        this.tryShowError(book, bookStatus.result)
      }
    )
    this.buttons.addView(this.buttonCreator.createButtonSpace())
    this.buttons.addView(
      this.buttonCreator.createRetryButton {
        this.openLoginDialogIfNecessary()
        this.borrowViewModel.tryBorrowMaybeAuthenticated(book)
      }
    )
    this.checkButtonViewCount()

    this.statusInProgress.visibility = View.INVISIBLE
    this.statusIdle.visibility = View.INVISIBLE
    this.statusFailed.visibility = View.VISIBLE
    this.statusFailedText.text = this.resources.getText(R.string.catalogOperationFailed)
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
    this.buttons.addView(
      this.buttonCreator.createGetButton {
        this.openLoginDialogIfNecessary()
        this.borrowViewModel.tryBorrowMaybeAuthenticated(book)
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

  @UiThread
  private fun onBookStatusHoldableUI(
    bookStatus: BookStatus.Holdable,
    book: Book
  ) {
    this.uiThread.checkIsUIThread()

    this.buttons.removeAllViews()
    this.buttons.addView(this.buttonCreator.createButtonSizedSpace())
    this.buttons.addView(
      this.buttonCreator.createReserveButton {
        this.openLoginDialogIfNecessary()
        this.borrowViewModel.tryReserveMaybeAuthenticated(book)
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
            this.buttonCreator.createRevokeHoldButton {
              this.openLoginDialogIfNecessary()
              this.borrowViewModel.tryRevokeMaybeAuthenticated(book)
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
              this.openLoginDialogIfNecessary()
              this.borrowViewModel.tryRevokeMaybeAuthenticated(book)
            }
          )
        }
        this.buttons.addView(
          this.buttonCreator.createGetButton {
            this.openLoginDialogIfNecessary()
            this.borrowViewModel.tryBorrowMaybeAuthenticated(book)
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

  @UiThread
  private fun onBookStatusLoanedUI(
    bookStatus: BookStatus.Loaned,
    book: Book
  ) {
    this.uiThread.checkIsUIThread()

    this.buttons.removeAllViews()
    when (bookStatus) {
      is BookStatus.Loaned.LoanedNotDownloaded ->
        this.buttons.addView(
          this.buttonCreator.createDownloadButton {
            this.openLoginDialogIfNecessary()
            this.borrowViewModel.tryBorrowMaybeAuthenticated(book)
          }
        )

      is BookStatus.Loaned.LoanedDownloaded ->
        when (val format = book.findPreferredFormat()) {
          is BookFormat.BookFormatPDF,
          is BookFormat.BookFormatEPUB -> {
            this.buttons.addView(
              this.buttonCreator.createReadButton {
                this.findNavigationController().openViewer(this.requireActivity(), book, format)
              }
            )
          }
          is BookFormat.BookFormatAudioBook -> {
            this.buttons.addView(
              this.buttonCreator.createListenButton {
                this.findNavigationController().openViewer(this.requireActivity(), book, format)
              }
            )
          }
        }
    }

    if (bookStatus.returnable) {
      this.buttons.addView(this.buttonCreator.createButtonSpace())
      this.buttons.addView(
        this.buttonCreator.createRevokeLoanButton {
          this.openLoginDialogIfNecessary()
          this.borrowViewModel.tryRevokeMaybeAuthenticated(book)
        }
      )
    }

    if (this.shouldShowDeleteButton(book)) {
      this.buttons.addView(this.buttonCreator.createButtonSpace())
      this.buttons.addView(
        this.buttonCreator.createDeleteButton {
          this.borrowViewModel.tryDelete(book.account, book.id)
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

  /**
   * Determine whether or not a book can be "deleted".
   *
   * A book can be deleted if:
   *
   * * It is loaned, downloaded, and not revocable (because otherwise, a revocation is needed).
   * * It is loanable, but there is a book database entry for it
   * * It is open access but there is a book database entry for it
   */

  private fun shouldShowDeleteButton(book: Book): Boolean {
    return try {
      val profile = this.profilesController.profileCurrent()
      val account = profile.account(this.parameters.feedEntry.accountID)
      return if (account.bookDatabase.books().contains(book.id)) {
        book.entry.availability.matchAvailability(
          object : OPDSAvailabilityMatcherType<Boolean, Exception> {
            override fun onHeldReady(availability: OPDSAvailabilityHeldReady): Boolean =
              false

            override fun onHeld(availability: OPDSAvailabilityHeld): Boolean =
              false

            override fun onHoldable(availability: OPDSAvailabilityHoldable): Boolean =
              false

            override fun onLoaned(availability: OPDSAvailabilityLoaned): Boolean =
              availability.revoke.isNone && book.isDownloaded

            override fun onLoanable(availability: OPDSAvailabilityLoanable): Boolean =
              true

            override fun onOpenAccess(availability: OPDSAvailabilityOpenAccess): Boolean =
              true

            override fun onRevoked(availability: OPDSAvailabilityRevoked): Boolean =
              false
          })
      } else {
        false
      }
    } catch (e: Exception) {
      this.logger.error("could not configure delete button: ", e)
      false
    }
  }

  @UiThread
  private fun onBookStatusDownloadingUI(
    bookStatus: BookStatus.Downloading,
    book: Book
  ) {
    this.uiThread.checkIsUIThread()

    this.buttons.removeAllViews()
    this.buttons.addView(this.buttonCreator.createButtonSizedSpace())
    this.buttons.addView(
      this.buttonCreator.createCancelDownloadButton {
        this.borrowViewModel.tryCancelDownload(book.account, book.id)
      }
    )
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
    this.buttons.addView(
      this.buttonCreator.createDismissButton {
        this.borrowViewModel.tryDismissBorrowError(book.account, book.id)
      }
    )
    this.buttons.addView(this.buttonCreator.createButtonSpace())
    this.buttons.addView(
      this.buttonCreator.createDetailsButton {
        this.tryShowError(book, bookStatus.result)
      }
    )
    this.buttons.addView(this.buttonCreator.createButtonSpace())
    this.buttons.addView(
      this.buttonCreator.createRetryButton {
        this.openLoginDialogIfNecessary()
        this.borrowViewModel.tryBorrowMaybeAuthenticated(book)
      }
    )
    this.checkButtonViewCount()

    this.statusInProgress.visibility = View.INVISIBLE
    this.statusIdle.visibility = View.INVISIBLE
    this.statusFailed.visibility = View.VISIBLE
    this.statusFailedText.text = this.getString(R.string.catalogOperationFailed)
  }

  @UiThread
  private fun onBookStatusFailedRevokeUI(
    bookStatus: BookStatus.FailedRevoke,
    book: Book
  ) {
    this.uiThread.checkIsUIThread()

    this.buttons.removeAllViews()
    this.buttons.addView(
      this.buttonCreator.createDismissButton {
        this.borrowViewModel.tryDismissRevokeError(book.account, book.id)
      }
    )
    this.buttons.addView(this.buttonCreator.createButtonSpace())
    this.buttons.addView(
      this.buttonCreator.createDetailsButton {
        this.tryShowError(book, bookStatus.result)
      }
    )
    this.buttons.addView(this.buttonCreator.createButtonSpace())
    this.buttons.addView(
      this.buttonCreator.createRetryButton {
        this.openLoginDialogIfNecessary()
        this.borrowViewModel.tryRevokeMaybeAuthenticated(book)
      }
    )
    this.checkButtonViewCount()

    this.statusInProgress.visibility = View.INVISIBLE
    this.statusIdle.visibility = View.INVISIBLE
    this.statusFailed.visibility = View.VISIBLE
    this.statusFailedText.text = this.resources.getText(R.string.catalogOperationFailed)
  }

  @UiThread
  private fun openLoginDialogIfNecessary() {
    this.uiThread.checkIsUIThread()

    if (this.borrowViewModel.isLoginRequired(this.parameters.feedEntry.accountID)) {
      this.findNavigationController()
        .openSettingsAccount(
          AccountFragmentParameters(
            accountId = this.parameters.feedEntry.accountID,
            closeOnLoginSuccess = true,
            showPleaseLogInTitle = true
          )
        )
    }
  }

  @UiThread
  private fun checkButtonViewCount() {
    Preconditions.checkState(
      this.buttons.childCount > 0,
      "At least one button must be present (existing ${this.buttons.childCount})"
    )
  }

  override fun onStop() {
    super.onStop()
    this.bookRegistrySubscription?.dispose()
  }

  private fun tryShowError(
    book: Book,
    result: TaskResult.Failure<*>
  ) {
    this.logger.debug("showing error: {}", book.id)

    val errorPageParameters = ErrorPageParameters(
      emailAddress = this.configurationService.supportErrorReportEmailAddress,
      body = "",
      subject = this.configurationService.supportErrorReportSubject,
      attributes = result.attributes.toSortedMap(),
      taskSteps = result.steps
    )
    this.findNavigationController().openErrorPage(errorPageParameters)
  }

  private fun findNavigationController(): CatalogNavigationControllerType =
    NavigationControllers.find(
      activity = this.requireActivity(),
      interfaceType = CatalogNavigationControllerType::class.java
    )
}
