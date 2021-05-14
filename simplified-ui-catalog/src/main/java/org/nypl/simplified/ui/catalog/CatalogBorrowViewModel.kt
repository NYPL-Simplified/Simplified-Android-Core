package org.nypl.simplified.ui.catalog

import androidx.lifecycle.ViewModel
import io.reactivex.disposables.Disposable
import org.librarysimplified.services.api.ServiceDirectoryType
import org.nypl.simplified.accounts.api.AccountEventLoginStateChanged
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoggedIn
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoggingIn
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoggingInWaitingForExternalAuthentication
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoggingOut
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoginFailed
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLogoutFailed
import org.nypl.simplified.accounts.api.AccountLoginState.AccountNotLoggedIn
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.book_registry.BookRegistryType
import org.nypl.simplified.books.book_registry.BookStatus
import org.nypl.simplified.books.book_registry.BookWithStatus
import org.nypl.simplified.books.controller.api.BooksControllerType
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * A view model that covers the basics of book borrowing. In the future, more of this
 * should be moved into the books controller.
 */

class CatalogBorrowViewModel(
  private val services: ServiceDirectoryType
) : ViewModel() {

  private val logger =
    LoggerFactory.getLogger(CatalogBorrowViewModel::class.java)

  private val booksController: BooksControllerType =
    this.services.requireService(BooksControllerType::class.java)
  private val profilesController: ProfilesControllerType =
    this.services.requireService(ProfilesControllerType::class.java)
  private val bookRegistry: BookRegistryType =
    this.services.requireService(BookRegistryType::class.java)

  private val bookBorrowAttempts: ConcurrentHashMap<BookTarget, Disposable> =
    ConcurrentHashMap()

  private data class BookTarget(
    val accountID: AccountID,
    val bookID: BookID
  )

  override fun onCleared() {
    val copy = this.bookBorrowAttempts.toMap()
    for (entry in copy.entries) {
      this.unsubscribe(entry.key)
    }
    this.bookBorrowAttempts.clear()
  }

  /**
   * @return `true` if a login is required on the current account
   */

  fun isLoginRequired(accountID: AccountID): Boolean {
    return try {
      val account = this.profilesController.profileCurrent().account(accountID)
      val requiresLogin = account.requiresCredentials
      val isNotLoggedIn = account.loginState !is AccountLoggedIn
      requiresLogin && isNotLoggedIn
    } catch (e: Exception) {
      this.logger.error("could not retrieve account: ", e)
      false
    }
  }

  private fun executeAfterLogin(
    accountID: AccountID,
    bookID: BookID,
    runOnSuccess: () -> Unit,
    runOnCancel: () -> Unit
  ) {
    val bookTarget =
      BookTarget(
        accountID = accountID,
        bookID = bookID
      )

    val subscription =
      this.profilesController.accountEvents()
        .ofType(AccountEventLoginStateChanged::class.java)
        .filter { event -> event.accountID == bookTarget.accountID }
        .subscribe { event ->
          when (event.state) {
            is AccountLoggingIn,
            is AccountLoggingInWaitingForExternalAuthentication,
            is AccountLoggingOut -> {
              // Still in progress!
            }

            AccountNotLoggedIn,
            is AccountLogoutFailed,
            is AccountLoginFailed -> {
              this.unsubscribe(bookTarget)
              runOnCancel()
            }

            is AccountLoggedIn -> {
              this.unsubscribe(bookTarget)
              runOnSuccess()
            }
          }
        }

    this.logger.debug("[{}][{}] subscribed", bookTarget.accountID, bookTarget.bookID)
    this.bookBorrowAttempts[bookTarget] = subscription
  }

  private fun unsubscribe(target: BookTarget) {
    this.logger.debug("[{}][{}] unsubscribed", target.accountID, target.bookID)
    this.bookBorrowAttempts.remove(target)?.dispose()
  }

  /*
   * Try borrowing, performing the authentication dialog step if necessary.
   */

  fun tryBorrowMaybeAuthenticated(
    book: Book
  ) {
    this.bookRegistry.updateIfStatusIsMoreImportant(
      BookWithStatus(
        book, BookStatus.RequestingLoan(book.id, "")
      )
    )

    if (!this.isLoginRequired(book.account)) {
      return this.tryBorrowAuthenticated(book)
    }

    this.executeAfterLogin(
      accountID = book.account,
      bookID = book.id,
      runOnSuccess = {
        if (!this.isLoginRequired(book.account)) {
          this.tryBorrowAuthenticated(book)
        } else {
          this.onBorrowAttemptCancelled(book)
        }
      },
      runOnCancel = {
        this.onBorrowAttemptCancelled(book)
      }
    )
  }

  private fun onBorrowAttemptCancelled(book: Book) {
    this.logger.debug("borrow attempt cancelled")
    this.bookRegistry.update(BookWithStatus(book, BookStatus.fromBook(book)))
  }

  /*
   * Try revoking, performing the authentication dialog step if necessary.
   */

  fun tryRevokeMaybeAuthenticated(
    book: Book
  ) {
    this.bookRegistry.updateIfStatusIsMoreImportant(
      BookWithStatus(
        book, BookStatus.RequestingRevoke(book.id)
      )
    )

    if (!this.isLoginRequired(book.account)) {
      return this.tryRevokeAuthenticated(book)
    }

    this.executeAfterLogin(
      accountID = book.account,
      bookID = book.id,
      runOnSuccess = {
        if (!this.isLoginRequired(book.account)) {
          this.tryRevokeAuthenticated(book)
        } else {
          this.onRevokeAttemptCancelled(book)
        }
      },
      runOnCancel = {
        this.onRevokeAttemptCancelled(book)
      }
    )
  }

  private fun onRevokeAttemptCancelled(book: Book) {
    this.logger.debug("revoke attempt cancelled")
    this.bookRegistry.update(BookWithStatus(book, BookStatus.fromBook(book)))
  }

  /*
   * Try reserving, performing the authentication dialog step if necessary.
   */

  fun tryReserveMaybeAuthenticated(
    book: Book
  ) {
    this.bookRegistry.updateIfStatusIsMoreImportant(
      BookWithStatus(
        book, BookStatus.RequestingLoan(book.id, "")
      )
    )

    if (!this.isLoginRequired(book.account)) {
      return this.tryReserveAuthenticated(book)
    }

    this.executeAfterLogin(
      accountID = book.account,
      bookID = book.id,
      runOnSuccess = {
        if (!this.isLoginRequired(book.account)) {
          this.tryReserveAuthenticated(book)
        } else {
          this.onReserveAttemptCancelled(book)
        }
      },
      runOnCancel = {
        this.onReserveAttemptCancelled(book)
      }
    )
  }

  private fun onReserveAttemptCancelled(book: Book) {
    this.logger.debug("reserve attempt cancelled")
    this.bookRegistry.update(BookWithStatus(book, BookStatus.fromBook(book)))
  }

  private fun tryReserveAuthenticated(
    book: Book
  ) {
    this.logger.debug("reserving: {}", book.id)
    this.booksController.bookBorrow(
      accountID = book.account,
      entry = book.entry
    )
  }

  private fun tryRevokeAuthenticated(
    book: Book
  ) {
    this.logger.debug("revoking: {}", book.id)
    this.booksController.bookRevoke(
      accountID = book.account,
      bookId = book.id
    )
  }

  private fun tryBorrowAuthenticated(
    book: Book
  ) {
    this.logger.debug("borrowing: {}", book.id)
    this.booksController.bookBorrow(
      accountID = book.account,
      entry = book.entry
    )
  }

  fun tryDismissBorrowError(
    accountID: AccountID,
    bookID: BookID
  ) {
    this.booksController.bookBorrowFailedDismiss(accountID, bookID)
  }

  fun tryDismissRevokeError(
    accountID: AccountID,
    bookID: BookID
  ) {
    this.logger.debug("dismissing revoke error: {}", bookID)
    this.booksController.bookRevokeFailedDismiss(accountID, bookID)
  }

  fun tryDelete(
    accountID: AccountID,
    bookID: BookID
  ) {
    this.logger.debug("deleting: {}", bookID)
    this.booksController.bookDelete(accountID, bookID)
  }
}
