package org.nypl.simplified.app;

import org.nypl.simplified.opds.core.OPDSNavigationFeedEntry;

/**
 * The type of functions evaluated when a user clicks the title of a
 * navigation feed entry.
 */

public interface CatalogNavigationFeedTitleClickListener
{
  void onClick(
    final OPDSNavigationFeedEntry e);
}
