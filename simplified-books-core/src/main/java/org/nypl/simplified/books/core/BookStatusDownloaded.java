package org.nypl.simplified.books.core;

import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;

import java.util.Calendar;

/**
 * The given book is downloaded and available for reading.
 */

public final class BookStatusDownloaded implements BookStatusDownloadedType
{
  private final BookID               id;
  private final OptionType<Calendar> loan_end_date;
  private final boolean              returnable;

  /**
   * Construct a status value.
   *
   * @param in_id            The book ID
   * @param in_loan_end_date The expiry date of the loan, if any
   * @param in_returnable    {@code true} iff the book is returnable
   */

  public BookStatusDownloaded(
    final BookID in_id,
    final OptionType<Calendar> in_loan_end_date,
    final boolean in_returnable)
  {
    this.id = NullCheck.notNull(in_id);
    this.loan_end_date = NullCheck.notNull(in_loan_end_date);
    this.returnable = in_returnable;
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

  @Override public boolean equals(final Object o)
  {
    if (this == o) {
      return true;
    }
    if (o == null || this.getClass() != o.getClass()) {
      return false;
    }

    final BookStatusDownloaded that = (BookStatusDownloaded) o;
    if (this.isReturnable() != that.isReturnable()) {
      return false;
    }
    if (!this.id.equals(that.id)) {
      return false;
    }
    return this.loan_end_date.equals(that.loan_end_date);
  }

  @Override public int hashCode()
  {
    int result = this.id.hashCode();
    result = 31 * result + this.loan_end_date.hashCode();
    result = 31 * result + (this.isReturnable() ? 1 : 0);
    return result;
  }

  @Override public String toString()
  {
    final StringBuilder b = new StringBuilder(64);
    b.append("[BookStatusDownloaded ");
    b.append(this.id);
    b.append(" returnable=");
    b.append(this.returnable);
    b.append("]");
    return NullCheck.notNull(b.toString());
  }

  /**
   * @return {@code true} iff the book is returnable
   */

  public boolean isReturnable()
  {
    return this.returnable;
  }
}
