package org.nypl.simplified.books.core;

import com.io7m.jnull.NullCheck;

/**
 * The given book is downloaded and available for reading.
 */

public final class BookStatusDone implements BookStatusType
{
  private final BookID id;

  public BookStatusDone(
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
    return m.onBookStatusDone(this);
  }
}
