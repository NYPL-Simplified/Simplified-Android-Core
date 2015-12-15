package org.nypl.simplified.opds.core;

import com.io7m.jfunctional.OptionType;

import java.net.URI;
import java.util.Calendar;
import java.util.List;

/**
 * The type of mutable builders for {@link OPDSAcquisitionFeedEntry} values.
 */

public interface OPDSAcquisitionFeedEntryBuilderType
{
  /**
   * Add an acquisition.
   *
   * @param a The acquisition
   */

  void addAcquisition(
    OPDSAcquisition a);

  /**
   * Set the author.
   *
   * @param name The author
   */

  void addAuthor(
    final String name);

  /**
   * Add a category.
   *
   * @param c The category
   */

  void addCategory(
    final OPDSCategory c);

  /**
   * Add a group.
   *
   * @param uri The group URI
   * @param b   The group name
   */

  void addGroup(
    final URI uri,
    final String b);

  /**
   * @return An entry based on all of the given values
   */

  OPDSAcquisitionFeedEntry build();

  /**
   * @return A list of the current acquisitions
   */

  List<OPDSAcquisition> getAcquisitions();

  /**
   * Set the availability.
   *
   * @param a The availability
   */

  void setAvailability(
    OPDSAvailabilityType a);

  /**
   * Set the cover.
   *
   * @param uri The cover URI
   */

  void setCoverOption(
    OptionType<URI> uri);

  /**
   * Set the report issues URI.
   *
   * @param uri The report issues URI
   */

  void setIssuesOption(
    OptionType<URI> uri);

  /**
   * Set the publication date.
   *
   * @param pub The publication date
   */

  void setPublishedOption(
    OptionType<Calendar> pub);

  /**
   * Set the publisher.
   *
   * @param pub The publisher
   */

  void setPublisherOption(
    OptionType<String> pub);

  /**
   * Set the summary.
   *
   * @param text The summary
   */

  void setSummaryOption(
    OptionType<String> text);

  /**
   * Set the thumbnail.
   *
   * @param uri The thumbnail
   */

  void setThumbnailOption(
    OptionType<URI> uri);
}
