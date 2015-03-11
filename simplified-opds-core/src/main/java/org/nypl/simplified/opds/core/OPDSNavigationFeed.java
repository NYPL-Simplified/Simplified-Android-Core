package org.nypl.simplified.opds.core;

import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

/**
 * The type of OPDS navigation feeds.
 */

@SuppressWarnings("synthetic-access") public final class OPDSNavigationFeed implements
  OPDSFeedType
{
  private static final class Builder implements OPDSNavigationFeedBuilderType
  {
    private final List<OPDSNavigationFeedEntry> entries;
    private final String                        id;
    private OptionType<OPDSSearchLink>          search;
    private final String                        title;
    private final Calendar                      updated;
    private final URI                           uri;

    private Builder(
      final URI in_uri,
      final String in_title,
      final Calendar in_updated,
      final String in_id)
    {
      this.uri = NullCheck.notNull(in_uri);
      this.title = NullCheck.notNull(in_title);
      this.updated = NullCheck.notNull(in_updated);
      this.id = NullCheck.notNull(in_id);
      this.entries = new ArrayList<OPDSNavigationFeedEntry>();
      this.search = Option.none();
    }

    @Override public void addEntry(
      final OPDSNavigationFeedEntry e)
    {
      this.entries.add(NullCheck.notNull(e));
    }

    @Override public OPDSNavigationFeed build()
    {
      return new OPDSNavigationFeed(
        this.uri,
        this.id,
        this.updated,
        this.title,
        this.entries,
        this.search);
    }

    @Override public void setSearchOption(
      final OptionType<OPDSSearchLink> s)
    {
      this.search = NullCheck.notNull(s);
    }
  }

  private static final long serialVersionUID = 2410830597217814161L;

  public static OPDSNavigationFeedBuilderType newBuilder(
    final URI in_uri,
    final String in_id,
    final Calendar in_updated,
    final String in_title)
  {
    return new Builder(in_uri, in_title, in_updated, in_id);
  }

  private final List<OPDSNavigationFeedEntry> entries;
  private final String                        id;
  private final OptionType<OPDSSearchLink>    search;
  private final String                        title;
  private final Calendar                      updated;
  private final URI                           uri;

  private OPDSNavigationFeed(
    final URI in_uri,
    final String in_id,
    final Calendar in_updated,
    final String in_title,
    final List<OPDSNavigationFeedEntry> in_entries,
    final OptionType<OPDSSearchLink> in_search)
  {
    this.uri = NullCheck.notNull(in_uri);
    this.id = NullCheck.notNull(in_id);
    this.updated = NullCheck.notNull(in_updated);
    this.title = NullCheck.notNull(in_title);
    this.entries =
      NullCheck.notNull(Collections.unmodifiableList(in_entries));
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
    final OPDSNavigationFeed other = (OPDSNavigationFeed) obj;
    return this.uri.equals(other.uri)
      && this.entries.equals(other.entries)
      && this.id.equals(other.id)
      && this.title.equals(other.title)
      && this.search.equals(other.search)
      && this.updated.equals(other.updated);
  }

  public List<OPDSNavigationFeedEntry> getFeedEntries()
  {
    return this.entries;
  }

  @Override public String getFeedID()
  {
    return this.id;
  }

  @Override public OptionType<OPDSSearchLink> getFeedSearchURI()
  {
    return this.search;
  }

  @Override public String getFeedTitle()
  {
    return this.title;
  }

  @Override public Calendar getFeedUpdated()
  {
    return this.updated;
  }

  @Override public URI getFeedURI()
  {
    return this.uri;
  }

  @Override public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = (prime * result) + this.uri.hashCode();
    result = (prime * result) + this.entries.hashCode();
    result = (prime * result) + this.id.hashCode();
    result = (prime * result) + this.title.hashCode();
    result = (prime * result) + this.updated.hashCode();
    result = (prime * result) + this.search.hashCode();
    return result;
  }

  @Override public <A, E extends Exception> A matchFeedType(
    final OPDSFeedMatcherType<A, E> m)
    throws E
  {
    return m.onNavigationFeed(this);
  }
}
