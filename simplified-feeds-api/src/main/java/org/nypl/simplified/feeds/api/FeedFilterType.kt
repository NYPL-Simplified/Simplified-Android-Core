package org.nypl.simplified.feeds.api

import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry

/**
 * A function that, given an OPDS entry, returns `true` if that entry should appear in
 * feeds.
 */

data class FeedFilter(
  var shouldSeeFeedEntry: (OPDSAcquisitionFeedEntry) -> Boolean
)
