package org.nypl.simplified.books.controller.api

import com.google.common.util.concurrent.FluentFuture
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.taskrecorder.api.TaskResult

/**
 * The books controller.
 */

interface BooksControllerType {

  /**
   * Attempt to borrow the given book.
   *
   * @param accountID The account that will receive the book
   * @param entry The OPDS feed entry for the book
   */

  fun bookBorrow(
    accountID: AccountID,
    entry: OPDSAcquisitionFeedEntry
  ): FluentFuture<TaskResult<*>>

  /**
   * Dismiss a failed book borrowing.
   *
   * @param account The account that failed to receive the book
   * @param id The ID of the book
   */

  fun bookBorrowFailedDismiss(
    account: AccountType,
    bookID: BookID
  )

  /**
   * Dismiss a failed book borrowing.
   *
   * @param accountID The account that failed to receive the book
   * @param bookID The ID of the book
   */

  fun bookBorrowFailedDismiss(
    accountID: AccountID,
    bookID: BookID
  )

  /**
   * Cancel a book download.
   *
   * @param account The account that would be receiving the book
   * @param bookID The ID of the book
   */

  fun bookDownloadCancel(
    account: AccountType,
    bookID: BookID
  )

  /**
   * Cancel a book download.
   *
   * @param accountID The account that would be receiving the book
   * @param bookID The ID of the book
   */

  fun bookDownloadCancel(
    accountID: AccountID,
    bookID: BookID
  )

  /**
   * Submit a problem report for a book
   *
   * @param account The account that owns the book
   * @param feedEntry Feed entry, used to get the URI to submit to
   * @param reportType Type of report to submit
   */

  fun bookReport(
    account: AccountType,
    feedEntry: FeedEntry.FeedEntryOPDS,
    reportType: String
  ): FluentFuture<Unit>

  /**
   * Sync all books for the given account.
   *
   * @param account The account
   */

  fun booksSync(
    account: AccountType
  ): FluentFuture<Unit>

  /**
   * Revoke the given book.
   *
   * @param account The account
   * @param bookId The ID of the book
   */

  fun bookRevoke(
    account: AccountType,
    bookId: BookID
  ): FluentFuture<TaskResult<Unit>>

  /**
   * Revoke the given book.
   *
   * @param accountID The account
   * @param bookId The ID of the book
   */

  fun bookRevoke(
    accountID: AccountID,
    bookId: BookID
  ): FluentFuture<TaskResult<Unit>>

  /**
   * Delete the given book.
   *
   * @param account The account
   * @param bookId The ID of the book
   */

  fun bookDelete(
    account: AccountType,
    bookId: BookID
  ): FluentFuture<Unit>

  /**
   * Delete the given book.
   *
   * @param account The account
   * @param bookId The ID of the book
   */

  fun bookDelete(
    account: AccountID,
    bookId: BookID
  ): FluentFuture<Unit>

  /**
   * Dismiss a failed book revocation.
   *
   * @param account The account that failed to revoke the book
   * @param id The ID of the book
   */

  fun bookRevokeFailedDismiss(
    account: AccountType,
    bookID: BookID
  ): FluentFuture<Unit>

  /**
   * Dismiss a failed book revocation.
   *
   * @param accountID The account that failed to revoke the book
   * @param id The ID of the book
   */

  fun bookRevokeFailedDismiss(
    accountID: AccountID,
    bookID: BookID
  ): FluentFuture<Unit>
}
