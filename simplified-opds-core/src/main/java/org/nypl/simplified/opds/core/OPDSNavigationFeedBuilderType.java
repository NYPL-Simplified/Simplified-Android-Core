package org.nypl.simplified.opds.core;


/**
 * The type of mutable builders for {@link OPDSNavigationFeed} values.
 */

public interface OPDSNavigationFeedBuilderType
{
  void addEntry(
    OPDSNavigationFeedEntry e);

  OPDSNavigationFeed build();
}
