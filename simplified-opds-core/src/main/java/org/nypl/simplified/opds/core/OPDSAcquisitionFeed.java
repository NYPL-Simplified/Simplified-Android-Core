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
    private final List<OPDSAcquisitionFeedEntry>              entries;
    private final Map<String, List<OPDSFacet>>                facets_by_group;
    private final List<OPDSFacet>                             facets_order;
    private final Map<String, URI>                            group_uris;
    private final Map<String, List<OPDSAcquisitionFeedEntry>> groups;
    private final List<String>                                groups_order;
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
      this.facets_order = new ArrayList<OPDSFacet>();
      this.facets_by_group = new HashMap<String, List<OPDSFacet>>();
      this.groups_order = new ArrayList<String>();
      this.groups = new HashMap<String, List<OPDSAcquisitionFeedEntry>>();
      this.group_uris = new HashMap<String, URI>();
      this.next = Option.none();
      this.search = Option.none();
    }

    @Override public void addEntry(
      final OPDSAcquisitionFeedEntry e)
    {
      NullCheck.notNull(e);

      final Set<Pair<String, URI>> in_groups = e.getGroups();
      if (in_groups.isEmpty()) {
        this.entries.add(e);
      } else {
        for (final Pair<String, URI> b : in_groups) {
          NullCheck.notNull(b);
          final String b_name = b.getLeft();
          final URI b_uri = b.getRight();

          List<OPDSAcquisitionFeedEntry> es;
          if (this.groups.containsKey(b_name)) {
            es = NullCheck.notNull(this.groups.get(b_name));
          } else {
            es = new ArrayList<OPDSAcquisitionFeedEntry>();
            this.groups_order.add(b_name);
          }

          es.add(e);
          this.groups.put(b_name, es);
          this.group_uris.put(b_name, b_uri);
        }
      }
    }

    @Override public void addFacet(
      final OPDSFacet f)
    {
      NullCheck.notNull(f);

      final String group = f.getGroup();
      final List<OPDSFacet> fs;
      if (this.facets_by_group.containsKey(group)) {
        fs = NullCheck.notNull(this.facets_by_group.get(group));
      } else {
        fs = new ArrayList<OPDSFacet>();
      }
      fs.add(f);
      this.facets_by_group.put(group, fs);
      this.facets_order.add(f);
    }

    @Override public OPDSAcquisitionFeed build()
    {
      final Map<String, OPDSGroup> r_groups =
        new HashMap<String, OPDSGroup>();

      for (final String name : this.groups.keySet()) {
        final String nn_name = NullCheck.notNull(name);
        final List<OPDSAcquisitionFeedEntry> in_entries =
          NullCheck.notNull(this.groups.get(nn_name));
        final URI in_uri = NullCheck.notNull(this.group_uris.get(nn_name));
        r_groups.put(nn_name, new OPDSGroup(nn_name, in_uri, in_entries));
      }

      return new OPDSAcquisitionFeed(
        this.uri,
        this.entries,
        r_groups,
        this.groups_order,
        this.id,
        this.updated,
        this.title,
        this.next,
        this.search,
        this.facets_order,
        this.facets_by_group);
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

  private final List<OPDSAcquisitionFeedEntry> entries;
  private final Map<String, List<OPDSFacet>>   facets_by_group;
  private final List<OPDSFacet>                facets_order;
  private final Map<String, OPDSGroup>         groups;
  private final List<String>                   groups_order;
  private final String                         id;
  private final OptionType<URI>                next;
  private final OptionType<OPDSSearchLink>     search;
  private final String                         title;
  private final Calendar                       updated;
  private final URI                            uri;

  private OPDSAcquisitionFeed(
    final URI in_uri,
    final List<OPDSAcquisitionFeedEntry> in_entries,
    final Map<String, OPDSGroup> in_groups,
    final List<String> in_groups_order,
    final String in_id,
    final Calendar in_updated,
    final String in_title,
    final OptionType<URI> in_next,
    final OptionType<OPDSSearchLink> in_search,
    final List<OPDSFacet> in_facets_order,
    final Map<String, List<OPDSFacet>> in_facets)
  {
    this.uri = NullCheck.notNull(in_uri);
    this.entries =
      NullCheck.notNull(Collections.unmodifiableList(in_entries));
    this.facets_order =
      NullCheck.notNull(Collections.unmodifiableList(in_facets_order));
    this.facets_by_group =
      NullCheck.notNull(Collections.unmodifiableMap(in_facets));
    this.groups = NullCheck.notNull(Collections.unmodifiableMap(in_groups));
    this.groups_order =
      NullCheck.notNull(Collections.unmodifiableList(in_groups_order));
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
      && this.facets_by_group.equals(other.facets_by_group)
      && this.facets_order.equals(other.facets_order)
      && this.groups.equals(other.groups)
      && this.groups_order.equals(other.groups_order)
      && this.id.equals(other.id)
      && this.title.equals(other.title)
      && this.updated.equals(other.updated)
      && this.next.equals(other.next)
      && this.search.equals(other.search);
  }

  public List<OPDSAcquisitionFeedEntry> getFeedEntries()
  {
    return this.entries;
  }

  public Map<String, List<OPDSFacet>> getFeedFacetsByGroup()
  {
    return this.facets_by_group;
  }

  public List<OPDSFacet> getFeedFacetsOrder()
  {
    return this.facets_order;
  }

  public Map<String, OPDSGroup> getFeedGroups()
  {
    return this.groups;
  }

  public List<String> getFeedGroupsOrder()
  {
    return this.groups_order;
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
    result = (prime * result) + this.groups.hashCode();
    result = (prime * result) + this.groups_order.hashCode();
    result = (prime * result) + this.facets_by_group.hashCode();
    result = (prime * result) + this.facets_order.hashCode();
    result = (prime * result) + this.id.hashCode();
    result = (prime * result) + this.title.hashCode();
    result = (prime * result) + this.updated.hashCode();
    result = (prime * result) + this.next.hashCode();
    result = (prime * result) + this.search.hashCode();
    return result;
  }
}
