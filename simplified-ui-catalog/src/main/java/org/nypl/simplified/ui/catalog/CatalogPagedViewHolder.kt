package org.nypl.simplified.ui.catalog

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.UiThread
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.google.common.base.Preconditions
import com.google.common.util.concurrent.FluentFuture
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import org.nypl.simplified.accounts.api.AccountLoginState
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookFormat
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.book_database.api.BookFormats
import org.nypl.simplified.books.book_registry.BookRegistryReadableType
import org.nypl.simplified.books.book_registry.BookStatus
import org.nypl.simplified.books.book_registry.BookStatusEvent
import org.nypl.simplified.books.book_registry.BookWithStatus
import org.nypl.simplified.books.covers.BookCoverProviderType
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.feeds.api.FeedEntry.FeedEntryCorrupt
import org.nypl.simplified.feeds.api.FeedEntry.FeedEntryOPDS
import org.nypl.simplified.futures.FluentFutureExtensions.map
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.ui.thread.api.UIThreadServiceType
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicReference

/**
 * A view holder for a single cell in an infinitely-scrolling feed.
 */

class CatalogPagedViewHolder(
  private val bookCovers: BookCoverProviderType,
  private val bookRegistry: BookRegistryReadableType,
  private val buttonCreator: CatalogButtons,
  private val compositeDisposable: CompositeDisposable,
  private val context: Context,
  private val fragmentManager: FragmentManager,
  private val loginViewModel: CatalogLoginViewModel,
  private val onBookSelected: (FeedEntryOPDS) -> Unit,
  private val parent: View,
  private val profilesController: ProfilesControllerType,
  private val uiThread: UIThreadServiceType
) : RecyclerView.ViewHolder(parent) {

  private val logger =
    LoggerFactory.getLogger(CatalogPagedViewHolder::class.java)

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
  private val idleAuthor =
    this.idle.findViewById<TextView>(R.id.bookCellIdleAuthor)!!
  private val idleButtons =
    this.idle.findViewById<ViewGroup>(R.id.bookCellIdleButtons)!!

  private val progressProgress =
    this.parent.findViewById<ProgressBar>(R.id.bookCellInProgressBar)!!
  private val progressText =
    this.parent.findViewById<TextView>(R.id.bookCellInProgressTitle)!!

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

        val newLoginSubscription =
          this.loginViewModel.loginDialogCompleted.subscribe {
            this.uiThread.runOnUIThread(this::onDialogClosed)
          }

        this.bookSubscription = newBookSubscription
        this.loginSubscription = newLoginSubscription
        this.compositeDisposable.add(newBookSubscription)
        this.compositeDisposable.add(newLoginSubscription)

        this.onFeedEntryOPDSUI(item)

        /*
         * Retrieve the current status of the book, or synthesize a status value based on the
         * OPDS feed entry if the book is not in the registry. The book will only be in the
         * registry if the user has ever tried to borrow it (as per the registry spec).
         */

        val status =
          this.bookRegistry.bookOrNull(item.bookID)
            ?: run {
              val book = Book(
                id = item.bookID,
                account = this.profilesController.profileAccountCurrent().id,
                cover = null,
                thumbnail = null,
                entry = item.feedEntry,
                formats = listOf()
              )
              BookWithStatus(book, BookStatus.fromBook(book))
            }

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

    this.setVisibilityIfNecessary(this.corrupt, View.INVISIBLE)
    this.setVisibilityIfNecessary(this.error, View.INVISIBLE)
    this.setVisibilityIfNecessary(this.idle, View.VISIBLE)
    this.setVisibilityIfNecessary(this.progress, View.INVISIBLE)

    this.setVisibilityIfNecessary(this.idleCover, View.INVISIBLE)
    this.idleCover.setImageDrawable(null)
    this.idleCover.contentDescription =
      CatalogBookAccessibilityStrings.coverDescription(this.context.resources, item)

    this.setVisibilityIfNecessary(this.idleProgress, View.VISIBLE)
    this.idleTitle.text = item.feedEntry.title
    this.idleAuthor.text = item.feedEntry.authorsCommaSeparated

    this.thumbnailLoading =
      this.bookCovers.loadThumbnailInto(
        item,
        this.idleCover,
        this.idleCover.layoutParams.width,
        this.idleCover.layoutParams.height
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
    this.uiThread.runOnUIThread {
      this.onBookChangedUI(event)
      this.checkSomethingIsVisible()
    }
  }

  private fun checkSomethingIsVisible() {
    Preconditions.checkState(
      this.idle.visibility == View.VISIBLE
        || this.progress.visibility == View.VISIBLE
        || this.corrupt.visibility == View.VISIBLE
        || this.error.visibility == View.VISIBLE,
      "Something must be visible!")
  }

  @UiThread
  private fun onBookChangedUI(event: BookStatusEvent) {
    this.uiThread.checkIsUIThread()

    val book = this.bookRegistry.bookOrNull(event.book())
    val status = book?.status
    if (status == null) {
      val previousEntry = this.feedEntry
      if (previousEntry != null) {
        this.bindTo(previousEntry)
      } else {
        // XXX: Not clear what we can do here. Is this code even reachable?
      }
      return
    }

    this.onFeedEntryOPDSUI(FeedEntryOPDS(book.book.entry))
    return this.onBookWithStatus(book)
  }

  @UiThread
  private fun onBookWithStatus(book: BookWithStatus) {
    return when (val status = book.status) {
      is BookStatus.Held.HeldInQueue ->
        this.onBookStatusHeldInQueue(status, book)
      is BookStatus.Held.HeldReady ->
        this.onBookStatusHeldReady(status, book)
      is BookStatus.Holdable ->
        this.onBookStatusHoldable(book)
      is BookStatus.Loanable ->
        this.onBookStatusLoanable(book)
      is BookStatus.Loaned.LoanedNotDownloaded ->
        this.onBookStatusLoanedNotDownloaded(book)
      is BookStatus.Loaned.LoanedDownloaded ->
        this.onBookStatusLoanedDownloaded(book)
      is BookStatus.Revoked ->
        this.onBookStatusRevoked(book)
      is BookStatus.FailedRevoke ->
        this.onBookStatusFailedRevoke(book)
      is BookStatus.FailedDownload ->
        this.onBookStatusFailedDownload(book)
      is BookStatus.FailedLoan ->
        this.onBookStatusFailedLoan(book)

      is BookStatus.RequestingRevoke,
      is BookStatus.RequestingLoan,
      is BookStatus.RequestingDownload -> {
        this.setVisibilityIfNecessary(this.corrupt, View.INVISIBLE)
        this.setVisibilityIfNecessary(this.error, View.INVISIBLE)
        this.setVisibilityIfNecessary(this.idle, View.INVISIBLE)
        this.setVisibilityIfNecessary(this.progress, View.VISIBLE)

        this.progressText.text = book.book.entry.title
        this.progressProgress.isIndeterminate = true
      }

      is BookStatus.Downloading ->
        this.onBookStatusDownloading(book, status)
    }
  }

  @UiThread
  private fun onBookStatusFailedRevoke(book: BookWithStatus) {
    this.setVisibilityIfNecessary(this.corrupt, View.INVISIBLE)
    this.setVisibilityIfNecessary(this.error, View.VISIBLE)
    this.setVisibilityIfNecessary(this.idle, View.INVISIBLE)
    this.setVisibilityIfNecessary(this.progress, View.INVISIBLE)

    this.errorDismiss.setOnClickListener {
      this.tryDismissError(book.book.id)
    }
    this.errorDetails.setOnClickListener {
      this.tryShowError(book.book.id)
    }
    this.errorRetry.setOnClickListener {
      this.tryRevokeMaybeAuthenticated(this.errorRetry, book.book.id)
    }
  }

  @UiThread
  private fun onBookStatusFailedDownload(book: BookWithStatus) {
    this.setVisibilityIfNecessary(this.corrupt, View.INVISIBLE)
    this.setVisibilityIfNecessary(this.error, View.VISIBLE)
    this.setVisibilityIfNecessary(this.idle, View.INVISIBLE)
    this.setVisibilityIfNecessary(this.progress, View.INVISIBLE)

    this.errorDismiss.setOnClickListener {
      this.tryDismissError(book.book.id)
    }
    this.errorDetails.setOnClickListener {
      this.tryShowError(book.book.id)
    }
    this.errorRetry.setOnClickListener {
      this.tryBorrowMaybeAuthenticated(this.errorRetry, book.book.id)
    }
  }

  @UiThread
  private fun onBookStatusFailedLoan(book: BookWithStatus) {
    this.setVisibilityIfNecessary(this.corrupt, View.INVISIBLE)
    this.setVisibilityIfNecessary(this.error, View.VISIBLE)
    this.setVisibilityIfNecessary(this.idle, View.INVISIBLE)
    this.setVisibilityIfNecessary(this.progress, View.INVISIBLE)

    this.errorDismiss.setOnClickListener {
      this.tryDismissError(book.book.id)
    }
    this.errorDetails.setOnClickListener {
      this.tryShowError(book.book.id)
    }
    this.errorRetry.setOnClickListener {
      this.tryBorrowMaybeAuthenticated(this.errorRetry, book.book.id)
    }
  }

  @UiThread
  private fun onBookStatusLoanedNotDownloaded(book: BookWithStatus) {
    this.setVisibilityIfNecessary(this.corrupt, View.INVISIBLE)
    this.setVisibilityIfNecessary(this.error, View.INVISIBLE)
    this.setVisibilityIfNecessary(this.idle, View.VISIBLE)
    this.setVisibilityIfNecessary(this.progress, View.INVISIBLE)

    this.idleButtons.removeAllViews()
    this.idleButtons.addView(this.buttonCreator.createDownloadButton { button ->
      this.tryBorrowMaybeAuthenticated(button, book.book.id)
    })
  }

  @UiThread
  private fun onBookStatusLoanable(book: BookWithStatus) {
    this.setVisibilityIfNecessary(this.corrupt, View.INVISIBLE)
    this.setVisibilityIfNecessary(this.error, View.INVISIBLE)
    this.setVisibilityIfNecessary(this.idle, View.VISIBLE)
    this.setVisibilityIfNecessary(this.progress, View.INVISIBLE)

    this.idleButtons.removeAllViews()
    this.idleButtons.addView(this.buttonCreator.createGetButton { button ->
      this.tryBorrowMaybeAuthenticated(button, book.book.id)
    })
  }

  @UiThread
  private fun onBookStatusHoldable(book: BookWithStatus) {
    this.setVisibilityIfNecessary(this.corrupt, View.INVISIBLE)
    this.setVisibilityIfNecessary(this.error, View.INVISIBLE)
    this.setVisibilityIfNecessary(this.idle, View.VISIBLE)
    this.setVisibilityIfNecessary(this.progress, View.INVISIBLE)

    this.idleButtons.removeAllViews()
    this.idleButtons.addView(this.buttonCreator.createReserveButton { button ->
      this.tryReserveMaybeAuthenticated(button, book.book.id)
    })
  }

  @UiThread
  private fun onBookStatusHeldReady(
    status: BookStatus.Held.HeldReady,
    book: BookWithStatus
  ) {
    this.setVisibilityIfNecessary(this.corrupt, View.INVISIBLE)
    this.setVisibilityIfNecessary(this.error, View.INVISIBLE)
    this.setVisibilityIfNecessary(this.idle, View.VISIBLE)
    this.setVisibilityIfNecessary(this.progress, View.INVISIBLE)

    if (status.isRevocable) {
      this.idleButtons.addView(
        this.buttonCreator.createRevokeHoldButton { button ->
          this.tryRevokeMaybeAuthenticated(button, book.book.id)
        })
    }
    this.idleButtons.addView(this.buttonCreator.createGetButton { button ->
      this.tryBorrowMaybeAuthenticated(button, book.book.id)
    })
  }

  @UiThread
  private fun onBookStatusHeldInQueue(
    status: BookStatus.Held.HeldInQueue,
    book: BookWithStatus
  ) {
    this.setVisibilityIfNecessary(this.corrupt, View.INVISIBLE)
    this.setVisibilityIfNecessary(this.error, View.INVISIBLE)
    this.setVisibilityIfNecessary(this.idle, View.VISIBLE)
    this.setVisibilityIfNecessary(this.progress, View.INVISIBLE)

    this.idleButtons.removeAllViews()
    if (status.isRevocable) {
      this.idleButtons.addView(
        this.buttonCreator.createRevokeHoldButton { button ->
          this.tryRevokeMaybeAuthenticated(button, book.book.id)
        })
    } else {
      this.idleButtons.addView(
        this.buttonCreator.createCenteredTextForButtons(R.string.catalogHoldCannotCancel))
    }
  }

  @UiThread
  private fun onBookStatusLoanedDownloaded(book: BookWithStatus) {
    this.setVisibilityIfNecessary(this.corrupt, View.INVISIBLE)
    this.setVisibilityIfNecessary(this.error, View.INVISIBLE)
    this.setVisibilityIfNecessary(this.idle, View.VISIBLE)
    this.setVisibilityIfNecessary(this.progress, View.INVISIBLE)

    this.idleButtons.removeAllViews()

    when (val format = book.book.findPreferredFormat()) {
      is BookFormat.BookFormatEPUB -> {
        this.idleButtons.addView(this.buttonCreator.createReadButton {

        })
      }
      is BookFormat.BookFormatAudioBook -> {
        this.idleButtons.addView(this.buttonCreator.createListenButton {

        })
      }
      is BookFormat.BookFormatPDF -> {
        this.idleButtons.addView(this.buttonCreator.createReadButton {

        })
      }
      null -> {

      }
    }
  }

  @UiThread
  private fun onBookStatusRevoked(book: BookWithStatus) {
    this.setVisibilityIfNecessary(this.corrupt, View.INVISIBLE)
    this.setVisibilityIfNecessary(this.error, View.INVISIBLE)
    this.setVisibilityIfNecessary(this.idle, View.VISIBLE)
    this.setVisibilityIfNecessary(this.progress, View.INVISIBLE)

    this.idleButtons.removeAllViews()
  }

  @UiThread
  private fun onBookStatusDownloading(
    book: BookWithStatus,
    status: BookStatus.Downloading
  ) {
    this.setVisibilityIfNecessary(this.corrupt, View.INVISIBLE)
    this.setVisibilityIfNecessary(this.error, View.INVISIBLE)
    this.setVisibilityIfNecessary(this.idle, View.INVISIBLE)
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

    this.setVisibilityIfNecessary(this.corrupt, View.INVISIBLE)
    this.setVisibilityIfNecessary(this.error, View.INVISIBLE)
    this.setVisibilityIfNecessary(this.idle, View.INVISIBLE)
    this.setVisibilityIfNecessary(this.progress, View.INVISIBLE)

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
  fun onDialogClosed() {
    this.uiThread.checkIsUIThread()
    this.runOnLoginDialogClosed.getAndSet(null)?.invoke()
  }

  /**
   * Open a login dialog. The given `execute` callback will be executed when the dialog is
   * closed.
   *
   * @see [runOnLoginDialogClosed]
   */

  private fun openLoginDialogAndThen(execute: () -> Unit) {
    try {
      val dialogParameters =
        CatalogFragmentLoginDialogParameters(
          this.profilesController.profileAccountCurrent().id)
      val dialog = CatalogFragmentLoginDialog.create(dialogParameters)
      dialog.show(this.fragmentManager, "LOGIN")
      this.runOnLoginDialogClosed.set(execute)
    } catch (e: Exception) {
      this.logger.error("could not open login dialog: ", e)
    }
  }

  /**
   * @return `true` if a login is required on the current account
   */

  private fun isLoginRequired(): Boolean {
    return try {
      val account = this.profilesController.profileAccountCurrent()
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
    bookID: BookID
  ) {
    button.isEnabled = false

    if (!this.isLoginRequired()) {
      this.tryBorrowAuthenticated(bookID)
      return
    }

    this.openLoginDialogAndThen {
      if (!this.isLoginRequired()) {
        this.tryBorrowAuthenticated(bookID)
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
    bookID: BookID
  ) {
    button.isEnabled = false

    if (!this.isLoginRequired()) {
      this.tryRevokeAuthenticated(bookID)
      return
    }

    this.openLoginDialogAndThen {
      if (!this.isLoginRequired()) {
        this.tryRevokeAuthenticated(bookID)
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
    bookID: BookID
  ) {
    button.isEnabled = false

    if (!this.isLoginRequired()) {
      this.tryReserveAuthenticated(bookID)
      return
    }

    this.openLoginDialogAndThen {
      if (!this.isLoginRequired()) {
        this.tryReserveAuthenticated(bookID)
      } else {
        this.logger.debug("authentication did not complete")
        button.isEnabled = true
      }
    }
  }

  private fun tryReserveAuthenticated(bookID: BookID) {
    this.logger.debug("reserving: {}", bookID)
  }

  private fun tryRevokeAuthenticated(bookID: BookID) {
    this.logger.debug("revoking: {}", bookID)
  }

  private fun tryBorrowAuthenticated(bookID: BookID) {
    this.logger.debug("borrowing: {}", bookID)
  }

  private fun tryShowError(bookID: BookID) {
    this.logger.debug("showing error: {}", bookID)
  }

  private fun tryDismissError(bookID: BookID) {
    this.logger.debug("dismissing error: {}", bookID)
  }

  private fun tryCancelDownload(bookID: BookID) {
    this.logger.debug("cancelling: {}", bookID)
  }
}