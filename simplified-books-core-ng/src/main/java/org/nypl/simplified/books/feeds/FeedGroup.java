package org.nypl.simplified.books.feeds;

import com.io7m.jnull.NullCheck;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.nypl.simplified.opds.core.OPDSGroup;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A feed group.
 */

public final class FeedGroup
{
  private final List<FeedEntry> entries;
  private final String              title;
  private final URI                 uri;

  /**
   * Construct a feed group.
   *
   * @param in_title   The group title
   * @param in_uri     The group URI
   * @param in_entries A list of feed entries
   */

  public FeedGroup(
    final String in_title,
    final URI in_uri,
    final List<FeedEntry> in_entries)
  {
    this.title = NullCheck.notNull(in_title);
    this.uri = NullCheck.notNull(in_uri);
    this.entries = NullCheck.notNull(in_entries);
  }

  /**
   * @param b An OPDS group
   *
   * @return A group from the given OPDS group
   */

  public static FeedGroup fromOPDSGroup(
    final OPDSGroup b)
  {
    NullCheck.notNull(b);

    final List<FeedEntry> es = new ArrayList<FeedEntry>(32);
    final List<OPDSAcquisitionFeedEntry> be_list = b.getGroupEntries();
    final int max = be_list.size();
    for (int index = 0; index < max; ++index) {
      final OPDSAcquisitionFeedEntry be = NullCheck.notNull(be_list.get(index));
      es.add(new FeedEntry.FeedEntryOPDS(be));
    }

    return new FeedGroup(b.getGroupTitle(), b.getGroupURI(), es);
  }

  /**
   * @param bs A map of OPDS groups
   *
   * @return A map of groups from the given OPDS groups
   */

  public static Map<String, FeedGroup> fromOPDSGroups(
    final Map<String, OPDSGroup> bs)
  {
    NullCheck.notNull(bs);

    final Map<String, FeedGroup> rm = new HashMap<String, FeedGroup>(32);
    for (final String name : bs.keySet()) {
      final OPDSGroup block = NullCheck.notNull(bs.get(name));
      rm.put(name, FeedGroup.fromOPDSGroup(block));
    }

    return rm;
  }

  /**
   * @return The list of entries in the group
   */

  public List<FeedEntry> getGroupEntries()
  {
    return this.entries;
  }

  /**
   * @return The group title
   */

  public String getGroupTitle()
  {
    return this.title;
  }

  /**
   * @return The URI of the group
   */

  public URI getGroupURI()
  {
    return this.uri;
  }
}
