package org.nypl.simplified.opds.core;

/**
 * The type of mutable builders for {@link OPDSAcquisitionFeed} values.
 */

public interface OPDSAcquisitionFeedBuilderType
{
  void addEntry(
    OPDSAcquisitionFeedEntry build);

  OPDSAcquisitionFeed build();
}
