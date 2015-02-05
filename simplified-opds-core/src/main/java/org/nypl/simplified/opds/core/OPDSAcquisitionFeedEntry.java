package org.nypl.simplified.opds.core;

import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;

/**
 * The type of entries in acquisition feeds.
 */

@SuppressWarnings("synthetic-access") public final class OPDSAcquisitionFeedEntry
{
  private static final class Builder implements
    OPDSAcquisitionFeedEntryBuilderType
  {
    private final List<OPDSAcquisition> acquisitions;
    private final List<String>          authors;
    private OptionType<URI>             cover;
    private final String                id;
    private String                      subtitle;
    private String                      summary;
    private OptionType<URI>             thumbnail;
    private final String                title;
    private final Calendar              updated;

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
      this.subtitle = "";
      this.authors = new ArrayList<String>();
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
        this.summary);
    }

    @Override public void setCoverOption(
      final OptionType<URI> uri)
    {
      this.cover = NullCheck.notNull(uri);
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

  public static OPDSAcquisitionFeedEntryBuilderType newBuilder(
    final String in_id,
    final String in_title,
    final Calendar in_updated)
  {
    return new Builder(in_id, in_title, in_updated);
  }

  private final List<OPDSAcquisition> acquisitions;
  private final List<String>          authors;
  private final OptionType<URI>       cover;
  private final String                id;
  private final String                subtitle;
  private final String                summary;
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
    final String in_summary)
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
  }

  public List<OPDSAcquisition> getAcquisitions()
  {
    return this.acquisitions;
  }

  public List<String> getAuthors()
  {
    return this.authors;
  }

  public OptionType<URI> getCover()
  {
    return this.cover;
  }

  public String getID()
  {
    return this.id;
  }

  public String getSubtitle()
  {
    return this.subtitle;
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
}
