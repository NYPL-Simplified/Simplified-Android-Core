package org.nypl.simplified.books.core;

import com.io7m.jnull.NullCheck;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;

/**
 * An entry from an OPDS feed.
 */

public final class FeedEntryOPDS implements FeedEntryType
{
  private static final long serialVersionUID = 1L;
  private final BookID                   book_id;
  private final OPDSAcquisitionFeedEntry entry;

  private FeedEntryOPDS(
    final BookID in_book_id,
    final OPDSAcquisitionFeedEntry in_e)
  {
    this.book_id = NullCheck.notNull(in_book_id);
    this.entry = NullCheck.notNull(in_e);
  }

  /**
   * Construct a feed entry from the given OPDS feed entry.
   *
   * @param e The entry
   *
   * @return A feed entry
   */

  public static FeedEntryType fromOPDSAcquisitionFeedEntry(
    final OPDSAcquisitionFeedEntry e)
  {
    return new FeedEntryOPDS(BookID.newIDFromEntry(e), e);
  }

  /**
   * @return The actual feed entry
   */

  public OPDSAcquisitionFeedEntry getFeedEntry()
  {
    return this.entry;
  }

  @Override public BookID getBookID()
  {
    return this.book_id;
  }

  @Override public <A, E extends Exception> A matchFeedEntry(
    final FeedEntryMatcherType<A, E> m)
    throws E
  {
    return m.onFeedEntryOPDS(this);
  }
}
