package org.nypl.simplified.books.core;

import com.io7m.jnull.NullCheck;

public final class FeedEntryCorrupt implements FeedEntryType
{
  private static final long serialVersionUID = 1L;

  public static FeedEntryType fromIDAndError(
    final BookID in_book_id,
    final Throwable in_x)
  {
    return new FeedEntryCorrupt(in_book_id, in_x);
  }

  private final BookID    book_id;
  private final Throwable error;

  private FeedEntryCorrupt(
    final BookID in_book_id,
    final Throwable in_x)
  {
    this.book_id = NullCheck.notNull(in_book_id);
    this.error = NullCheck.notNull(in_x);
  }

  @Override public BookID getBookID()
  {
    return this.book_id;
  }

  public Throwable getError()
  {
    return this.error;
  }

  @Override public <A, E extends Exception> A matchFeedEntry(
    final FeedEntryMatcherType<A, E> m)
    throws E
  {
    return m.onFeedEntryCorrupt(this);
  }
}
