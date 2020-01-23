package org.nypl.simplified.ui.catalog

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.nypl.simplified.feeds.api.FeedEntry

/**
 * This adapter displays a list of feed items in a catalog lane.
 *
 * @see CatalogLaneItemViewHolder
 */
class CatalogLaneAdapter(
  private val items: List<FeedEntry.FeedEntryOPDS>,
  private val onBookSelected: (FeedEntry.FeedEntryOPDS) -> Unit
) : RecyclerView.Adapter<CatalogLaneItemViewHolder>() {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CatalogLaneItemViewHolder {
    val view = LayoutInflater.from(parent.context).inflate(R.layout.feed_lane_item, parent, false)
    return CatalogLaneItemViewHolder(view, onBookSelected)
  }

  override fun getItemCount() = items.size

  override fun onBindViewHolder(holder: CatalogLaneItemViewHolder, position: Int) {
    holder.bindTo(items[position])
  }
}
