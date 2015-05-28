package org.nypl.simplified.opds.core;

import java.net.URI;
import java.util.Calendar;

import com.io7m.jfunctional.OptionType;

/**
 * The type of mutable builders for {@link OPDSAcquisitionFeedEntry} values.
 */

public interface OPDSAcquisitionFeedEntryBuilderType
{
  void addAcquisition(
    OPDSAcquisition a);

  void addAuthor(
    final String name);

  void addGroup(
    final URI uri,
    final String b);

  void addCategory(
    final OPDSCategory c);

  OPDSAcquisitionFeedEntry build();

  void setCoverOption(
    OptionType<URI> uri);

  void setPublishedOption(
    OptionType<Calendar> pub);

  void setPublisherOption(
    OptionType<String> pub);

  void setSummaryOption(
    OptionType<String> text);

  void setThumbnailOption(
    OptionType<URI> uri);
}
