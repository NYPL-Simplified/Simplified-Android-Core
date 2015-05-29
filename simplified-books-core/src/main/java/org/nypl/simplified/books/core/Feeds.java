package org.nypl.simplified.books.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nypl.simplified.opds.core.OPDSAcquisitionFeed;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.nypl.simplified.opds.core.OPDSFacet;

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
    final Map<String, List<FeedFacetType>> facets_by_group =
      new HashMap<String, List<FeedFacetType>>();
    final Map<String, List<OPDSFacet>> f_map = f.getFeedFacetsByGroup();
    for (final String k : f_map.keySet()) {
      final List<OPDSFacet> fs = f_map.get(k);
      final List<FeedFacetType> rs = new ArrayList<FeedFacetType>();
      for (final OPDSFacet ff : fs) {
        rs.add(new FeedFacetOPDS(NullCheck.notNull(ff)));
      }
      facets_by_group.put(k, rs);
    }

    final List<FeedFacetType> facets_order = new ArrayList<FeedFacetType>();
    for (final OPDSFacet ff : f.getFeedFacetsOrder()) {
      facets_order.add(new FeedFacetOPDS(NullCheck.notNull(ff)));
    }

    final FeedWithoutGroups rf =
      FeedWithoutGroups.newEmptyFeed(
        f.getFeedURI(),
        f.getFeedID(),
        f.getFeedUpdated(),
        f.getFeedTitle(),
        f.getFeedNext(),
        f.getFeedSearchURI(),
        facets_by_group,
        facets_order);

    final List<OPDSAcquisitionFeedEntry> in_entries = f.getFeedEntries();
    for (int index = 0; index < in_entries.size(); ++index) {
      final OPDSAcquisitionFeedEntry fe =
        NullCheck.notNull(in_entries.get(index));
      rf.add(FeedEntryOPDS.fromOPDSAcquisitionFeedEntry(fe));
    }

    return rf;
  }
}
