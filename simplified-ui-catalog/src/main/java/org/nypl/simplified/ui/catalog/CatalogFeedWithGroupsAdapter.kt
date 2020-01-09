package org.nypl.simplified.ui.catalog

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.nypl.simplified.books.covers.BookCoverProviderType
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.feeds.api.FeedGroup
import org.nypl.simplified.ui.thread.api.UIThreadServiceType
import org.slf4j.LoggerFactory
import java.net.URI

/**
 * An adapter that produces swimlanes for feeds that have groups.
 */

class CatalogFeedWithGroupsAdapter(
  private val groups: List<FeedGroup>,
  private val uiThread: UIThreadServiceType,
  private val coverLoader: BookCoverProviderType,
  private val onFeedSelected: (title: String, uri: URI) -> Unit,
  private val onBookSelected: (FeedEntry.FeedEntryOPDS) -> Unit
) : RecyclerView.Adapter<CatalogFeedWithGroupsLaneViewHolder>() {

  private val logger =
    LoggerFactory.getLogger(CatalogFeedWithGroupsAdapter::class.java)

  private var viewHolders = 0

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): CatalogFeedWithGroupsLaneViewHolder {
    ++this.viewHolders
    this.logger.trace("creating view holder ($viewHolders)")

    val inflater =
      LayoutInflater.from(parent.context)
    val item =
      inflater.inflate(R.layout.feed_lane, parent, false)

    return CatalogFeedWithGroupsLaneViewHolder(
      parent = item,
      uiThread = this.uiThread,
      coverLoader = this.coverLoader,
      onFeedSelected = this.onFeedSelected,
      onBookSelected = this.onBookSelected
    )
  }

  override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
    this.logger.trace("detaching from recycler view")

    val childCount = recyclerView.childCount
    for (childIndex in 0 until childCount) {
      val holder =
        recyclerView.getChildViewHolder(recyclerView.getChildAt(childIndex))
          as CatalogFeedWithGroupsLaneViewHolder
      holder.unbind()
    }
  }

  override fun getItemCount(): Int =
    this.groups.size

  override fun onBindViewHolder(
    holder: CatalogFeedWithGroupsLaneViewHolder,
    position: Int
  ) {
    holder.bindTo(this.groups[position])
  }
}
