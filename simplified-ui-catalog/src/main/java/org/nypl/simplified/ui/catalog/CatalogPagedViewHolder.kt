package org.nypl.simplified.ui.catalog

import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.UiThread
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.common.base.Preconditions
import com.google.common.util.concurrent.FluentFuture
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import org.librarysimplified.services.api.ServiceDirectoryType
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookFormat
import org.nypl.simplified.books.book_database.api.BookFormats.BookFormatDefinition.BOOK_FORMAT_AUDIO
import org.nypl.simplified.books.book_database.api.BookFormats.BookFormatDefinition.BOOK_FORMAT_EPUB
import org.nypl.simplified.books.book_database.api.BookFormats.BookFormatDefinition.BOOK_FORMAT_PDF
import org.nypl.simplified.books.book_registry.BookRegistryReadableType
import org.nypl.simplified.books.book_registry.BookStatus
import org.nypl.simplified.books.book_registry.BookStatusEvent
import org.nypl.simplified.books.book_registry.BookWithStatus
import org.nypl.simplified.books.covers.BookCoverProviderType
import org.nypl.simplified.buildconfig.api.BuildConfigurationServiceType
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.feeds.api.FeedEntry.FeedEntryCorrupt
import org.nypl.simplified.feeds.api.FeedEntry.FeedEntryOPDS
import org.nypl.simplified.futures.FluentFutureExtensions.map
import org.nypl.simplified.presentableerror.api.PresentableErrorType
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.nypl.simplified.taskrecorder.api.TaskStepResolution
import org.nypl.simplified.ui.accounts.AccountFragmentParameters
import org.nypl.simplified.ui.catalog.R.string
import org.nypl.simplified.ui.errorpage.ErrorPageParameters
import org.nypl.simplified.ui.thread.api.UIThreadServiceType
import org.slf4j.LoggerFactory
import java.util.SortedMap
import java.util.concurrent.atomic.AtomicReference

/**
 * A view holder for a single cell in an infinitely-scrolling feed.
 */

class CatalogPagedViewHolder(
  private val borrowViewModel: CatalogBorrowViewModel,
  private val buttonCreator: CatalogButtons,
  private val context: FragmentActivity,
  private val navigation: () -> CatalogNavigationControllerType,
  private val onBookSelected: (FeedEntryOPDS) -> Unit,
  private val parent: View,
  private val registrySubscriptions: CompositeDisposable,
  private val services: ServiceDirectoryType,
  private val ownership: CatalogFeedOwnership
) : RecyclerView.ViewHolder(parent) {

  private val logger =
    LoggerFactory.getLogger(CatalogPagedViewHolder::class.java)

  private val bookCovers: BookCoverProviderType =
    this.services.requireService(BookCoverProviderType::class.java)
  private val bookRegistry: BookRegistryReadableType =
    this.services.requireService(BookRegistryReadableType::class.java)
  private val configurationService: BuildConfigurationServiceType =
    this.services.requireService(BuildConfigurationServiceType::class.java)
  private val profilesController: ProfilesControllerType =
    this.services.requireService(ProfilesControllerType::class.java)
  private val uiThread: UIThreadServiceType =
    this.services.requireService(UIThreadServiceType::class.java)

  private var thumbnailLoading: FluentFuture<Unit>? = null
  private val runOnLoginDialogClosed: AtomicReference<() -> Unit> = AtomicReference()

  private val idle =
    this.parent.findViewById<ViewGroup>(R.id.bookCellIdle)!!
  private val corrupt =
    this.parent.findViewById<ViewGroup>(R.id.bookCellCorrupt)!!
  private val error =
    this.parent.findViewById<ViewGroup>(R.id.bookCellError)!!
  private val progress =
    this.parent.findViewById<ViewGroup>(R.id.bookCellInProgress)!!

  private val idleCover =
    this.parent.findViewById<ImageView>(R.id.bookCellIdleCover)!!
  private val idleProgress =
    this.parent.findViewById<ProgressBar>(R.id.bookCellIdleCoverProgress)!!
  private val idleTitle =
    this.idle.findViewById<TextView>(R.id.bookCellIdleTitle)!!
  private val idleMeta =
    this.idle.findViewById<TextView>(R.id.bookCellIdleMeta)!!
  private val idleAuthor =
    this.idle.findViewById<TextView>(R.id.bookCellIdleAuthor)!!
  private val idleButtons =
    this.idle.findViewById<ViewGroup>(R.id.bookCellIdleButtons)!!

  private val progressProgress =
    this.parent.findViewById<ProgressBar>(R.id.bookCellInProgressBar)!!
  private val progressText =
    this.parent.findViewById<TextView>(R.id.bookCellInProgressTitle)!!

  private val errorTitle =
    this.error.findViewById<TextView>(R.id.bookCellErrorTitle)
  private val errorDismiss =
    this.error.findViewById<Button>(R.id.bookCellErrorButtonDismiss)
  private val errorDetails =
    this.error.findViewById<Button>(R.id.bookCellErrorButtonDetails)
  private val errorRetry =
    this.error.findViewById<Button>(R.id.bookCellErrorButtonRetry)

  private var bookSubscription: Disposable? = null
  private var feedEntry: FeedEntry? = null
  private var loginSubscription: Disposable? = null

  fun bindTo(item: FeedEntry?) {
    this.feedEntry = item
    this.unbind()

    return when (item) {
      is FeedEntryCorrupt -> {
        this.setVisibilityIfNecessary(this.corrupt, View.VISIBLE)
        this.checkSomethingIsVisible()
      }

      is FeedEntryOPDS -> {
        val newBookSubscription =
          this.bookRegistry.bookEvents().subscribe { bookEvent ->
            if (bookEvent.book() == item.bookID) {
              this.onBookChanged(bookEvent)
            }
          }

        this.bookSubscription = newBookSubscription
        this.registrySubscriptions.add(newBookSubscription)
        this.onFeedEntryOPDSUI(item)

        /*
         * Retrieve the current status of the book, or synthesize a status value based on the
         * OPDS feed entry if the book is not in the registry. The book will only be in the
         * registry if the user has ever tried to borrow it (as per the registry spec).
         */

        val status =
          this.bookRegistry.bookOrNull(item.bookID)
            ?: this.synthesizeBookWithStatus(item)

        this.onBookWithStatus(status)
        this.checkSomethingIsVisible()
      }

      null -> {
        this.unbind()
        this.setVisibilityIfNecessary(this.idle, View.VISIBLE)
        this.setVisibilityIfNecessary(this.idleProgress, View.VISIBLE)
        this.checkSomethingIsVisible()
      }
    }
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

  private fun setVisibilityIfNecessary(
    view: View,
    visibility: Int
  ) {
    if (view.visibility != visibility) {
      view.visibility = visibility
    }
  }

  @UiThread
  private fun onFeedEntryOPDSUI(item: FeedEntryOPDS) {
    this.uiThread.checkIsUIThread()

    this.setVisibilityIfNecessary(this.corrupt, View.GONE)
    this.setVisibilityIfNecessary(this.error, View.GONE)
    this.setVisibilityIfNecessary(this.idle, View.VISIBLE)
    this.setVisibilityIfNecessary(this.progress, View.GONE)

    this.setVisibilityIfNecessary(this.idleCover, View.INVISIBLE)
    this.idleCover.setImageDrawable(null)
    this.idleCover.contentDescription =
      CatalogBookAccessibilityStrings.coverDescription(this.context.resources, item)

    this.setVisibilityIfNecessary(this.idleProgress, View.VISIBLE)
    this.idleTitle.text = item.feedEntry.title
    this.idleAuthor.text = item.feedEntry.authorsCommaSeparated
    this.errorTitle.text = item.feedEntry.title

    this.idleMeta.text = when (item.probableFormat) {
      BOOK_FORMAT_EPUB ->
        context.getString(string.catalogBookFormatEPUB)
      BOOK_FORMAT_AUDIO ->
        context.getString(string.catalogBookFormatAudioBook)
      BOOK_FORMAT_PDF ->
        context.getString(string.catalogBookFormatPDF)
      null -> ""
    }

    val targetHeight =
      this.parent.resources.getDimensionPixelSize(R.dimen.cover_thumbnail_height)
    val targetWidth = 0
    this.thumbnailLoading =
      this.bookCovers.loadThumbnailInto(
        item, this.idleCover, targetWidth, targetHeight
      ).map {
        this.uiThread.runOnUIThread {
          this.setVisibilityIfNecessary(this.idleProgress, View.INVISIBLE)
          this.setVisibilityIfNecessary(this.idleCover, View.VISIBLE)
        }
      }

    val onClick: (View) -> Unit = { this.onBookSelected.invoke(item) }
    this.idle.setOnClickListener(onClick)
    this.idleTitle.setOnClickListener(onClick)
    this.idleCover.setOnClickListener(onClick)
  }

  private fun onBookChanged(event: BookStatusEvent) {
    val previousEntry =
      this.feedEntry as FeedEntryOPDS

    val bookWithStatus =
      this.bookRegistry.bookOrNull(event.book())
        ?: this.synthesizeBookWithStatus(previousEntry)

    // Update the cached parameters with the feed entry. We'll need this later if the availability
    // has changed but it's been removed from the registry (e.g. when revoking a hold).
    this.feedEntry = FeedEntryOPDS(
      accountID = previousEntry.accountID,
      feedEntry = bookWithStatus.book.entry
    )

    this.uiThread.runOnUIThread {
      this.onBookChangedUI(bookWithStatus)
      this.checkSomethingIsVisible()
    }
  }

  private fun checkSomethingIsVisible() {
    Preconditions.checkState(
      this.idle.visibility == View.VISIBLE ||
        this.progress.visibility == View.VISIBLE ||
        this.corrupt.visibility == View.VISIBLE ||
        this.error.visibility == View.VISIBLE,
      "Something must be visible!"
    )
  }

  @UiThread
  private fun onBookChangedUI(book: BookWithStatus) {
    this.uiThread.checkIsUIThread()
    this.bindTo(this.feedEntry)
    this.onFeedEntryOPDSUI(this.feedEntry as FeedEntryOPDS)
    this.onBookWithStatus(book)
  }

  @UiThread
  private fun onBookWithStatus(book: BookWithStatus) {
    return when (val status = book.status) {
      is BookStatus.Held.HeldInQueue ->
        this.onBookStatusHeldInQueue(status, book.book)
      is BookStatus.Held.HeldReady ->
        this.onBookStatusHeldReady(status, book.book)
      is BookStatus.Holdable ->
        this.onBookStatusHoldable(book.book)
      is BookStatus.Loanable ->
        this.onBookStatusLoanable(book.book)
      is BookStatus.Loaned.LoanedNotDownloaded ->
        this.onBookStatusLoanedNotDownloaded(book.book)
      is BookStatus.Loaned.LoanedDownloaded ->
        this.onBookStatusLoanedDownloaded(book.book)
      is BookStatus.Revoked ->
        this.onBookStatusRevoked(book)
      is BookStatus.FailedRevoke ->
        this.onBookStatusFailedRevoke(status, book.book)
      is BookStatus.FailedDownload ->
        this.onBookStatusFailedDownload(status, book.book)
      is BookStatus.FailedLoan ->
        this.onBookStatusFailedLoan(status, book.book)

      is BookStatus.RequestingRevoke,
      is BookStatus.RequestingLoan,
      is BookStatus.RequestingDownload -> {
        this.setVisibilityIfNecessary(this.corrupt, View.GONE)
        this.setVisibilityIfNecessary(this.error, View.GONE)
        this.setVisibilityIfNecessary(this.idle, View.GONE)
        this.setVisibilityIfNecessary(this.progress, View.VISIBLE)

        this.progressText.text = book.book.entry.title
        this.progressProgress.isIndeterminate = true
      }

      is BookStatus.Downloading ->
        this.onBookStatusDownloading(book, status)
    }
  }

  @UiThread
  private fun onBookStatusFailedRevoke(
    bookStatus: BookStatus.FailedRevoke,
    book: Book
  ) {
    this.setVisibilityIfNecessary(this.corrupt, View.GONE)
    this.setVisibilityIfNecessary(this.error, View.VISIBLE)
    this.setVisibilityIfNecessary(this.idle, View.GONE)
    this.setVisibilityIfNecessary(this.progress, View.GONE)

    this.errorDismiss.setOnClickListener {
      this.borrowViewModel.tryDismissRevokeError(book.account, book.id)
    }
    this.errorDetails.setOnClickListener {
      this.tryShowError(book, bookStatus.result)
    }
    this.errorRetry.setOnClickListener {
      this.borrowViewModel.tryRevokeMaybeAuthenticated(book)
    }
  }

  @UiThread
  private fun onBookStatusFailedDownload(
    bookStatus: BookStatus.FailedDownload,
    book: Book
  ) {
    this.setVisibilityIfNecessary(this.corrupt, View.GONE)
    this.setVisibilityIfNecessary(this.error, View.VISIBLE)
    this.setVisibilityIfNecessary(this.idle, View.GONE)
    this.setVisibilityIfNecessary(this.progress, View.GONE)

    this.errorDismiss.setOnClickListener {
      this.borrowViewModel.tryDismissBorrowError(book.account, book.id)
    }
    this.errorDetails.setOnClickListener {
      this.tryShowError(book, bookStatus.result)
    }
    this.errorRetry.setOnClickListener {
      this.openLoginDialogIfNecessary(book.account)
      this.borrowViewModel.tryBorrowMaybeAuthenticated(book)
    }
  }

  @UiThread
  private fun onBookStatusFailedLoan(
    bookStatus: BookStatus.FailedLoan,
    book: Book
  ) {
    this.setVisibilityIfNecessary(this.corrupt, View.GONE)
    this.setVisibilityIfNecessary(this.error, View.VISIBLE)
    this.setVisibilityIfNecessary(this.idle, View.GONE)
    this.setVisibilityIfNecessary(this.progress, View.GONE)

    this.errorDismiss.setOnClickListener {
      this.borrowViewModel.tryDismissBorrowError(book.account, book.id)
    }
    this.errorDetails.setOnClickListener {
      this.tryShowError(book, bookStatus.result)
    }
    this.errorRetry.setOnClickListener {
      this.openLoginDialogIfNecessary(book.account)
      this.borrowViewModel.tryBorrowMaybeAuthenticated(book)
    }
  }

  @UiThread
  private fun onBookStatusLoanedNotDownloaded(book: Book) {
    this.setVisibilityIfNecessary(this.corrupt, View.GONE)
    this.setVisibilityIfNecessary(this.error, View.GONE)
    this.setVisibilityIfNecessary(this.idle, View.VISIBLE)
    this.setVisibilityIfNecessary(this.progress, View.GONE)

    this.idleButtons.removeAllViews()
    this.idleButtons.addView(this.buttonCreator.createDownloadButton {
      this.openLoginDialogIfNecessary(book.account)
      this.borrowViewModel.tryBorrowMaybeAuthenticated(book)
    })
    this.idleButtons.addView(this.buttonCreator.createButtonSizedSpace())
    this.idleButtons.addView(this.buttonCreator.createButtonSizedSpace())
  }

  @UiThread
  private fun onBookStatusLoanable(book: Book) {
    this.setVisibilityIfNecessary(this.corrupt, View.GONE)
    this.setVisibilityIfNecessary(this.error, View.GONE)
    this.setVisibilityIfNecessary(this.idle, View.VISIBLE)
    this.setVisibilityIfNecessary(this.progress, View.GONE)

    this.idleButtons.removeAllViews()
    this.idleButtons.addView(this.buttonCreator.createGetButton {
      this.openLoginDialogIfNecessary(book.account)
      this.borrowViewModel.tryBorrowMaybeAuthenticated(book)
    })
    this.idleButtons.addView(this.buttonCreator.createButtonSizedSpace())
    this.idleButtons.addView(this.buttonCreator.createButtonSizedSpace())
  }

  @UiThread
  private fun onBookStatusHoldable(book: Book) {
    this.setVisibilityIfNecessary(this.corrupt, View.GONE)
    this.setVisibilityIfNecessary(this.error, View.GONE)
    this.setVisibilityIfNecessary(this.idle, View.VISIBLE)
    this.setVisibilityIfNecessary(this.progress, View.GONE)

    this.idleButtons.removeAllViews()
    this.idleButtons.addView(this.buttonCreator.createReserveButton {
      this.openLoginDialogIfNecessary(book.account)
      this.borrowViewModel.tryReserveMaybeAuthenticated(book)
    })
    this.idleButtons.addView(this.buttonCreator.createButtonSizedSpace())
    this.idleButtons.addView(this.buttonCreator.createButtonSizedSpace())
  }

  @UiThread
  private fun onBookStatusHeldReady(
    status: BookStatus.Held.HeldReady,
    book: Book
  ) {
    this.setVisibilityIfNecessary(this.corrupt, View.GONE)
    this.setVisibilityIfNecessary(this.error, View.GONE)
    this.setVisibilityIfNecessary(this.idle, View.VISIBLE)
    this.setVisibilityIfNecessary(this.progress, View.GONE)

    this.idleButtons.removeAllViews()
    if (status.isRevocable) {
      this.idleButtons.addView(
        this.buttonCreator.createRevokeHoldButton {
          this.borrowViewModel.tryRevokeMaybeAuthenticated(book)
        })
    }
    this.idleButtons.addView(this.buttonCreator.createGetButton {
      this.openLoginDialogIfNecessary(book.account)
      this.borrowViewModel.tryBorrowMaybeAuthenticated(book)
    })
  }

  @UiThread
  private fun onBookStatusHeldInQueue(
    status: BookStatus.Held.HeldInQueue,
    book: Book
  ) {
    this.setVisibilityIfNecessary(this.corrupt, View.GONE)
    this.setVisibilityIfNecessary(this.error, View.GONE)
    this.setVisibilityIfNecessary(this.idle, View.VISIBLE)
    this.setVisibilityIfNecessary(this.progress, View.GONE)

    this.idleButtons.removeAllViews()
    if (status.isRevocable) {
      this.idleButtons.addView(
        this.buttonCreator.createRevokeHoldButton {
          this.borrowViewModel.tryRevokeMaybeAuthenticated(book)
        })
      this.idleButtons.addView(this.buttonCreator.createButtonSizedSpace())
      this.idleButtons.addView(this.buttonCreator.createButtonSizedSpace())
    } else {
      this.idleButtons.addView(
        this.buttonCreator.createCenteredTextForButtons(R.string.catalogHoldCannotCancel)
      )
    }
  }

  @UiThread
  private fun onBookStatusLoanedDownloaded(book: Book) {
    this.setVisibilityIfNecessary(this.corrupt, View.GONE)
    this.setVisibilityIfNecessary(this.error, View.GONE)
    this.setVisibilityIfNecessary(this.idle, View.VISIBLE)
    this.setVisibilityIfNecessary(this.progress, View.GONE)

    this.idleButtons.removeAllViews()

    when (val format = book.findPreferredFormat()) {
      is BookFormat.BookFormatPDF,
      is BookFormat.BookFormatEPUB -> {
        this.idleButtons.addView(this.buttonCreator.createReadButton {
          this.navigation().openViewer(this.context, book, format)
        })
      }
      is BookFormat.BookFormatAudioBook -> {
        this.idleButtons.addView(this.buttonCreator.createListenButton {
          this.navigation().openViewer(this.context, book, format)
        })
      }
      null -> {
        this.idleButtons.addView(this.buttonCreator.createButtonSizedSpace())
      }
    }

    this.idleButtons.addView(this.buttonCreator.createButtonSizedSpace())
    this.idleButtons.addView(this.buttonCreator.createButtonSizedSpace())
  }

  @Suppress("UNUSED_PARAMETER")
  @UiThread
  private fun onBookStatusRevoked(book: BookWithStatus) {
    this.setVisibilityIfNecessary(this.corrupt, View.GONE)
    this.setVisibilityIfNecessary(this.error, View.GONE)
    this.setVisibilityIfNecessary(this.idle, View.VISIBLE)
    this.setVisibilityIfNecessary(this.progress, View.GONE)

    this.idleButtons.removeAllViews()
  }

  @UiThread
  private fun onBookStatusDownloading(
    book: BookWithStatus,
    status: BookStatus.Downloading
  ) {
    this.setVisibilityIfNecessary(this.corrupt, View.GONE)
    this.setVisibilityIfNecessary(this.error, View.GONE)
    this.setVisibilityIfNecessary(this.idle, View.GONE)
    this.setVisibilityIfNecessary(this.progress, View.VISIBLE)

    this.progressText.text = book.book.entry.title
    this.progressProgress.isIndeterminate = false
    this.progressProgress.progress = status.progressPercent.toInt()
  }

  @UiThread
  fun unbind() {
    this.runOnLoginDialogClosed.set(null)
    this.unsubscribeFromBookRegistry()
    this.unsubscribeFromLogin()

    this.setVisibilityIfNecessary(this.corrupt, View.GONE)
    this.setVisibilityIfNecessary(this.error, View.GONE)
    this.setVisibilityIfNecessary(this.idle, View.GONE)
    this.setVisibilityIfNecessary(this.progress, View.GONE)

    this.errorDetails.setOnClickListener(null)
    this.errorDismiss.setOnClickListener(null)
    this.errorRetry.setOnClickListener(null)
    this.idle.setOnClickListener(null)
    this.idleAuthor.text = null
    this.idleButtons.removeAllViews()
    this.idleCover.contentDescription = null
    this.idleCover.setImageDrawable(null)
    this.idleCover.setOnClickListener(null)
    this.idleTitle.setOnClickListener(null)
    this.idleTitle.text = null
    this.progress.setOnClickListener(null)
    this.progressText.setOnClickListener(null)

    this.thumbnailLoading = this.thumbnailLoading?.let { loading ->
      loading.cancel(true)
      null
    }
  }

  private fun unsubscribeFromLogin() {
    this.loginSubscription = this.loginSubscription?.let { disposable ->
      disposable.dispose()
      null
    }
  }

  private fun unsubscribeFromBookRegistry() {
    this.bookSubscription = this.bookSubscription?.let { disposable ->
      disposable.dispose()
      null
    }
  }

  @UiThread
  private fun openLoginDialogIfNecessary(accountID: AccountID) {
    this.uiThread.checkIsUIThread()

    if (this.borrowViewModel.isLoginRequired(accountID)) {
      this.navigation()
        .openSettingsAccount(
          AccountFragmentParameters(
            accountId = accountID,
            closeOnLoginSuccess = true,
            showPleaseLogInTitle = true
          )
        )
    }
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
      attributes = this.collectAttributes(result),
      taskSteps = result.steps
    )
    this.navigation().openErrorPage(errorPageParameters)
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
}
