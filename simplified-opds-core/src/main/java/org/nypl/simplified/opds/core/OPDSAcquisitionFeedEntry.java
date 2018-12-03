package org.nypl.simplified.opds.core;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Pair;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The type of entries in acquisition feeds.
 */

@SuppressWarnings("synthetic-access")
public final class OPDSAcquisitionFeedEntry implements Serializable
{
  private static final long serialVersionUID = 1L;
  private final List<OPDSAcquisition>  acquisitions;
  private final List<String>           authors;
  private final OPDSAvailabilityType   availability;
  private final List<OPDSCategory>     categories;
  private final OptionType<URI>        cover;
  private final OptionType<URI>        annotations;
  private final Set<Pair<String, URI>> groups;
  private final String                 id;
  private final OptionType<URI>        issues;
  private final OptionType<URI>        related;
  private final OptionType<Calendar>   published;
  private final OptionType<String>     publisher;
  private final String                 distribution;
  private final String                 summary;
  private final OptionType<URI>        thumbnail;
  private final String                 title;
  private final Calendar               updated;
  private final OptionType<URI>        alternate;
  private final OptionType<URI>        analytics;
  private final OptionType<DRMLicensor> licensor;

  private OPDSAcquisitionFeedEntry(
    final List<String> in_authors,
    final List<OPDSAcquisition> in_acquisitions,
    final OPDSAvailabilityType in_availability,
    final Set<Pair<String, URI>> in_groups,
    final OptionType<URI> in_cover,
    final OptionType<URI> in_annotations,
    final String in_id,
    final OptionType<URI> in_issues,
    final OptionType<URI> in_related,
    final String in_title,
    final OptionType<URI> in_thumbnail,
    final Calendar in_updated,
    final String in_summary,
    final OptionType<Calendar> in_published,
    final OptionType<String> in_publisher,
    final String in_distribution,
    final List<OPDSCategory> in_categories,
    final OptionType<URI> in_alternate,
    final OptionType<URI> in_analytics,
    final OptionType<DRMLicensor> in_licensor)
  {
    this.authors = NullCheck.notNull(Collections.unmodifiableList(in_authors));
    this.acquisitions =
      NullCheck.notNull(Collections.unmodifiableList(in_acquisitions));
    this.availability = NullCheck.notNull(in_availability);
    this.groups = NullCheck.notNull(in_groups);
    this.cover = NullCheck.notNull(in_cover);
    this.annotations = NullCheck.notNull(in_annotations);
    this.id = NullCheck.notNull(in_id);
    this.issues = NullCheck.notNull(in_issues);
    this.related = NullCheck.notNull(in_related);
    this.title = NullCheck.notNull(in_title);
    this.thumbnail = NullCheck.notNull(in_thumbnail);
    this.updated = NullCheck.notNull(in_updated);
    this.summary = NullCheck.notNull(in_summary);
    this.published = NullCheck.notNull(in_published);
    this.publisher = NullCheck.notNull(in_publisher);
    this.distribution = NullCheck.notNull(in_distribution);
    this.categories = NullCheck.notNull(in_categories);
    this.alternate = NullCheck.notNull(in_alternate);
    this.analytics = NullCheck.notNull(in_analytics);
    this.licensor = NullCheck.notNull(in_licensor);
  }

  /**
   * Construct a new mutable builder for feed entries.
   *
   * @param in_id           The feed ID
   * @param in_title        The feed title
   * @param in_updated      The feed updated time
   * @param in_availability The availability
   *
   * @return A new builder
   */

  public static OPDSAcquisitionFeedEntryBuilderType newBuilder(
    final String in_id,
    final String in_title,
    final Calendar in_updated,
    final OPDSAvailabilityType in_availability)
  {
    return new Builder(in_id, in_title, in_updated, in_availability);
  }

  /**
   * Construct a new mutable builder for feed entries. The builder will be
   * initialized with the values of the existing entry {@code e}.
   *
   * @param e An existing entry
   *
   * @return A new builder
   */

  public static OPDSAcquisitionFeedEntryBuilderType newBuilderFrom(
    final OPDSAcquisitionFeedEntry e)
  {
    final Builder b = new Builder(
      e.getID(), e.getTitle(), e.getUpdated(), e.getAvailability());

    for (final OPDSAcquisition a : e.getAcquisitions()) {
      b.addAcquisition(a);
    }

    for (final Pair<String, URI> a : e.getGroups()) {
      b.addGroup(a.getRight(), a.getLeft());
    }

    for (final String a : e.getAuthors()) {
      b.addAuthor(a);
    }

    b.setAvailability(e.getAvailability());

    for (final OPDSCategory c : e.getCategories()) {
      b.addCategory(c);
    }

    b.setCoverOption(e.getCover());
    b.setIssuesOption(e.getIssues());
    b.setAnnotationsOption(e.getAnnotations());
    b.setRelatedOption(e.getRelated());
    b.setPublishedOption(e.getPublished());
    b.setPublisherOption(e.getPublisher());
    b.setDistribution(e.getDistribution());
    b.setAlternateOption(e.getAlternate());
    b.setAnalyticsOption(e.getAnalytics());
    b.setLicensorOption(e.getLicensor());

    {
      final String summary = e.getSummary();
      if (!summary.isEmpty()) {
        b.setSummaryOption(Option.some(summary));
      }
    }


    b.setThumbnailOption(e.getThumbnail());
    return b;
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
           && this.availability.equals(other.availability)
           && this.authors.equals(other.authors)
           && this.groups.equals(other.groups)
           && this.categories.equals(other.categories)
           && this.cover.equals(other.cover)
           && this.alternate.equals(other.alternate)
           && this.analytics.equals(other.analytics)
           && this.annotations.equals(other.annotations)
           && this.id.equals(other.id)
           && this.issues.equals(other.issues)
           && this.related.equals(other.related)
           && this.summary.equals(other.summary)
           && this.thumbnail.equals(other.thumbnail)
           && this.title.equals(other.title)
           && this.updated.equals(other.updated)
           && this.published.equals(other.published)
           && this.publisher.equals(other.publisher)
           && this.licensor.equals(other.licensor)
           && this.distribution.equals(other.distribution);
  }

  /**
   * @return The list of acquisitions
   */

  public List<OPDSAcquisition> getAcquisitions()
  {
    return this.acquisitions;
  }

  /**
   * @return The list of authors
   */

  public List<String> getAuthors()
  {
    return this.authors;
  }

  /**
   * @return The entry availability
   */

  public OPDSAvailabilityType getAvailability()
  {
    return this.availability;
  }

  /**
   * @return The list of categories
   */

  public List<OPDSCategory> getCategories()
  {
    return this.categories;
  }

  /**
   * @return The cover image
   */

  public OptionType<URI> getCover()
  {
    return this.cover;
  }


  /**
   * @return the annotations url
   */

  public OptionType<URI> getAnnotations()
  {
    return this.annotations;
  }

  /**
   * @return alternate url
   */
  public OptionType<URI> getAlternate()
  {
    return this.alternate;
  }

  /**
   * @return analytics url
   */
  public OptionType<URI> getAnalytics()
  {
    return this.analytics;
  }

  /**
   * @return The report issues URI
   */

  public OptionType<URI> getIssues()
  {
    return this.issues;
  }

  /**
   * @return The related feed url
   */

  public OptionType<URI> getRelated()
  {
    return this.related;
  }

  /**
   * @return The groups
   */

  public Set<Pair<String, URI>> getGroups()
  {
    return this.groups;
  }

  /**
   * @return The entry ID
   */

  public String getID()
  {
    return this.id;
  }

  /**
   * @return The entry publication date, if any
   */

  public OptionType<Calendar> getPublished()
  {
    return this.published;
  }

  /**
   * @return The publisher, if any
   */

  public OptionType<String> getPublisher()
  {
    return this.publisher;
  }

  /**
   * @return The distribution, if any
   */

  public String getDistribution()
  {
    return this.distribution;
  }

  /**
   * @return The summary
   */

  public String getSummary()
  {
    return this.summary;
  }

  /**
   * @return The thumbnail, if any
   */

  public OptionType<URI> getThumbnail()
  {
    return this.thumbnail;
  }

  /**
   * @return The licensor
   */
  public OptionType<DRMLicensor> getLicensor()
  {
    return this.licensor;
  }

  /**
   * @return The title
   */

  public String getTitle()
  {
    return this.title;
  }

  /**
   * @return The time of the last update
   */

  public Calendar getUpdated()
  {
    return this.updated;
  }

  /**
   * @return The authors as a comma separated string
   */

  public String getAuthorsCommaSeparated()
  {
    final StringBuilder sb = new StringBuilder();
    final List<String> author_list = this.getAuthors();
    final int max = author_list.size();
    for (int index = 0; index < max; ++index) {
      final String author = NullCheck.notNull(author_list.get(index));
      sb.append(author);
      if ((index + 1) < max) {
        sb.append(", ");
      }
    }
    return sb.toString();
  }

  @Override public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = (prime * result) + this.acquisitions.hashCode();
    result = (prime * result) + this.availability.hashCode();
    result = (prime * result) + this.authors.hashCode();
    result = (prime * result) + this.groups.hashCode();
    result = (prime * result) + this.cover.hashCode();
    result = (prime * result) + this.alternate.hashCode();
    result = (prime * result) + this.analytics.hashCode();
    result = (prime * result) + this.annotations.hashCode();
    result = (prime * result) + this.categories.hashCode();
    result = (prime * result) + this.id.hashCode();
    result = (prime * result) + this.issues.hashCode();
    result = (prime * result) + this.related.hashCode();
    result = (prime * result) + this.summary.hashCode();
    result = (prime * result) + this.thumbnail.hashCode();
    result = (prime * result) + this.title.hashCode();
    result = (prime * result) + this.updated.hashCode();
    result = (prime * result) + this.published.hashCode();
    result = (prime * result) + this.publisher.hashCode();
    result = (prime * result) + this.distribution.hashCode();
    result = (prime * result) + this.licensor.hashCode();
    return result;
  }

  @Override public String toString()
  {
    final StringBuilder b = new StringBuilder(128);
    b.append("[OPDSAcquisitionFeedEntry acquisitions=");
    b.append(this.acquisitions);
    b.append(", authors=");
    b.append(this.authors);
    b.append(", availability=");
    b.append(this.availability);
    b.append(", categories=");
    b.append(this.categories);
    b.append(", cover=");
    b.append(this.cover);
    b.append(", alternate=");
    b.append(this.alternate);
    b.append(", analytics=");
    b.append(this.analytics);
    b.append(", annotations=");
    b.append(this.annotations);
    b.append(", groups=");
    b.append(this.groups);
    b.append(", id=");
    b.append(this.id);
    b.append(", issues=");
    b.append(this.issues);
    b.append(", related=");
    b.append(this.related);
    b.append(", published=");
    b.append(this.published);
    b.append(", publisher=");
    b.append(this.publisher);
    b.append(", distribution=");
    b.append(this.distribution);
    b.append(", summary=");
    b.append(this.summary);
    b.append(", thumbnail=");
    b.append(this.thumbnail);
    b.append(", title=");
    b.append(this.title);
    b.append(", updated=");
    b.append(this.updated);
    b.append(", licensor=");
    b.append(this.licensor);
    b.append("]");
    return NullCheck.notNull(b.toString());
  }

  private static final class Builder
    implements OPDSAcquisitionFeedEntryBuilderType
  {
    private final List<OPDSAcquisition>  acquisitions;
    private final List<String>           authors;
    private final List<OPDSCategory>     categories;
    private final Set<Pair<String, URI>> groups;
    private final String                 id;
    private final String                 title;
    private final Calendar               updated;
    private       OPDSAvailabilityType   availability;
    private       OptionType<URI>        cover;
    private       OptionType<URI>        alternate;
    private       OptionType<URI>        analytics;
    private       OptionType<URI>        annotations;
    private       OptionType<URI>        issues;
    private       OptionType<URI>        related;
    private       OptionType<Calendar>   published;
    private       OptionType<String>     publisher;
    private       String                 distribution;
    private       String                 summary;
    private       OptionType<URI>        thumbnail;
    private       OptionType<DRMLicensor>        licensor;

    private Builder(
      final String in_id,
      final String in_title,
      final Calendar in_updated,
      final OPDSAvailabilityType in_availability)
    {
      this.id = NullCheck.notNull(in_id);
      this.issues = Option.none();
      this.related = Option.none();
      this.title = NullCheck.notNull(in_title);
      this.updated = NullCheck.notNull(in_updated);
      this.availability = NullCheck.notNull(in_availability);
      this.summary = "";
      this.thumbnail = Option.none();
      this.cover = Option.none();
      this.alternate = Option.none();
      this.analytics = Option.none();
      this.annotations = Option.none();
      this.acquisitions = new ArrayList<OPDSAcquisition>(8);
      this.authors = new ArrayList<String>(4);
      this.published = Option.none();
      this.publisher = Option.none();
      this.distribution = "";
      this.categories = new ArrayList<OPDSCategory>(8);
      this.groups = new HashSet<Pair<String, URI>>(8);
      this.licensor = Option.none();
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
        this.availability,
        this.groups,
        this.cover,
        this.annotations,
        this.id,
        this.issues,
        this.related,
        this.title,
        this.thumbnail,
        this.updated,
        this.summary,
        this.published,
        this.publisher,
        this.distribution,
        this.categories,
        this.alternate,
        this.analytics,
        this.licensor);
    }

    @Override public List<OPDSAcquisition> getAcquisitions()
    {
      return this.acquisitions;
    }

    @Override public void setAvailability(
      final OPDSAvailabilityType a)
    {
      this.availability = NullCheck.notNull(a);
    }

    @Override public void setCoverOption(
      final OptionType<URI> uri)
    {
      this.cover = NullCheck.notNull(uri);
    }


    @Override public void setAlternateOption(
      final OptionType<URI> uri)
    {
      this.alternate = NullCheck.notNull(uri);
    }

    @Override public void setAnalyticsOption(
      final OptionType<URI> uri)
    {
      this.analytics = NullCheck.notNull(uri);
    }

    @Override public void setAnnotationsOption(
      final OptionType<URI> uri)
    {
      this.annotations = NullCheck.notNull(uri);
    }

    @Override public void setIssuesOption(
      final OptionType<URI> uri)
    {
      this.issues = NullCheck.notNull(uri);
    }

    @Override public void setRelatedOption(
      final OptionType<URI> uri)
    {
      this.related = NullCheck.notNull(uri);
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

    @Override public void setDistribution(
      final String dist)
    {
      this.distribution = NullCheck.notNull(dist);
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

    @Override public void setLicensorOption(
      final OptionType<DRMLicensor> lic)
    {
      this.licensor = NullCheck.notNull(lic);
    }
  }
}
