package org.nypl.simplified.books.core;

import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;

/**
 * The given book could not be revoked (hold cancelled, loan returned, etc).
 */

public final class BookStatusRevokeFailed implements BookStatusType
{
  private final OptionType<Throwable> error;
  private final BookID                id;

  /**
   * Construct a status value.
   *
   * @param in_id            The book ID
   * @param x                The exception raised, if any
   */

  public BookStatusRevokeFailed(
    final BookID in_id,
    final OptionType<Throwable> x)
  {
    this.id = NullCheck.notNull(in_id);
    this.error = NullCheck.notNull(x);
  }

  @Override public BookID getID()
  {
    return this.id;
  }

  /**
   * @return error
   */
  public OptionType<Throwable> getError()
  {
    return this.error;
  }

  @Override public <A, E extends Exception> A matchBookStatus(
    final BookStatusMatcherType<A, E> m)
    throws E
  {
    return m.onBookStatusRevokeFailed(this);
  }

  @Override public String toString()
  {
    final StringBuilder b = new StringBuilder(128);
    b.append("[BookStatusRevokeFailed ");
    b.append(this.id);
    b.append(" ");
    b.append(this.error);
    b.append("]");
    return NullCheck.notNull(b.toString());
  }

  @Override public BookStatusPriorityOrdering getPriority()
  {
    return BookStatusPriorityOrdering.BOOK_STATUS_REVOKE_FAILED;
  }
}
