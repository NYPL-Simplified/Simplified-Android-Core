package org.nypl.simplified.opds.core;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Pair;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

/**
 * The type of OPDS acquisition feeds.
 */

@SuppressWarnings("synthetic-access") public final class OPDSAcquisitionFeed implements
  Serializable
{
  private static final class Builder implements
    OPDSAcquisitionFeedBuilderType
  {
    private final Map<String, URI>                            block_uris;
    private final Map<String, List<OPDSAcquisitionFeedEntry>> blocks;
    private final List<String>                                blocks_order;
    private final List<OPDSAcquisitionFeedEntry>              entries;
    private final String                                      id;
    private OptionType<URI>                                   next;
    private OptionType<OPDSSearchLink>                        search;
    private final String                                      title;
    private final Calendar                                    updated;
    private final URI                                         uri;

    private Builder(
      final URI in_uri,
      final String in_title,
      final String in_id,
      final Calendar in_updated)
    {
      this.uri = NullCheck.notNull(in_uri);
      this.title = NullCheck.notNull(in_title);
      this.id = NullCheck.notNull(in_id);
      this.updated = NullCheck.notNull(in_updated);
      this.entries = new ArrayList<OPDSAcquisitionFeedEntry>();
      this.blocks_order = new ArrayList<String>();
      this.blocks = new HashMap<String, List<OPDSAcquisitionFeedEntry>>();
      this.block_uris = new HashMap<String, URI>();
      this.next = Option.none();
      this.search = Option.none();
    }

    @Override public void addEntry(
      final OPDSAcquisitionFeedEntry e)
    {
      NullCheck.notNull(e);

      final Set<Pair<String, URI>> in_blocks = e.getBlocks();
      if (in_blocks.isEmpty()) {
        this.entries.add(e);
      } else {
        for (final Pair<String, URI> b : in_blocks) {
          NullCheck.notNull(b);
          final String b_name = b.getLeft();
          final URI b_uri = b.getRight();

          List<OPDSAcquisitionFeedEntry> es;
          if (this.blocks.containsKey(b_name)) {
            es = NullCheck.notNull(this.blocks.get(b_name));
          } else {
            es = new ArrayList<OPDSAcquisitionFeedEntry>();
            this.blocks_order.add(b_name);
          }

          es.add(e);
          this.blocks.put(b_name, es);
          this.block_uris.put(b_name, b_uri);
        }
      }
    }

    @Override public OPDSAcquisitionFeed build()
    {
      final Map<String, OPDSBlock> r_blocks =
        new HashMap<String, OPDSBlock>();

      for (final String name : this.blocks.keySet()) {
        final String nn_name = NullCheck.notNull(name);
        final List<OPDSAcquisitionFeedEntry> in_entries =
          NullCheck.notNull(this.blocks.get(nn_name));
        final URI in_uri = NullCheck.notNull(this.block_uris.get(nn_name));
        r_blocks.put(nn_name, new OPDSBlock(nn_name, in_uri, in_entries));
      }

      return new OPDSAcquisitionFeed(
        this.uri,
        this.entries,
        r_blocks,
        this.blocks_order,
        this.id,
        this.updated,
        this.title,
        this.next,
        this.search);
    }

    @Override public void setNextOption(
      final OptionType<URI> in_next)
    {
      this.next = NullCheck.notNull(in_next);
    }

    @Override public void setSearchOption(
      final OptionType<OPDSSearchLink> in_search)
    {
      this.search = NullCheck.notNull(in_search);
    }
  }

  private static final long serialVersionUID = -7962463871020194252L;

  public static OPDSAcquisitionFeedBuilderType newBuilder(
    final URI in_uri,
    final String in_id,
    final Calendar in_updated,
    final String in_title)
  {
    return new Builder(in_uri, in_title, in_id, in_updated);
  }

  private final Map<String, OPDSBlock>         blocks;
  private final List<String>                   blocks_order;
  private final List<OPDSAcquisitionFeedEntry> entries;
  private final String                         id;
  private final OptionType<URI>                next;
  private final OptionType<OPDSSearchLink>     search;
  private final String                         title;
  private final Calendar                       updated;
  private final URI                            uri;

  private OPDSAcquisitionFeed(
    final URI in_uri,
    final List<OPDSAcquisitionFeedEntry> in_entries,
    final Map<String, OPDSBlock> in_blocks,
    final List<String> in_blocks_order,
    final String in_id,
    final Calendar in_updated,
    final String in_title,
    final OptionType<URI> in_next,
    final OptionType<OPDSSearchLink> in_search)
  {
    this.uri = NullCheck.notNull(in_uri);
    this.entries =
      NullCheck.notNull(Collections.unmodifiableList(in_entries));
    this.blocks = NullCheck.notNull(Collections.unmodifiableMap(in_blocks));
    this.blocks_order =
      NullCheck.notNull(Collections.unmodifiableList(in_blocks_order));
    this.id = NullCheck.notNull(in_id);
    this.updated = NullCheck.notNull(in_updated);
    this.title = NullCheck.notNull(in_title);
    this.next = NullCheck.notNull(in_next);
    this.search = NullCheck.notNull(in_search);
  }

  @Override public boolean equals(
    final @Nullable Object obj)
  {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (this.getClass() != obj.getClass()) {
      return false;
    }
    final OPDSAcquisitionFeed other = (OPDSAcquisitionFeed) obj;
    return this.uri.equals(other.uri)
      && this.entries.equals(other.entries)
      && this.blocks.equals(other.blocks)
      && this.blocks_order.equals(other.blocks_order)
      && this.id.equals(other.id)
      && this.title.equals(other.title)
      && this.updated.equals(other.updated)
      && this.next.equals(other.next)
      && this.search.equals(other.search);
  }

  public Map<String, OPDSBlock> getFeedBlocks()
  {
    return this.blocks;
  }

  public List<String> getFeedBlocksOrder()
  {
    return this.blocks_order;
  }

  public List<OPDSAcquisitionFeedEntry> getFeedEntries()
  {
    return this.entries;
  }

  public String getFeedID()
  {
    return this.id;
  }

  public OptionType<URI> getFeedNext()
  {
    return this.next;
  }

  public OptionType<OPDSSearchLink> getFeedSearchURI()
  {
    return this.search;
  }

  public String getFeedTitle()
  {
    return this.title;
  }

  public Calendar getFeedUpdated()
  {
    return this.updated;
  }

  public URI getFeedURI()
  {
    return this.uri;
  }

  @Override public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = (prime * result) + this.uri.hashCode();
    result = (prime * result) + this.entries.hashCode();
    result = (prime * result) + this.blocks.hashCode();
    result = (prime * result) + this.blocks_order.hashCode();
    result = (prime * result) + this.id.hashCode();
    result = (prime * result) + this.title.hashCode();
    result = (prime * result) + this.updated.hashCode();
    result = (prime * result) + this.next.hashCode();
    result = (prime * result) + this.search.hashCode();
    return result;
  }
}
