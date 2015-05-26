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

    if (f.getFeedGroups().isEmpty()) {
      return Feeds.withoutGroups(f);
    }
    return Feeds.withGroups(f);
  }

  private static FeedWithGroups withGroups(
    final OPDSAcquisitionFeed f)
  {
    return FeedWithGroups.fromAcquisitionFeed(f);
  }

  private static FeedWithoutGroups withoutGroups(
    final OPDSAcquisitionFeed f)
  {
    final FeedWithoutGroups rf =
      FeedWithoutGroups.newEmptyFeed(
        f.getFeedURI(),
        f.getFeedID(),
        f.getFeedUpdated(),
        f.getFeedTitle(),
        f.getFeedNext(),
        f.getFeedSearchURI(),
        f.getFeedFacetsByGroup(),
        f.getFeedFacetsOrder());

    final List<OPDSAcquisitionFeedEntry> in_entries = f.getFeedEntries();
    for (int index = 0; index < in_entries.size(); ++index) {
      final OPDSAcquisitionFeedEntry fe =
        NullCheck.notNull(in_entries.get(index));
      rf.add(FeedEntryOPDS.fromOPDSAcquisitionFeedEntry(fe));
    }

    return rf;
  }
}
