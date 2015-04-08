package org.nypl.simplified.books.core;

import com.io7m.jnull.NullCheck;

/**
 * The given book is owned/loaned but is not downloaded and is therefore not
 * ready for reading.
 */

public final class BookStatusLoaned implements BookStatusLoanedType
{
  private final BookID id;

  public BookStatusLoaned(
    final BookID in_id)
  {
    this.id = NullCheck.notNull(in_id);
  }

  @Override public BookID getID()
  {
    return this.id;
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
    final StringBuilder b = new StringBuilder();
    b.append("[BookStatusLoaned ");
    b.append(this.id);
    b.append("]");
    return NullCheck.notNull(b.toString());
  }
}
