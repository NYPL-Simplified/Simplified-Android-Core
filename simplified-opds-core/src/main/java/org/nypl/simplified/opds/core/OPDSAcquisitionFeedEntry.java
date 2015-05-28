package org.nypl.simplified.opds.core;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Pair;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

/**
 * The type of entries in acquisition feeds.
 */

@SuppressWarnings("synthetic-access") public final class OPDSAcquisitionFeedEntry implements
  Serializable
{
  private static final class Builder implements
    OPDSAcquisitionFeedEntryBuilderType
  {
    private final List<OPDSAcquisition>  acquisitions;
    private final List<String>           authors;
    private final List<OPDSCategory>     categories;
    private OptionType<URI>              cover;
    private final Set<Pair<String, URI>> groups;
    private final String                 id;
    private OptionType<Calendar>         published;
    private OptionType<String>           publisher;
    private String                       summary;
    private OptionType<URI>              thumbnail;
    private final String                 title;
    private final Calendar               updated;

    private Builder(
      final String in_id,
      final String in_title,
      final Calendar in_updated)
    {
      this.id = NullCheck.notNull(in_id);
      this.title = NullCheck.notNull(in_title);
      this.updated = NullCheck.notNull(in_updated);
      this.summary = "";
      this.thumbnail = Option.none();
      this.cover = Option.none();
      this.acquisitions = new ArrayList<OPDSAcquisition>();
      this.authors = new ArrayList<String>();
      this.published = Option.none();
      this.publisher = Option.none();
      this.categories = new ArrayList<OPDSCategory>();
      this.groups = new HashSet<Pair<String, URI>>();
    }

    @Override public void addAcquisition(
      final OPDSAcquisition a)
    {
      this.acquisitions.add(NullCheck.notNull(a));
    }

    @Override public void addAuthor(
      final String name)
    {
      this.authors.add(NullCheck.notNull(name));
    }

    @Override public void addCategory(
      final OPDSCategory c)
    {
      this.categories.add(NullCheck.notNull(c));
    }

    @Override public void addGroup(
      final URI uri,
      final String b)
    {
      NullCheck.notNull(uri);
      NullCheck.notNull(b);
      this.groups.add(Pair.pair(b, uri));
    }

    @Override public OPDSAcquisitionFeedEntry build()
    {
      return new OPDSAcquisitionFeedEntry(
        this.authors,
        this.acquisitions,
        this.groups,
        this.cover,
        this.id,
        this.title,
        this.thumbnail,
        this.updated,
        this.summary,
        this.published,
        this.publisher,
        this.categories);
    }

    @Override public void setCoverOption(
      final OptionType<URI> uri)
    {
      this.cover = NullCheck.notNull(uri);
    }

    @Override public void setPublishedOption(
      final OptionType<Calendar> pub)
    {
      this.published = NullCheck.notNull(pub);
    }

    @Override public void setPublisherOption(
      final OptionType<String> pub)
    {
      this.publisher = NullCheck.notNull(pub);
    }

    @Override public void setSummaryOption(
      final OptionType<String> text)
    {
      if (text.isNone()) {
        this.summary = "";
      } else {
        this.summary = ((Some<String>) text).get();
      }
    }

    @Override public void setThumbnailOption(
      final OptionType<URI> uri)
    {
      this.thumbnail = NullCheck.notNull(uri);
    }
  }

  private static final long serialVersionUID = 2L;

  /**
   * Construct a new mutable builder for feed entries.
   *
   * @param in_id
   *          The feed ID
   * @param in_title
   *          The feed title
   * @param in_updated
   *          The feed updated time
   * @return A new builder
   */

  public static OPDSAcquisitionFeedEntryBuilderType newBuilder(
    final String in_id,
    final String in_title,
    final Calendar in_updated)
  {
    return new Builder(in_id, in_title, in_updated);
  }

  private final List<OPDSAcquisition>  acquisitions;
  private final List<String>           authors;
  private final List<OPDSCategory>     categories;
  private final OptionType<URI>        cover;
  private final Set<Pair<String, URI>> groups;
  private final String                 id;
  private final OptionType<Calendar>   published;
  private final OptionType<String>     publisher;
  private final String                 summary;
  private final OptionType<URI>        thumbnail;
  private final String                 title;
  private final Calendar               updated;

  private OPDSAcquisitionFeedEntry(
    final List<String> in_authors,
    final List<OPDSAcquisition> in_acquisitions,
    final Set<Pair<String, URI>> in_groups,
    final OptionType<URI> in_cover,
    final String in_id,
    final String in_title,
    final OptionType<URI> in_thumbnail,
    final Calendar in_updated,
    final String in_summary,
    final OptionType<Calendar> in_published,
    final OptionType<String> in_publisher,
    final List<OPDSCategory> in_categories)
  {
    this.authors =
      NullCheck.notNull(Collections.unmodifiableList(in_authors));
    this.acquisitions =
      NullCheck.notNull(Collections.unmodifiableList(in_acquisitions));
    this.groups = NullCheck.notNull(in_groups);
    this.cover = NullCheck.notNull(in_cover);
    this.id = NullCheck.notNull(in_id);
    this.title = NullCheck.notNull(in_title);
    this.thumbnail = NullCheck.notNull(in_thumbnail);
    this.updated = NullCheck.notNull(in_updated);
    this.summary = NullCheck.notNull(in_summary);
    this.published = NullCheck.notNull(in_published);
    this.publisher = NullCheck.notNull(in_publisher);
    this.categories = NullCheck.notNull(in_categories);
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
    final OPDSAcquisitionFeedEntry other = (OPDSAcquisitionFeedEntry) obj;
    return this.acquisitions.equals(other.acquisitions)
      && this.authors.equals(other.authors)
      && this.groups.equals(other.groups)
      && this.categories.equals(other.categories)
      && this.cover.equals(other.cover)
      && this.id.equals(other.id)
      && this.summary.equals(other.summary)
      && this.thumbnail.equals(other.thumbnail)
      && this.title.equals(other.title)
      && this.updated.equals(other.updated)
      && this.published.equals(other.published)
      && this.publisher.equals(other.publisher);
  }

  public List<OPDSAcquisition> getAcquisitions()
  {
    return this.acquisitions;
  }

  public List<String> getAuthors()
  {
    return this.authors;
  }

  public List<OPDSCategory> getCategories()
  {
    return this.categories;
  }

  public OptionType<URI> getCover()
  {
    return this.cover;
  }

  public Set<Pair<String, URI>> getGroups()
  {
    return this.groups;
  }

  public String getID()
  {
    return this.id;
  }

  public OptionType<Calendar> getPublished()
  {
    return this.published;
  }

  public OptionType<String> getPublisher()
  {
    return this.publisher;
  }

  public String getSummary()
  {
    return this.summary;
  }

  public OptionType<URI> getThumbnail()
  {
    return this.thumbnail;
  }

  public String getTitle()
  {
    return this.title;
  }

  public Calendar getUpdated()
  {
    return this.updated;
  }

  @Override public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = (prime * result) + this.acquisitions.hashCode();
    result = (prime * result) + this.authors.hashCode();
    result = (prime * result) + this.groups.hashCode();
    result = (prime * result) + this.cover.hashCode();
    result = (prime * result) + this.categories.hashCode();
    result = (prime * result) + this.id.hashCode();
    result = (prime * result) + this.summary.hashCode();
    result = (prime * result) + this.thumbnail.hashCode();
    result = (prime * result) + this.title.hashCode();
    result = (prime * result) + this.updated.hashCode();
    result = (prime * result) + this.published.hashCode();
    result = (prime * result) + this.publisher.hashCode();
    return result;
  }

  @Override public String toString()
  {
    final StringBuilder b = new StringBuilder();
    b.append("OPDSAcquisitionFeedEntry [acquisitions=");
    b.append(this.acquisitions);
    b.append(", authors=");
    b.append(this.authors);
    b.append(", groups=");
    b.append(this.groups);
    b.append(", categories=");
    b.append(this.categories);
    b.append(", cover=");
    b.append(this.cover);
    b.append(", id=");
    b.append(this.id);
    b.append(", published=");
    b.append(this.published);
    b.append(", publisher=");
    b.append(this.publisher);
    b.append(", summary=");
    b.append(this.summary);
    b.append(", thumbnail=");
    b.append(this.thumbnail);
    b.append(", title=");
    b.append(this.title);
    b.append(", updated=");
    b.append(this.updated);
    b.append("]");
    return NullCheck.notNull(b.toString());
  }
}
