package org.nypl.simplified.books.core;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.nypl.simplified.opds.core.OPDSBlock;

import com.io7m.jnull.NullCheck;

public final class FeedBlock
{
  public static FeedBlock fromOPDSBlock(
    final OPDSBlock b)
  {
    NullCheck.notNull(b);

    final List<FeedEntryType> es = new ArrayList<FeedEntryType>();
    final List<OPDSAcquisitionFeedEntry> be_list = b.getBlockEntries();
    final int max = be_list.size();
    for (int index = 0; index < max; ++index) {
      final OPDSAcquisitionFeedEntry be =
        NullCheck.notNull(be_list.get(index));
      es.add(FeedEntryOPDS.fromOPDSAcquisitionFeedEntry(be));
    }

    return new FeedBlock(b.getBlockTitle(), b.getBlockURI(), es);
  }

  public static Map<String, FeedBlock> fromOPDSBlocks(
    final Map<String, OPDSBlock> bs)
  {
    NullCheck.notNull(bs);

    final Map<String, FeedBlock> rm = new HashMap<String, FeedBlock>();
    for (final String name : bs.keySet()) {
      final OPDSBlock block = NullCheck.notNull(bs.get(name));
      rm.put(name, FeedBlock.fromOPDSBlock(block));
    }

    return rm;
  }

  private final List<FeedEntryType> entries;
  private final String              title;
  private final URI                 uri;

  public FeedBlock(
    final String in_title,
    final URI in_uri,
    final List<FeedEntryType> in_entries)
  {
    this.title = NullCheck.notNull(in_title);
    this.uri = NullCheck.notNull(in_uri);
    this.entries = NullCheck.notNull(in_entries);
  }

  public List<FeedEntryType> getBlockEntries()
  {
    return this.entries;
  }

  public String getBlockTitle()
  {
    return this.title;
  }

  public URI getBlockURI()
  {
    return this.uri;
  }
}
