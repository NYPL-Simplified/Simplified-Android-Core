package org.nypl.simplified.ui.catalog

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.nypl.simplified.books.covers.BookCoverProviderType
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.feeds.api.FeedGroup
import java.net.URI

/**
 * A `ViewHolder` that represents a single swimlane within the [CatalogFeedWithGroupsAdapter].
 */
class CatalogFeedWithGroupsLaneViewHolder(
  private val parent: View,
  private val coverLoader: BookCoverProviderType,
  private val onFeedSelected: (title: String, uri: URI) -> Unit,
  private val onBookSelected: (FeedEntry.FeedEntryOPDS) -> Unit
) : RecyclerView.ViewHolder(parent) {

  private val title =
    this.parent.findViewById<TextView>(R.id.feedLaneTitle)
  private val scrollView =
    this.parent.findViewById<RecyclerView>(R.id.feedLaneCoversScroll)

  init {
    scrollView.apply {
      setHasFixedSize(true)
      layoutManager = LinearLayoutManager(
        this.context, LinearLayoutManager.HORIZONTAL, false
      )
      addItemDecoration(
        SpaceItemDecoration(
          this.resources.getDimensionPixelSize(R.dimen.catalogFeedCoversSpace)
        )
      )
    }
  }

  fun bindTo(group: FeedGroup) {
    this.title.text = group.groupTitle
    this.title.setOnClickListener {
      this.onFeedSelected.invoke(group.groupTitle, group.groupURI)
    }

    /*
     * If the group is empty, there isn't much we can do.
     */

    if (group.groupEntries.isEmpty()) {
      this.scrollView.adapter = null
      return
    }

    /*
     * Populate our feed with our book covers
     */

    val filtered = group.groupEntries.filterIsInstance<FeedEntry.FeedEntryOPDS>()
    this.scrollView.adapter = CatalogLaneAdapter(
      filtered, coverLoader, onBookSelected
    )
  }

  fun unbind() {
    this.scrollView.adapter = null
  }
}
