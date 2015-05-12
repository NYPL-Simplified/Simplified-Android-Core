package org.nypl.simplified.books.core;

import java.util.List;

import org.nypl.simplified.opds.core.OPDSAcquisitionFeed;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;

import com.io7m.jnull.NullCheck;

public final class Feeds
{
  public static FeedType fromAcquisitionFeed(
    final OPDSAcquisitionFeed f)
  {
    NullCheck.notNull(f);

    if (f.getFeedBlocks().isEmpty()) {
      return Feeds.withoutBlocks(f);
    }
    return Feeds.withBlocks(f);
  }

  private static FeedWithBlocks withBlocks(
    final OPDSAcquisitionFeed f)
  {
    return FeedWithBlocks.fromAcquisitionFeed(f);
  }

  private static FeedWithoutBlocks withoutBlocks(
    final OPDSAcquisitionFeed f)
  {
    final FeedWithoutBlocks rf =
      FeedWithoutBlocks.newEmptyFeed(
        f.getFeedURI(),
        f.getFeedID(),
        f.getFeedUpdated(),
        f.getFeedTitle(),
        f.getFeedNext(),
        f.getFeedSearchURI());

    final List<OPDSAcquisitionFeedEntry> in_entries = f.getFeedEntries();
    for (int index = 0; index < in_entries.size(); ++index) {
      final OPDSAcquisitionFeedEntry fe =
        NullCheck.notNull(in_entries.get(index));
      rf.add(FeedEntryOPDS.fromOPDSAcquisitionFeedEntry(fe));
    }

    return rf;
  }
}
