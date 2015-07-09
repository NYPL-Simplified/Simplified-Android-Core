package org.nypl.simplified.books.core;

import com.io7m.jnull.NullCheck;

/**
 * The given book is currently placed on hold.
 */

public final class BookStatusHeld implements BookStatusType
{
  private final BookID id;

  public BookStatusHeld(
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
    return BookStatusPriorityOrdering.BOOK_STATUS_HELD;
  }

  @Override public <A, E extends Exception> A matchBookStatus(
    final BookStatusMatcherType<A, E> m)
    throws E
  {
    return m.onBookStatusHeld(this);
  }

  @Override public String toString()
  {
    final StringBuilder b = new StringBuilder();
    b.append("[BookStatusHeld ");
    b.append(this.id);
    b.append("]");
    return NullCheck.notNull(b.toString());
  }
}
