package org.nypl.simplified.tests

import com.google.common.util.concurrent.FluentFuture
import com.google.common.util.concurrent.SettableFuture
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
    account: AccountType?,
    id: BookID?,
    acquisition: OPDSAcquisition?,
    entry: OPDSAcquisitionFeedEntry?
  ): FluentFuture<TaskResult<BookStatusDownloadErrorDetails, Unit>> {
    return FluentFuture.from(SettableFuture.create())
  }

  override fun bookBorrowFailedDismiss(
    account: AccountType?,
    id: BookID?
  ) {

  }

  override fun bookDownloadCancel(
    account: AccountType?,
    id: BookID?
  ) {

  }

  override fun bookReport(
    account: AccountType?,
    feed_entry: FeedEntry.FeedEntryOPDS?,
    report_type: String?
  ): FluentFuture<com.io7m.jfunctional.Unit> {
    return FluentFuture.from(SettableFuture.create())
  }

  override fun booksSync(account: AccountType?): FluentFuture<Unit> {
    return FluentFuture.from(SettableFuture.create())
  }

  override fun bookRevoke(
    account: AccountType?,
    book_id: BookID?
  ): FluentFuture<TaskResult<BookStatusRevokeErrorDetails, Unit>> {
    return FluentFuture.from(SettableFuture.create())
  }

  override fun bookDelete(
    account: AccountType?,
    book_id: BookID?
  ): FluentFuture<com.io7m.jfunctional.Unit> {
    return FluentFuture.from(SettableFuture.create())
  }

  override fun bookRevokeFailedDismiss(
    account: AccountType?,
    id: BookID?
  ): FluentFuture<com.io7m.jfunctional.Unit> {
    return FluentFuture.from(SettableFuture.create())
  }

}
