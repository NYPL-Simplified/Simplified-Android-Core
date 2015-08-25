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
  private final boolean              returnable;

  /**
   * Construct a status value.
   *
   * @param in_id         The book ID
   * @param in_end_date   The end date of the loan, if any
   * @param in_returnable {@code true} if the book is returnable
   */

  public BookStatusLoaned(
    final BookID in_id,
    final OptionType<Calendar> in_end_date,
    final boolean in_returnable)
  {
    this.id = NullCheck.notNull(in_id);
    this.end_date = NullCheck.notNull(in_end_date);
    this.returnable = in_returnable;
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

  /**
   * @return {@code true} iff the book is returnable.
   */

  public boolean isReturnable()
  {
    return this.returnable;
  }
}
