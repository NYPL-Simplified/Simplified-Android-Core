package org.nypl.simplified.books.core;

import com.io7m.jfunctional.FunctionType;
import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;
import org.nypl.simplified.opds.core.OPDSRFC3339Formatter;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * The given book is currently reserved for the user. This is equivalent to a
 * hold where the current user is at position 0 in the queue.
 */

public final class BookStatusReserved implements BookStatusType
{
  private final BookID               id;
  private final OptionType<Calendar> end_date;

  /**
   * Construct a status value.
   *
   * @param in_id       The book ID
   * @param in_end_date The expiry date
   */

  public BookStatusReserved(
    final BookID in_id,
    final OptionType<Calendar> in_end_date)
  {
    this.id = NullCheck.notNull(in_id);
    this.end_date = NullCheck.notNull(in_end_date);
  }

  /**
   * @return The expiry date of the reservation, if any
   */

  public OptionType<Calendar> getExpiryDate()
  {
    return this.end_date;
  }

  @Override public BookID getID()
  {
    return this.id;
  }

  @Override public BookStatusPriorityOrdering getPriority()
  {
    return BookStatusPriorityOrdering.BOOK_STATUS_RESERVED;
  }

  @Override public <A, E extends Exception> A matchBookStatus(
    final BookStatusMatcherType<A, E> m)
    throws E
  {
    return m.onBookStatusReserved(this);
  }

  @Override public String toString()
  {
    final SimpleDateFormat fmt = OPDSRFC3339Formatter.newDateFormatter();
    final StringBuilder b = new StringBuilder(128);
    b.append("[BookStatusReserved ");
    b.append(this.id);
    b.append(" ");
    b.append(
      this.end_date.map(
        new FunctionType<Calendar, String>()
        {
          @Override public String call(final Calendar et)
          {
            return fmt.format(et.getTime());
          }
        }));
    b.append("]");
    return NullCheck.notNull(b.toString());
  }
}
