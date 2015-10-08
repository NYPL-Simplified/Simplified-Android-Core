package org.nypl.simplified.books.core;

import com.io7m.jnull.NullCheck;

import java.net.URI;

/**
 * The given book is revoked, but the server has not yet been notified. A given
 * book is expected to spend very little time in this state, and will not be
 * displayed.
 */

public final class BookStatusRevoked implements BookStatusType
{
  private final BookID id;
  private final URI    revoke;

  /**
   * Construct a status value.
   *
   * @param in_id     The book ID
   * @param in_revoke The revocation URI
   */

  public BookStatusRevoked(
    final BookID in_id,
    final URI in_revoke)
  {
    this.id = NullCheck.notNull(in_id);
    this.revoke = NullCheck.notNull(in_revoke);
  }

  @Override public BookID getID()
  {
    return this.id;
  }

  @Override public BookStatusPriorityOrdering getPriority()
  {
    return BookStatusPriorityOrdering.BOOK_STATUS_LOANED;
  }

  @Override public <A, E extends Exception> A matchBookStatus(
    final BookStatusMatcherType<A, E> m)
    throws E
  {
    return m.onBookStatusRevoked(this);
  }

  @Override public String toString()
  {
    final StringBuilder b = new StringBuilder(128);
    b.append("[BookStatusRevoked ");
    b.append(this.id);
    b.append(" ");
    b.append(this.revoke);
    b.append("]");
    return NullCheck.notNull(b.toString());
  }
}
