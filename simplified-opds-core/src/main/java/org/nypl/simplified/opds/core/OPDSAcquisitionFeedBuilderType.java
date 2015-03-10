package org.nypl.simplified.opds.core;

import java.net.URI;

import com.io7m.jfunctional.OptionType;

/**
 * The type of mutable builders for {@link OPDSAcquisitionFeed} values.
 */

public interface OPDSAcquisitionFeedBuilderType
{
  void addEntry(
    OPDSAcquisitionFeedEntry build);

  OPDSAcquisitionFeed build();

  void setNextOption(
    final OptionType<URI> next);
}
