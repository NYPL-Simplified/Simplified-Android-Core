package org.nypl.simplified.books.controller.api;

import com.google.common.util.concurrent.ListenableFuture;
import com.io7m.jfunctional.Unit;

import org.nypl.simplified.accounts.database.api.AccountType;
import org.nypl.simplified.books.api.BookID;
import org.nypl.simplified.feeds.api.FeedEntry;
import org.nypl.simplified.opds.core.OPDSAcquisition;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;

/**
 * The books controller.
 */

public interface BooksControllerType {

  /**
   * Attempt to borrow the given book.
   *
   * @param account     The account that will receive the book
   * @param id          The ID of the book
   * @param acquisition The acquisition entry for the book
   * @param entry       The OPDS feed entry for the book
   */

  void bookBorrow(
    AccountType account,
    BookID id,
    OPDSAcquisition acquisition,
    OPDSAcquisitionFeedEntry entry);

  /**
   * Dismiss a failed book borrowing.
   *
   * @param account The account that failed to receive the book
   * @param id      The ID of the book
   */

  void bookBorrowFailedDismiss(
    AccountType account,
    BookID id);

  /**
   * Cancel a book download.
   *
   * @param account The account that would be receiving the book
   * @param id      The ID of the book
   */

  void bookDownloadCancel(
    AccountType account,
    BookID id);

  /**
   * Submit a problem report for a book
   *
   * @param account The account that owns the book
   * @param feed_entry  Feed entry, used to get the URI to submit to
   * @param report_type Type of report to submit
   */

  ListenableFuture<Unit> bookReport(
    final AccountType account,
    final FeedEntry.FeedEntryOPDS feed_entry,
    final String report_type);

  /**
   * Sync all books for the given account.
   *
   * @param account The account
   */

  ListenableFuture<Unit> booksSync(
    AccountType account);

  /**
   * Revoke the given book.
   *
   * @param account The account
   * @param book_id The ID of the book
   */

  ListenableFuture<Unit> bookRevoke(
    AccountType account,
    BookID book_id);

  /**
   * Delete the given book.
   *
   * @param account The account
   * @param book_id The ID of the book
   */

  ListenableFuture<Unit> bookDelete(
    AccountType account,
    BookID book_id);

  /**
   * Dismiss a failed book revocation.
   *
   * @param account The account that failed to revoke the book
   * @param id      The ID of the book
   */

  ListenableFuture<Unit> bookRevokeFailedDismiss(
    AccountType account,
    BookID id);
}
