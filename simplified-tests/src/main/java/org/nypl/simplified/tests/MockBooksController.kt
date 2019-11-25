package org.nypl.simplified.tests

import com.google.common.util.concurrent.FluentFuture
import com.google.common.util.concurrent.SettableFuture
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.book_registry.BookStatusDownloadErrorDetails
import org.nypl.simplified.books.book_registry.BookStatusRevokeErrorDetails
import org.nypl.simplified.books.controller.api.BooksControllerType
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.opds.core.OPDSAcquisition
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.taskrecorder.api.TaskResult

class MockBooksController : BooksControllerType {

  override fun bookBorrow(
    accountID: AccountID,
    bookID: BookID,
    acquisition: OPDSAcquisition,
    entry: OPDSAcquisitionFeedEntry
  ): FluentFuture<TaskResult<BookStatusDownloadErrorDetails, Unit>> {
    return FluentFuture.from(SettableFuture.create())
  }

  override fun bookBorrowWithDefaultAcquisition(
    account: AccountType,
    bookID: BookID,
    entry: OPDSAcquisitionFeedEntry
  ): FluentFuture<TaskResult<BookStatusDownloadErrorDetails, Unit>> {
    return FluentFuture.from(SettableFuture.create())
  }

  override fun bookBorrowWithDefaultAcquisition(
    accountID: AccountID,
    id: BookID,
    entry: OPDSAcquisitionFeedEntry
  ): FluentFuture<TaskResult<BookStatusDownloadErrorDetails, Unit>> {
    return FluentFuture.from(SettableFuture.create())
  }

  override fun bookBorrowFailedDismiss(
    accountID: AccountID,
    bookID: BookID
  ) {

  }

  override fun bookDownloadCancel(
    account: AccountID,
    id: BookID
  ) {

  }

  override fun bookRevoke(
    accountID: AccountID,
    bookId: BookID
  ): FluentFuture<TaskResult<BookStatusRevokeErrorDetails, Unit>> {
    return FluentFuture.from(SettableFuture.create())
  }

  override fun bookRevokeFailedDismiss(
    accountID: AccountID,
    bookID: BookID
  ): FluentFuture<Unit> {
    return FluentFuture.from(SettableFuture.create())
  }

  override fun bookBorrow(
    account: AccountType,
    bookID: BookID,
    acquisition: OPDSAcquisition,
    entry: OPDSAcquisitionFeedEntry
  ): FluentFuture<TaskResult<BookStatusDownloadErrorDetails, Unit>> {
    return FluentFuture.from(SettableFuture.create())
  }

  override fun bookBorrowFailedDismiss(
    account: AccountType,
    bookID: BookID
  ) {

  }

  override fun bookDownloadCancel(
    account: AccountType,
    bookID: BookID
  ) {

  }

  override fun bookReport(
    account: AccountType,
    feedEntry: FeedEntry.FeedEntryOPDS,
    reportType: String
  ): FluentFuture<Unit> {
    return FluentFuture.from(SettableFuture.create())
  }

  override fun booksSync(account: AccountType): FluentFuture<Unit> {
    return FluentFuture.from(SettableFuture.create())
  }

  override fun bookRevoke(
    account: AccountType,
    bookId: BookID
  ): FluentFuture<TaskResult<BookStatusRevokeErrorDetails, Unit>> {
    return FluentFuture.from(SettableFuture.create())
  }

  override fun bookDelete(
    account: AccountType,
    bookId: BookID
  ): FluentFuture<Unit> {
    return FluentFuture.from(SettableFuture.create())
  }

  override fun bookRevokeFailedDismiss(
    account: AccountType,
    bookID: BookID
  ): FluentFuture<Unit> {
    return FluentFuture.from(SettableFuture.create())
  }

}
