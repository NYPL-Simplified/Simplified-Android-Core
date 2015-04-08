package org.nypl.simplified.books.core;

import com.io7m.jnull.NullCheck;

/**
 * The given book is being requested but it is not yet known if the book is
 * loaned or not.
 */

public final class BookStatusRequesting implements BookStatusType
{
  private final BookID id;

  public BookStatusRequesting(
    final BookID in_id)
  {
    this.id = NullCheck.notNull(in_id);
  }

  @Override public BookID getID()
  {
    return this.id;
  }

  @Override public <A, E extends Exception> A matchBookStatus(
    final BookStatusMatcherType<A, E> m)
    throws E
  {
    return m.onBookStatusRequesting(this);
  }
}
