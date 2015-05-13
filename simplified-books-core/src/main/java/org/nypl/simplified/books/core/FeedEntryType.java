package org.nypl.simplified.books.core;

import java.io.Serializable;

public interface FeedEntryType extends Serializable
{
  BookID getBookID();

  /**
   * Match the type of feed entry.
   *
   * @param m
   *          The matcher
   * @return The value returned by the matcher
   * @throws E
   *           If the matcher raises <tt>E</tt>
   */

  <A, E extends Exception> A matchFeedEntry(
    FeedEntryMatcherType<A, E> m)
    throws E;
}
