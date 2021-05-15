package org.nypl.simplified.ui.catalog

import android.content.res.Resources
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookFormat
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.book_registry.BookStatus
import org.nypl.simplified.books.book_registry.BookStatusEvent
import org.nypl.simplified.books.book_registry.BookWithStatus
import org.nypl.simplified.buildconfig.api.BuildConfigurationServiceType
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.listeners.api.FragmentListenerType
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
import org.nypl.simplified.ui.errorpage.ErrorPageParameters
import org.slf4j.LoggerFactory
import java.net.URI

class CatalogBookDetailViewModel(
  private val resources: Resources,
  private val profilesController: ProfilesControllerType,
  private val bookRegistry: BookRegistryType,
  private val buildConfiguration: BuildConfigurationServiceType,
  private val borrowViewModel: CatalogBorrowViewModel,
  private val parameters: CatalogFragmentBookDetailParameters,
  private val listener: FragmentListenerType<CatalogBookDetailEvent>
) : ViewModel() {

  private val logger =
    LoggerFactory.getLogger(CatalogBookDetailViewModel::class.java)

  private val subscriptions =
    CompositeDisposable(
      this.bookRegistry.bookEvents()
        .filter { event -> event.bookId == this.parameters.bookID }
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(this::onBookStatusEvent)
    )

  private val bookWithStatusMutable: MutableLiveData<BookWithStatus> =
    MutableLiveData(this.createBookWithStatus())

  private val bookWithStatus: BookWithStatus
    get() = bookWithStatusMutable.value!!

  private fun onBookStatusEvent(event: BookStatusEvent) {
    val bookWithStatus = this.createBookWithStatus()
    this.bookWithStatusMutable.value = bookWithStatus

    when (val status = event.statusNow) {
      is BookStatus.DownloadWaitingForExternalAuthentication -> {
        this.listener.post(
          CatalogBookDetailEvent.DownloadWaitingForExternalAuthentication(
            bookID = status.id,
            downloadURI = status.downloadURI
          )
        )
      }
    }
  }

  /*
   * Retrieve the current status of the book, or synthesize a status value based on the
   * OPDS feed entry if the book is not in the registry. The book will only be in the
   * registry if the user has ever tried to borrow it (as per the registry spec).
   */

  private fun createBookWithStatus(): BookWithStatus {
    return this.bookRegistry.bookOrNull(this.parameters.bookID)
      ?: synthesizeBookWithStatus(this.parameters.feedEntry)
  }

  private fun synthesizeBookWithStatus(
    item: FeedEntry.FeedEntryOPDS
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

  override fun onCleared() {
    super.onCleared()
    subscriptions.clear()
  }

  val bookWithStatusLive: LiveData<BookWithStatus>
    get() = bookWithStatusMutable

  val accountProvider = try {
    this.profilesController.profileCurrent()
      .account(this.parameters.feedEntry.accountID)
      .provider
  } catch (e: Exception) {
    this.logger.debug("Couldn't load account provider from profile", e)
    null
  }

  val showDebugBookDetailStatus: Boolean
    get() = this.buildConfiguration.showDebugBookDetailStatus

  /**
   * Determine whether or not a book can be "deleted".
   *
   * A book can be deleted if:
   *
   * * It is loaned, downloaded, and not revocable (because otherwise, a revocation is needed).
   * * It is loanable, but there is a book database entry for it
   * * It is open access but there is a book database entry for it
   */

  val bookCanBeDeleted: Boolean get() {
    return try {
      val book = this.bookWithStatus.book
      val profile = this.profilesController.profileCurrent()
      val account = profile.account(book.account)
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
      this.logger.error("could not determine if the book could be deleted: ", e)
      false
    }
  }

  fun openRelatedFeed(feedRelated: URI) {
    val targetFeed =
      this.resolveFeedFromBook(
        accountID = this.bookWithStatus.book.account,
        title = this.resources.getString(R.string.catalogRelatedBooks),
        uri = feedRelated
      )
    this.listener.post(CatalogBookDetailEvent.OpenFeed(targetFeed))
  }

  /**
   * Resolve a given URI as a remote feed. The URI, if non-absolute, is resolved against
   * the current feed arguments in order to produce new arguments to load another feed. This
   * method is intended to be called from book detail contexts, where there may not be a
   * feed accessible that has unambiguous account ownership information (ownership can be
   * per-book, and feeds can contain a mix of accounts).
   *
   * @param accountID The account ID that owns the book
   * @param title The title of the target feed
   * @param uri The URI of the target feed
   */

  private fun resolveFeedFromBook(
    accountID: AccountID,
    title: String,
    uri: URI
  ): CatalogFeedArguments {
    return when (val arguments = this.parameters.feedArguments) {
      is CatalogFeedArguments.CatalogFeedArgumentsRemote ->
        CatalogFeedArguments.CatalogFeedArgumentsRemote(
          feedURI = arguments.feedURI.resolve(uri).normalize(),
          isSearchResults = false,
          ownership = CatalogFeedOwnership.OwnedByAccount(accountID),
          title = title
        )

      is CatalogFeedArguments.CatalogFeedArgumentsLocalBooks -> {
        CatalogFeedArguments.CatalogFeedArgumentsRemote(
          feedURI = uri.normalize(),
          isSearchResults = false,
          ownership = CatalogFeedOwnership.OwnedByAccount(accountID),
          title = title
        )
      }
    }
  }

  fun dismissBorrowError() {
    this.borrowViewModel.tryDismissBorrowError(
      this.bookWithStatus.book.account,
      this.bookWithStatus.book.id
    )
  }

  fun dismissRevokeError() {
    this.borrowViewModel.tryDismissRevokeError(
      this.bookWithStatus.book.account,
      this.bookWithStatus.book.id
    )
  }

  fun delete() {
    this.borrowViewModel.tryDelete(
      this.bookWithStatus.book.account,
      this.bookWithStatus.book.id
    )
  }

  fun borrowMaybeAuthenticated() {
    this.openLoginDialogIfNecessary()
    this.borrowViewModel.tryBorrowMaybeAuthenticated(
      this.bookWithStatus.book
    )
  }

  fun reserveMaybeAuthenticated() {
    this.openLoginDialogIfNecessary()
    this.borrowViewModel.tryReserveMaybeAuthenticated(
      this.bookWithStatus.book
    )
  }

  fun revokeMaybeAuthenticated() {
    this.openLoginDialogIfNecessary()
    this.borrowViewModel.tryRevokeMaybeAuthenticated(
      this.bookWithStatus.book
    )
  }

  private fun openLoginDialogIfNecessary() {
    val accountID = this.bookWithStatus.book.account
    if (this.borrowViewModel.isLoginRequired(accountID)) {
      this.listener.post(
        CatalogBookDetailEvent.LoginRequired(accountID)
      )
    }
  }

  fun openViewer(format: BookFormat) {
    this.listener.post(
      CatalogBookDetailEvent.OpenViewer(this.bookWithStatus.book, format)
    )
  }

  fun showError(result: TaskResult.Failure<*>) {
      this.logger.debug("showing error: {}", this.bookWithStatus.book.id)

      val errorPageParameters = ErrorPageParameters(
        emailAddress = this.buildConfiguration.supportErrorReportEmailAddress,
        body = "",
        subject = this.buildConfiguration.supportErrorReportSubject,
        attributes = result.attributes.toSortedMap(),
        taskSteps = result.steps
      )
      this.listener.post(
        CatalogBookDetailEvent.OpenErrorPage(errorPageParameters)
      )
  }
}
