package org.nypl.simplified.books.book_registry;

import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;

import org.joda.time.DateTime;
import org.nypl.simplified.books.api.BookID;

/**
 * The given book is being requested for download, but the download has not
 * actually started yet.
 */

public final class BookStatusRequestingDownload implements BookStatusLoanedType {

  private final BookID id;
  private final OptionType<DateTime> loan_end_date;

  /**
   * Construct the book status.
   *
   * @param in_id            The book ID
   * @param in_loan_end_date The end date of the loan, if any
   */

  public BookStatusRequestingDownload(
      final BookID in_id,
      final OptionType<DateTime> in_loan_end_date) {

    this.id = NullCheck.notNull(in_id);
    this.loan_end_date = NullCheck.notNull(in_loan_end_date);
  }

  @Override
  public BookID getID() {
    return this.id;
  }

  @Override
  public <A, E extends Exception> A matchBookLoanedStatus(
      final BookStatusLoanedMatcherType<A, E> m)
      throws E {
    return m.onBookStatusRequestingDownload(this);
  }

  @Override
  public <A, E extends Exception> A matchBookStatus(
      final BookStatusMatcherType<A, E> m)
      throws E {
    return m.onBookStatusLoanedType(this);
  }

  @Override
  public String toString() {
    final StringBuilder b = new StringBuilder(128);
    b.append("[BookStatusRequestingDownload ");
    b.append(this.id);
    b.append("]");
    return NullCheck.notNull(b.toString());
  }

  @Override
  public OptionType<DateTime> getLoanExpiryDate() {
    return this.loan_end_date;
  }

  @Override
  public BookStatusPriorityOrdering getPriority() {
    return BookStatusPriorityOrdering.BOOK_STATUS_DOWNLOAD_REQUESTING;
  }
}
