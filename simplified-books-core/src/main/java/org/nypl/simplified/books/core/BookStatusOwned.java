package org.nypl.simplified.books.core;

import com.io7m.jnull.NullCheck;

/**
 * The given book is owned/loaned but is not downloaded and is therefore not
 * ready for reading.
 */

public final class BookStatusOwned implements BookStatusType
{
  private final BookID id;

  public BookStatusOwned(
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
    return m.onBookStatusOwned(this);
  }
}
