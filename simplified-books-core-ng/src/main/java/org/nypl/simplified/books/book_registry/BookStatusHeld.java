package org.nypl.simplified.books.book_registry;

import com.io7m.jfunctional.FunctionType;
import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;

import org.nypl.simplified.books.book_database.BookID;
import org.nypl.simplified.rfc3339.core.RFC3339Formatter;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * The given book is currently placed on hold.
 */

public final class BookStatusHeld implements BookStatusType {

  private final BookID id;
  private final OptionType<Integer> queue_position;
  private final OptionType<Calendar> start_date;
  private final OptionType<Calendar> end_date;
  private final boolean revocable;

  /**
   * Construct a status value.
   *
   * @param in_id             The book ID
   * @param in_queue_position The current position of the user in the queue
   * @param in_start_date     The start date of the hold
   * @param in_end_date       The approximate date that the book will become
   *                          available
   * @param in_revocable      {@code true} iff the hold is revocable
   */

  public BookStatusHeld(
      final BookID in_id,
      final OptionType<Integer> in_queue_position,
      final OptionType<Calendar> in_start_date,
      final OptionType<Calendar> in_end_date,
      final boolean in_revocable) {

    this.id = NullCheck.notNull(in_id);
    this.queue_position = NullCheck.notNull(in_queue_position);
    this.start_date = NullCheck.notNull(in_start_date);
    this.end_date = NullCheck.notNull(in_end_date);
    this.revocable = in_revocable;
  }

  /**
   * @return The approximate date that the book will become available
   */

  public OptionType<Calendar> getEndDate() {
    return this.end_date;
  }

  @Override
  public BookID getID() {
    return this.id;
  }

  @Override
  public BookStatusPriorityOrdering getPriority() {
    return BookStatusPriorityOrdering.BOOK_STATUS_HELD;
  }

  /**
   * @return The current position of the user in the queue
   */

  public OptionType<Integer> getQueuePosition() {
    return this.queue_position;
  }

  @Override
  public <A, E extends Exception> A matchBookStatus(
      final BookStatusMatcherType<A, E> m)
      throws E {
    return m.onBookStatusHeld(this);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || this.getClass() != o.getClass()) {
      return false;
    }

    final BookStatusHeld that = (BookStatusHeld) o;
    if (this.isRevocable() != that.isRevocable()) {
      return false;
    }
    if (!this.id.equals(that.id)) {
      return false;
    }
    if (!this.queue_position.equals(that.queue_position)) {
      return false;
    }
    if (!this.start_date.equals(that.start_date)) {
      return false;
    }
    return this.end_date.equals(that.end_date);
  }

  @Override
  public int hashCode() {
    int result = this.id.hashCode();
    result = 31 * result + this.queue_position.hashCode();
    result = 31 * result + this.start_date.hashCode();
    result = 31 * result + this.end_date.hashCode();
    result = 31 * result + (this.isRevocable() ? 1 : 0);
    return result;
  }

  @Override
  public String toString() {
    final SimpleDateFormat fmt = RFC3339Formatter.newDateFormatter();
    final StringBuilder b = new StringBuilder(128);
    b.append("[BookStatusHeld ");
    b.append(this.id);
    b.append(" ");
    b.append(this.queue_position);
    b.append(" ");
    b.append(
        this.start_date.map(
            new FunctionType<Calendar, String>() {
              @Override
              public String call(final Calendar et) {
                return fmt.format(et.getTime());
              }
            }));
    b.append(" ");
    b.append(
        this.end_date.map(
            new FunctionType<Calendar, String>() {
              @Override
              public String call(final Calendar et) {
                return fmt.format(et.getTime());
              }
            }));
    b.append(" revocable=");
    b.append(this.revocable);
    b.append("]");
    return NullCheck.notNull(b.toString());
  }

  /**
   * @return {@code true} iff the hold is revocable
   */

  public boolean isRevocable() {
    return this.revocable;
  }
}
