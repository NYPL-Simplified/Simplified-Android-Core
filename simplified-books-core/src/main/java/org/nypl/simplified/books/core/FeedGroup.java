package org.nypl.simplified.books.core;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.nypl.simplified.opds.core.OPDSGroup;

import com.io7m.jnull.NullCheck;

public final class FeedGroup
{
  public static FeedGroup fromOPDSGroup(
    final OPDSGroup b)
  {
    NullCheck.notNull(b);

    final List<FeedEntryType> es = new ArrayList<FeedEntryType>();
    final List<OPDSAcquisitionFeedEntry> be_list = b.getGroupEntries();
    final int max = be_list.size();
    for (int index = 0; index < max; ++index) {
      final OPDSAcquisitionFeedEntry be =
        NullCheck.notNull(be_list.get(index));
      es.add(FeedEntryOPDS.fromOPDSAcquisitionFeedEntry(be));
    }

    return new FeedGroup(b.getGroupTitle(), b.getGroupURI(), es);
  }

  public static Map<String, FeedGroup> fromOPDSGroups(
    final Map<String, OPDSGroup> bs)
  {
    NullCheck.notNull(bs);

    final Map<String, FeedGroup> rm = new HashMap<String, FeedGroup>();
    for (final String name : bs.keySet()) {
      final OPDSGroup block = NullCheck.notNull(bs.get(name));
      rm.put(name, FeedGroup.fromOPDSGroup(block));
    }

    return rm;
  }

  private final List<FeedEntryType> entries;
  private final String              title;
  private final URI                 uri;

  public FeedGroup(
    final String in_title,
    final URI in_uri,
    final List<FeedEntryType> in_entries)
  {
    this.title = NullCheck.notNull(in_title);
    this.uri = NullCheck.notNull(in_uri);
    this.entries = NullCheck.notNull(in_entries);
  }

  public List<FeedEntryType> getGroupEntries()
  {
    return this.entries;
  }

  public String getGroupTitle()
  {
    return this.title;
  }

  public URI getGroupURI()
  {
    return this.uri;
  }
}
