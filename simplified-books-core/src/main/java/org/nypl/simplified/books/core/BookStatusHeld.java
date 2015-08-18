package org.nypl.simplified.books.core;

import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;

/**
 * The given book is currently placed on hold.
 */

public final class BookStatusHeld implements BookStatusType
{
  private final BookID              id;
  private final OptionType<Integer> queue_position;

  /**
   * Construct a status value.
   *
   * @param in_id             The book ID
   * @param in_queue_position The current position of the user in the queue
   */

  public BookStatusHeld(
    final BookID in_id,
    final OptionType<Integer> in_queue_position)
  {
    this.id = NullCheck.notNull(in_id);
    this.queue_position = in_queue_position;
  }

  @Override public BookID getID()
  {
    return this.id;
  }

  @Override public BookStatusPriorityOrdering getPriority()
  {
    return BookStatusPriorityOrdering.BOOK_STATUS_HELD;
  }

  /**
   * @return The current position of the user in the queue
   */

  public OptionType<Integer> getQueuePosition()
  {
    return this.queue_position;
  }

  @Override public <A, E extends Exception> A matchBookStatus(
    final BookStatusMatcherType<A, E> m)
    throws E
  {
    return m.onBookStatusHeld(this);
  }

  @Override public String toString()
  {
    final StringBuilder b = new StringBuilder(128);
    b.append("[BookStatusHeld ");
    b.append(this.id);
    b.append(" ");
    b.append(this.queue_position);
    b.append("]");
    return NullCheck.notNull(b.toString());
  }
}
