package org.nypl.simplified.books.core;

import com.io7m.jfunctional.FunctionType;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;
import org.nypl.simplified.opds.core.OPDSAcquisition;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeed;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.nypl.simplified.opds.core.OPDSFacet;
import org.nypl.simplified.opds.core.OPDSOpenSearch1_1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Functions for constructing feeds.
 */

public final class Feeds
{
  private Feeds()
  {
    throw new UnreachableCodeException();
  }

  /**
   * Construct a feed from the given acquisition feed.
   *
   * @param f      The feed
   * @param search The search document
   *
   * @return A new feed
   */

  public static FeedType fromAcquisitionFeed(
    final OPDSAcquisitionFeed f,
    final OptionType<OPDSOpenSearch1_1> search)
  {
    NullCheck.notNull(f);

    if (f.getFeedGroups().isEmpty()) {
      return Feeds.withoutGroups(f, search);
    }
    return Feeds.withGroups(f, search);
  }

  private static FeedWithGroups withGroups(
    final OPDSAcquisitionFeed f,
    final OptionType<OPDSOpenSearch1_1> search)
  {
    return FeedWithGroups.fromAcquisitionFeed(f, search);
  }

  private static FeedWithoutGroups withoutGroups(
    final OPDSAcquisitionFeed f,
    final OptionType<OPDSOpenSearch1_1> search)
  {
    final Map<String, List<FeedFacetType>> facets_by_group =
      new HashMap<String, List<FeedFacetType>>(4);
    final Map<String, List<OPDSFacet>> f_map = f.getFeedFacetsByGroup();
    for (final String k : f_map.keySet()) {
      final List<OPDSFacet> fs = f_map.get(k);
      final List<FeedFacetType> rs = new ArrayList<FeedFacetType>(4);
      for (final OPDSFacet ff : fs) {
        rs.add(new FeedFacetOPDS(NullCheck.notNull(ff)));
      }
      facets_by_group.put(k, rs);
    }

    final List<FeedFacetType> facets_order = new ArrayList<FeedFacetType>(4);
    for (final OPDSFacet ff : f.getFeedFacetsOrder()) {
      facets_order.add(new FeedFacetOPDS(NullCheck.notNull(ff)));
    }

    final OptionType<FeedSearchType> actual_search = search.map(
      new FunctionType<OPDSOpenSearch1_1, FeedSearchType>()
      {
        @Override public FeedSearchType call(
          final OPDSOpenSearch1_1 s)
        {
          return new FeedSearchOpen1_1(s);
        }
      });

    final FeedWithoutGroups rf = FeedWithoutGroups.newEmptyFeed(
      f.getFeedURI(),
      f.getFeedID(),
      f.getFeedUpdated(),
      f.getFeedTitle(),
      f.getFeedNext(),
      actual_search,
      facets_by_group,
      facets_order,
      f.getFeedTermsOfService(),
      f.getFeedAbout(),
      f.getFeedPrivacyPolicy(),
      f.getFeedLicenses());

    final List<OPDSAcquisitionFeedEntry> in_entries = f.getFeedEntries();
    for (int index = 0; index < in_entries.size(); ++index) {
      final OPDSAcquisitionFeedEntry fe =
        NullCheck.notNull(in_entries.get(index));
      if (!fe.getAcquisitions().isEmpty()) {
        OPDSAcquisition best = NullCheck.notNull(fe.getAcquisitions().get(0));
        for (final OPDSAcquisition current : fe.getAcquisitions()) {
          final OPDSAcquisition nn_current = NullCheck.notNull(current);
          if (Feeds.priority(nn_current)
            > Feeds.priority(best)) {
            best = nn_current;
          }
        }
        final OptionType<OPDSAcquisition> a_opt = Option.some(best);
        if (a_opt.isSome()) {
          rf.add(FeedEntryOPDS.fromOPDSAcquisitionFeedEntry(fe));
        }
      }
    }

    return rf;
  }

  private static int priority(
    final OPDSAcquisition a)
  {
    switch (a.getType()) {
      case ACQUISITION_BORROW:
        return 6;
      case ACQUISITION_OPEN_ACCESS:
        return 4;
      case ACQUISITION_GENERIC:
        return 5;
      case ACQUISITION_SAMPLE:
        return 3;
      case ACQUISITION_BUY:
        return 2;
      case ACQUISITION_SUBSCRIBE:
        return 1;
    }

    return 0;
  }
}
