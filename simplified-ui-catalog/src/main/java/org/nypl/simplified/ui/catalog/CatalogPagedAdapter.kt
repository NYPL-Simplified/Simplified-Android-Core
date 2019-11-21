package org.nypl.simplified.ui.catalog

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.disposables.CompositeDisposable
import org.nypl.simplified.books.book_registry.BookRegistryReadableType
import org.nypl.simplified.books.covers.BookCoverProviderType
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.ui.thread.api.UIThreadServiceType
import org.slf4j.LoggerFactory

/**
 * An adapter that handles views for paged lists. This is essentially responsible for
 * configuring the views of infinitely-scrolling feeds.
 */

class CatalogPagedAdapter(
  private val bookRegistry: BookRegistryReadableType,
  private val bookCovers: BookCoverProviderType,
  private val context: Context,
  private val uiThread: UIThreadServiceType,
  private val onBookSelected: (FeedEntry.FeedEntryOPDS) -> Unit
) : PagedListAdapter<FeedEntry, CatalogPagedViewHolder>(CatalogPagedAdapterDiffing.comparisonCallback) {

  private val logger =
    LoggerFactory.getLogger(CatalogPagedAdapter::class.java)

  private val shortAnimationDuration =
    this.context.resources.getInteger(android.R.integer.config_shortAnimTime)

  private var viewHolders = 0

  private val compositeDisposable =
    CompositeDisposable()

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): CatalogPagedViewHolder {
    ++this.viewHolders
    this.logger.trace("creating view holder (${viewHolders})")

    return CatalogPagedViewHolder(
      bookRegistry = this.bookRegistry,
      bookCovers = this.bookCovers,
      compositeDisposable = this.compositeDisposable,
      uiThread = this.uiThread,
      parent = LayoutInflater.from(parent.context).inflate(R.layout.book_cell, parent, false),
      shortAnimationDuration = this.shortAnimationDuration,
      onBookSelected = this.onBookSelected
    )
  }

  override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
    this.logger.trace("detaching from recycler view")

    this.compositeDisposable.dispose()
    val childCount = recyclerView.childCount
    for (childIndex in 0 until childCount) {
      val holder =
        recyclerView.getChildViewHolder(recyclerView.getChildAt(childIndex))
          as CatalogPagedViewHolder
      holder.unbind()
    }
  }

  override fun onViewDetachedFromWindow(holder: CatalogPagedViewHolder) {
    holder.unbind()
  }

  override fun onViewRecycled(holder: CatalogPagedViewHolder) {
    this.logger.trace("view recycled")
    holder.unbind()
  }

  override fun onBindViewHolder(
    holder: CatalogPagedViewHolder,
    position: Int
  ) {
    holder.bindTo(this.getItem(position))
  }
}