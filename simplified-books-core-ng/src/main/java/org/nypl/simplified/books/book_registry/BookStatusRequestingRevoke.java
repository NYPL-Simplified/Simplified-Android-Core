package org.nypl.simplified.books.book_registry;

import com.io7m.jnull.NullCheck;

import org.nypl.simplified.books.book_database.BookID;

/**
 * The given book is being returned.
 */

public final class BookStatusRequestingRevoke implements BookStatusType {
  private final BookID id;

  /**
   * Construct the book status.
   *
   * @param in_id The book ID
   */

  public BookStatusRequestingRevoke(
      final BookID in_id) {
    this.id = NullCheck.notNull(in_id);
  }

  @Override
  public BookID getID() {
    return this.id;
  }

  @Override
  public <A, E extends Exception> A matchBookStatus(
      final BookStatusMatcherType<A, E> m)
      throws E {
    return m.onBookStatusRequestingRevoke(this);
  }

  @Override
  public String toString() {
    final StringBuilder b = new StringBuilder(128);
    b.append("[BookStatusRequestingRevoke ");
    b.append(this.id);
    b.append("]");
    return NullCheck.notNull(b.toString());
  }

  @Override
  public BookStatusPriorityOrdering getPriority() {
    return BookStatusPriorityOrdering.BOOK_STATUS_REVOKE_IN_PROGRESS;
  }
}
