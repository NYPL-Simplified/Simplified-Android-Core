package org.nypl.simplified.ui.catalog

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.nypl.simplified.books.covers.BookCoverProviderType
import org.nypl.simplified.feeds.api.FeedEntry
import org.slf4j.LoggerFactory

/**
 * An adapter that handles views for paged lists. This is essentially responsible for
 * configuring the views of infinitely-scrolling feeds.
 */

class CatalogPagedAdapter(
  private val context: Context,
  private val listener: CatalogPagedViewListener,
  private val buttonCreator: CatalogButtons,
  private val bookCovers: BookCoverProviderType,
) : PagedListAdapter<FeedEntry, CatalogPagedViewHolder>(CatalogPagedAdapterDiffing.comparisonCallback) {

  private val logger =
    LoggerFactory.getLogger(CatalogPagedAdapter::class.java)

  private var viewHolders = 0

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): CatalogPagedViewHolder {
    ++this.viewHolders
    this.logger.trace("creating view holder ($viewHolders)")

    return CatalogPagedViewHolder(
      context = this.context,
      listener = this.listener,
      parent = LayoutInflater.from(parent.context).inflate(R.layout.book_cell, parent, false),
      buttonCreator = this.buttonCreator,
      bookCovers = this.bookCovers,
    )
  }

  override fun onBindViewHolder(holder: CatalogPagedViewHolder, position: Int) {
    holder.bindTo(this.getItem(position))
  }

  override fun onViewRecycled(holder: CatalogPagedViewHolder) {
    super.onViewRecycled(holder)
    holder.unbind()
  }

  override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
    this.logger.trace("detaching from recycler view")

    /*
     * Because individual cells each maintain a subscription to a book status, we want
     * to aggressively unsubscribe the views when the adapter is detached from the recycler view.
     */

    val childCount = recyclerView.childCount
    for (childIndex in 0 until childCount) {
      val holder =
        recyclerView.getChildViewHolder(recyclerView.getChildAt(childIndex))
          as CatalogPagedViewHolder
      holder.unbind()
    }
  }
}
