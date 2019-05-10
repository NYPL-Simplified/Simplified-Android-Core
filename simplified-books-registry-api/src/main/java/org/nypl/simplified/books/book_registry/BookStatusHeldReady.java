package org.nypl.simplified.books.book_registry;

import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.nypl.simplified.books.api.BookID;

/**
 * The given book is currently reserved for the user. This is equivalent to a
 * hold where the current user is at position 0 in the queue.
 */

public final class BookStatusHeldReady implements BookStatusType
{
  private final BookID id;
  private final OptionType<DateTime> end_date;
  private final boolean              revocable;

  /**
   * Construct a status value.
   *
   * @param in_id        The book ID
   * @param in_end_date  The expiry date
   * @param in_revocable {@code true} iff the hold is revocable
   */

  public BookStatusHeldReady(
    final BookID in_id,
    final OptionType<DateTime> in_end_date,
    final boolean in_revocable)
  {
    this.id = NullCheck.notNull(in_id);
    this.end_date = NullCheck.notNull(in_end_date);
    this.revocable = in_revocable;
  }

  /**
   * @return The expiry date of the reservation, if any
   */

  public OptionType<DateTime> getExpiryDate()
  {
    return this.end_date;
  }

  @Override public BookID getID()
  {
    return this.id;
  }

  @Override public BookStatusPriorityOrdering getPriority()
  {
    return BookStatusPriorityOrdering.BOOK_STATUS_HELD_READY;
  }

  @Override public <A, E extends Exception> A matchBookStatus(
    final BookStatusMatcherType<A, E> m)
    throws E
  {
    return m.onBookStatusHeldReady(this);
  }

  @Override public String toString()
  {
    final DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
    final StringBuilder b = new StringBuilder(128);
    b.append("[BookStatusHeldReady ");
    b.append(this.id);
    b.append(" ");
    b.append(this.end_date.map(fmt::print));
    b.append(" revocable=");
    b.append(this.revocable);
    b.append("]");
    return NullCheck.notNull(b.toString());
  }

  /**
   * @return {@code true} iff the hold is revocable
   */

  public boolean isRevocable()
  {
    return this.revocable;
  }
}
