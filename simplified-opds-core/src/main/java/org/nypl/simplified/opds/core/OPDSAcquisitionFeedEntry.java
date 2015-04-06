package org.nypl.simplified.opds.core;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
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
    private final List<OPDSAcquisition> acquisitions;
    private final List<String>          authors;
    private final List<String>          categories;
    private String                      summary;
    private OptionType<URI>             cover;
    private final String                id;
    private final Calendar              published;
    private OptionType<String>          publisher;
    private String                      subtitle;
    private OptionType<URI>             thumbnail;
    private final String                title;
    private final Calendar              updated;

    private Builder(
      final String in_id,
      final String in_title,
      final Calendar in_updated,
      final Calendar in_published)
    {
      this.id = NullCheck.notNull(in_id);
      this.title = NullCheck.notNull(in_title);
      this.updated = NullCheck.notNull(in_updated);
      this.summary = "";
      this.thumbnail = Option.none();
      this.cover = Option.none();
      this.acquisitions = new ArrayList<OPDSAcquisition>();
      this.subtitle = "";
      this.authors = new ArrayList<String>();
      this.published = NullCheck.notNull(in_published);
      this.publisher = Option.none();
      this.categories = new ArrayList<String>();
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
      final String c)
    {
      this.categories.add(NullCheck.notNull(c));
    }

    @Override public OPDSAcquisitionFeedEntry build()
    {
      return new OPDSAcquisitionFeedEntry(
        this.authors,
        this.acquisitions,
        this.cover,
        this.id,
        this.title,
        this.thumbnail,
        this.updated,
        this.subtitle,
        this.summary,
        this.published,
        this.publisher,
        this.categories);
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

    @Override public void setCoverOption(
      final OptionType<URI> uri)
    {
      this.cover = NullCheck.notNull(uri);
    }

    @Override public void setPublisherOption(
      final OptionType<String> pub)
    {
      this.publisher = NullCheck.notNull(pub);
    }

    @Override public void setSubtitleOption(
      final OptionType<String> text)
    {
      if (text.isNone()) {
        this.subtitle = "";
      } else {
        this.subtitle = ((Some<String>) text).get();
      }
    }

    @Override public void setThumbnailOption(
      final OptionType<URI> uri)
    {
      this.thumbnail = NullCheck.notNull(uri);
    }
  }

  private static final long serialVersionUID = -8647949453454680335L;

  public static OPDSAcquisitionFeedEntryBuilderType newBuilder(
    final String in_id,
    final String in_title,
    final Calendar in_updated,
    final Calendar in_published)
  {
    return new Builder(in_id, in_title, in_updated, in_published);
  }

  private final List<OPDSAcquisition> acquisitions;
  private final List<String>          authors;
  private final List<String>          categories;
  private final String                summary;
  private final OptionType<URI>       cover;
  private final String                id;
  private final Calendar              published;
  private final OptionType<String>    publisher;
  private final String                subtitle;
  private final OptionType<URI>       thumbnail;
  private final String                title;
  private final Calendar              updated;

  private OPDSAcquisitionFeedEntry(
    final List<String> in_authors,
    final List<OPDSAcquisition> in_acquisitions,
    final OptionType<URI> in_cover,
    final String in_id,
    final String in_title,
    final OptionType<URI> in_thumbnail,
    final Calendar in_updated,
    final String in_subtitle,
    final String in_summary,
    final Calendar in_published,
    final OptionType<String> in_publisher,
    final List<String> in_categories)
  {
    this.authors =
      NullCheck.notNull(Collections.unmodifiableList(in_authors));
    this.acquisitions =
      NullCheck.notNull(Collections.unmodifiableList(in_acquisitions));
    this.cover = NullCheck.notNull(in_cover);
    this.id = NullCheck.notNull(in_id);
    this.title = NullCheck.notNull(in_title);
    this.thumbnail = NullCheck.notNull(in_thumbnail);
    this.updated = NullCheck.notNull(in_updated);
    this.subtitle = NullCheck.notNull(in_subtitle);
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
      && this.categories.equals(other.categories)
      && this.cover.equals(other.cover)
      && this.id.equals(other.id)
      && this.subtitle.equals(other.subtitle)
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

  public List<String> getCategories()
  {
    return this.categories;
  }

  public String getSummary()
  {
    return this.summary;
  }

  public OptionType<URI> getCover()
  {
    return this.cover;
  }

  public String getID()
  {
    return this.id;
  }

  public Calendar getPublished()
  {
    return this.published;
  }

  public OptionType<String> getPublisher()
  {
    return this.publisher;
  }

  public String getSubtitle()
  {
    return this.subtitle;
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
    result = (prime * result) + this.cover.hashCode();
    result = (prime * result) + this.categories.hashCode();
    result = (prime * result) + this.id.hashCode();
    result = (prime * result) + this.subtitle.hashCode();
    result = (prime * result) + this.summary.hashCode();
    result = (prime * result) + this.thumbnail.hashCode();
    result = (prime * result) + this.title.hashCode();
    result = (prime * result) + this.updated.hashCode();
    result = (prime * result) + this.published.hashCode();
    result = (prime * result) + this.publisher.hashCode();
    return result;
  }
}
