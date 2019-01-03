package org.nypl.simplified.books.feeds;

import org.nypl.simplified.books.book_database.BookID;

import java.io.Serializable;

/**
 * The type of feed entries.
 */

public interface FeedEntryType extends Serializable
{
  /**
   * @return The book ID for the feed entry
   */

  BookID getBookID();

  /**
   * Match the type of feed entry.
   *
   * @param m   The matcher
   * @param <A> The type of returned values
   * @param <E> The type of raised exceptions
   *
   * @return The value returned by the matcher
   *
   * @throws E If the matcher raises {@code E}
   */

  <A, E extends Exception> A matchFeedEntry(
    FeedEntryMatcherType<A, E> m)
    throws E;
}
