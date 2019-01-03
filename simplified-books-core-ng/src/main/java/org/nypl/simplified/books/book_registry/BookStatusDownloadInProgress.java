package org.nypl.simplified.books.book_registry;

import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;

import org.nypl.simplified.books.book_database.BookID;

import java.util.Calendar;

/**
 * The given book is currently downloading.
 */

public final class BookStatusDownloadInProgress implements BookStatusDownloadingType {

  private final long current_total;
  private final long expected_total;
  private final BookID id;
  private final OptionType<Calendar> loan_end_date;

  /**
   * Construct a status value.
   *
   * @param in_id             The book ID
   * @param in_current_total  The current number of bytes downloaded
   * @param in_expected_total The expected total bytes
   * @param in_loan_end_date  The end date of the loan, if any
   */

  public BookStatusDownloadInProgress(
      final BookID in_id,
      final long in_current_total,
      final long in_expected_total,
      final OptionType<Calendar> in_loan_end_date) {

    this.id = NullCheck.notNull(in_id);
    this.current_total = in_current_total;
    this.expected_total = in_expected_total;
    this.loan_end_date = NullCheck.notNull(in_loan_end_date);
  }

  /**
   * @return The current number of downloaded bytes
   */

  public long getCurrentTotalBytes() {
    return this.current_total;
  }

  /**
   * @return The expected total bytes
   */

  public long getExpectedTotalBytes() {
    return this.expected_total;
  }

  @Override
  public BookID getID() {
    return this.id;
  }

  @Override
  public OptionType<Calendar> getLoanExpiryDate() {
    return this.loan_end_date;
  }

  @Override
  public BookStatusPriorityOrdering getPriority() {
    return BookStatusPriorityOrdering.BOOK_STATUS_DOWNLOAD_IN_PROGRESS;
  }

  @Override
  public <A, E extends Exception> A matchBookDownloadingStatus(
      final BookStatusDownloadingMatcherType<A, E> m)
      throws E {
    return m.onBookStatusDownloadInProgress(this);
  }

  @Override
  public <A, E extends Exception> A matchBookLoanedStatus(
      final BookStatusLoanedMatcherType<A, E> m)
      throws E {
    return m.onBookStatusDownloading(this);
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
    b.append("[BookStatusDownloadInProgress ");
    b.append(this.id);
    b.append(" [");
    b.append(this.current_total);
    b.append("/");
    b.append(this.expected_total);
    b.append("]]");
    return NullCheck.notNull(b.toString());
  }
}
