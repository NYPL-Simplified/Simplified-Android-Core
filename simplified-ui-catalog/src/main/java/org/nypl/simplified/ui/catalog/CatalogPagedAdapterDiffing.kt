package org.nypl.simplified.ui.catalog

import androidx.recyclerview.widget.DiffUtil
import org.nypl.simplified.feeds.api.FeedEntry

/**
 * Functions to compare feed entries for paged adapters.
 */

object CatalogPagedAdapterDiffing {

  val comparisonCallback =
    object : DiffUtil.ItemCallback<FeedEntry>() {
      override fun areItemsTheSame(
        oldItem: FeedEntry,
        newItem: FeedEntry
      ): Boolean {
        return oldItem == newItem
      }

      override fun areContentsTheSame(
        oldItem: FeedEntry,
        newItem: FeedEntry
      ): Boolean {
        return oldItem == newItem
      }
    }

}