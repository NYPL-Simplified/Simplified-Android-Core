package org.nypl.simplified.books.core;

import java.util.Calendar;

import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;

/**
 * The given book is downloaded and available for reading.
 */

public final class BookStatusDownloaded implements BookStatusDownloadedType
{
  private final BookID               id;
  private final OptionType<Calendar> loan_end_date;

  public BookStatusDownloaded(
    final BookID in_id,
    final OptionType<Calendar> in_loan_end_date)
  {
    this.id = NullCheck.notNull(in_id);
    this.loan_end_date = NullCheck.notNull(in_loan_end_date);
  }

  @Override public BookID getID()
  {
    return this.id;
  }

  @Override public OptionType<Calendar> getLoanExpiryDate()
  {
    return this.loan_end_date;
  }

  @Override public BookStatusPriorityOrdering getPriority()
  {
    return BookStatusPriorityOrdering.BOOK_STATUS_DOWNLOADED;
  }

  @Override public <A, E extends Exception> A matchBookLoanedStatus(
    final BookStatusLoanedMatcherType<A, E> m)
    throws E
  {
    return m.onBookStatusDownloaded(this);
  }

  @Override public <A, E extends Exception> A matchBookStatus(
    final BookStatusMatcherType<A, E> m)
    throws E
  {
    return m.onBookStatusLoanedType(this);
  }

  @Override public String toString()
  {
    final StringBuilder b = new StringBuilder();
    b.append("[BookStatusDownloaded ");
    b.append(this.id);
    b.append("]");
    return NullCheck.notNull(b.toString());
  }
}
