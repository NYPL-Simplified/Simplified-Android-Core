package org.nypl.simplified.books.core;

import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;

import com.io7m.jnull.NullCheck;

public final class FeedEntryOPDS implements FeedEntryType
{
  private static final long              serialVersionUID = 1L;
  private final BookID                   book_id;
  private final OPDSAcquisitionFeedEntry entry;

  private FeedEntryOPDS(
    final BookID in_book_id,
    final OPDSAcquisitionFeedEntry in_e)
  {
    this.book_id = NullCheck.notNull(in_book_id);
    this.entry = NullCheck.notNull(in_e);
  }

  public OPDSAcquisitionFeedEntry getFeedEntry()
  {
    return this.entry;
  }

  public static FeedEntryType fromOPDSAcquisitionFeedEntry(
    final OPDSAcquisitionFeedEntry e)
  {
    return new FeedEntryOPDS(BookID.newIDFromEntry(e), e);
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
