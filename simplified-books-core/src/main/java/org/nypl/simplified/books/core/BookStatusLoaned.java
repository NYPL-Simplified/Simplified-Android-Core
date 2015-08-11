package org.nypl.simplified.books.core;

import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;

import java.util.Calendar;

/**
 * The given book is owned/loaned but is not downloaded and is therefore not
 * ready for reading.
 */

public final class BookStatusLoaned implements BookStatusLoanedType
{
  private final OptionType<Calendar> end_date;
  private final BookID               id;

  /**
   * Construct a status value.
   *
   * @param in_id       The book ID
   * @param in_end_date The end date of the loan, if any
   */

  public BookStatusLoaned(
    final BookID in_id,
    final OptionType<Calendar> in_end_date)
  {
    this.id = NullCheck.notNull(in_id);
    this.end_date = NullCheck.notNull(in_end_date);
  }

  @Override public BookID getID()
  {
    return this.id;
  }

  @Override public OptionType<Calendar> getLoanExpiryDate()
  {
    return this.end_date;
  }

  @Override public BookStatusPriorityOrdering getPriority()
  {
    return BookStatusPriorityOrdering.BOOK_STATUS_LOANED;
  }

  @Override public <A, E extends Exception> A matchBookLoanedStatus(
    final BookStatusLoanedMatcherType<A, E> m)
    throws E
  {
    return m.onBookStatusLoaned(this);
  }

  @Override public <A, E extends Exception> A matchBookStatus(
    final BookStatusMatcherType<A, E> m)
    throws E
  {
    return m.onBookStatusLoanedType(this);
  }

  @Override public String toString()
  {
    final StringBuilder b = new StringBuilder(128);
    b.append("[BookStatusLoaned ");
    b.append(this.id);
    b.append("]");
    return NullCheck.notNull(b.toString());
  }
}
