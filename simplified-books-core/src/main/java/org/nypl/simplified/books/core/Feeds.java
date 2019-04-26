package org.nypl.simplified.books.core;

import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;

import org.nypl.simplified.opds.core.OPDSAcquisitionFeed;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.nypl.simplified.opds.core.OPDSAcquisitionPath;
import org.nypl.simplified.opds.core.OPDSFacet;
import org.nypl.simplified.opds.core.OPDSOpenSearch1_1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Functions for constructing feeds.
 */

public final class Feeds {

  private Feeds() {
    throw new UnreachableCodeException();
  }

  /**
   * Construct a feed from the given acquisition feed.
   *
   * @param f      The feed
   * @param search The search document
   * @return A new feed
   */

  public static FeedType fromAcquisitionFeed(
    final OPDSAcquisitionFeed f,
    final OptionType<OPDSOpenSearch1_1> search) {

    NullCheck.notNull(f);
    if (f.getFeedGroups().isEmpty()) {
      return Feeds.withoutGroups(f, search);
    }
    return Feeds.withGroups(f, search);
  }

  private static FeedWithGroups withGroups(
    final OPDSAcquisitionFeed f,
    final OptionType<OPDSOpenSearch1_1> search) {
    final Map<String, List<FeedFacetType>> facets_by_group = constructFacetGroups(f);
    final List<FeedFacetType> facets_order = constructFacetsOrdered(f);
    final OptionType<FeedSearchType> actual_search = search.map(FeedSearchOpen1_1::new);

    return new FeedWithGroups(
      f.getFeedURI(),
      f.getFeedID(),
      f.getFeedUpdated(),
      f.getFeedTitle(),
      actual_search,
      f.getFeedGroupsOrder(),
      FeedGroup.fromOPDSGroups(f.getFeedGroups()),
      facets_by_group,
      facets_order,
      f.getFeedTermsOfService(),
      f.getFeedAbout(),
      f.getFeedPrivacyPolicy(),
      f.getFeedLicenses());
  }

  private static FeedWithoutGroups withoutGroups(
    final OPDSAcquisitionFeed f,
    final OptionType<OPDSOpenSearch1_1> search) {
    final Map<String, List<FeedFacetType>> facets_by_group = constructFacetGroups(f);
    final List<FeedFacetType> facets_order = constructFacetsOrdered(f);
    final OptionType<FeedSearchType> actual_search = search.map(FeedSearchOpen1_1::new);

    final FeedWithoutGroups resultFeed =
      FeedWithoutGroups.newEmptyFeed(
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
      final OPDSAcquisitionFeedEntry feedEntry =
        Objects.requireNonNull(in_entries.get(index));
      final OptionType<OPDSAcquisitionPath> acquisition =
        BookAcquisitionSelection.INSTANCE.preferredAcquisition(feedEntry.getAcquisitionPaths());

      if (acquisition.isSome()) {
        resultFeed.add(FeedEntryOPDS.Companion.fromOPDSAcquisitionFeedEntry(feedEntry));
      }
    }

    return resultFeed;
  }

  private static List<FeedFacetType> constructFacetsOrdered(OPDSAcquisitionFeed f) {
    final List<FeedFacetType> facets_order = new ArrayList<>(4);
    for (final OPDSFacet ff : f.getFeedFacetsOrder()) {
      facets_order.add(new FeedFacetOPDS(NullCheck.notNull(ff)));
    }
    return facets_order;
  }

  private static Map<String, List<FeedFacetType>> constructFacetGroups(OPDSAcquisitionFeed f) {
    final Map<String, List<FeedFacetType>> facets_by_group = new HashMap<>(4);
    final Map<String, List<OPDSFacet>> f_map = f.getFeedFacetsByGroup();
    for (final String k : f_map.keySet()) {
      final List<OPDSFacet> fs = f_map.get(k);
      final List<FeedFacetType> rs = new ArrayList<>(4);
      for (final OPDSFacet ff : fs) {
        rs.add(new FeedFacetOPDS(NullCheck.notNull(ff)));
      }
      facets_by_group.put(k, rs);
    }
    return facets_by_group;
  }
}
