package org.nypl.simplified.books.core;

import com.io7m.jnull.NullCheck;

/**
 * A status value indicating that a book can be loaned.
 */

public final class BookStatusLoanable implements BookStatusType
{
  private final BookID id;

  /**
   * Construct a status value.
   *
   * @param in_id The book ID
   */

  public BookStatusLoanable(
    final BookID in_id)
  {
    this.id = NullCheck.notNull(in_id);
  }

  @Override public BookID getID()
  {
    return this.id;
  }

  @Override public BookStatusPriorityOrdering getPriority()
  {
    return BookStatusPriorityOrdering.BOOK_STATUS_LOANABLE;
  }

  @Override public <A, E extends Exception> A matchBookStatus(
    final BookStatusMatcherType<A, E> m)
    throws E
  {
    return m.onBookStatusLoanable(this);
  }

  @Override public String toString()
  {
    final StringBuilder b = new StringBuilder(128);
    b.append("BookStatusLoanable ");
    b.append(this.id);
    b.append("]");
    return NullCheck.notNull(b.toString());
  }
}
