package org.nypl.simplified.ui.catalog

import org.nypl.simplified.feeds.api.FeedEntry
import java.io.Serializable

/**
 * Parameters for a book detail page.
 */

data class CatalogBookDetailFragmentParameters(
  val feedEntry: FeedEntry.FeedEntryOPDS,
  val feedArguments: CatalogFeedArguments
) : Serializable {
  val bookID = this.feedEntry.bookID
}
