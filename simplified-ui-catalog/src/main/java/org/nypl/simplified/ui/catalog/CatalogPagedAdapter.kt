package org.nypl.simplified.ui.catalog

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.disposables.CompositeDisposable
import org.librarysimplified.services.api.ServiceDirectoryType
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.listeners.api.FragmentListenerType
import org.slf4j.LoggerFactory

/**
 * An adapter that handles views for paged lists. This is essentially responsible for
 * configuring the views of infinitely-scrolling feeds.
 */

class CatalogPagedAdapter(
  private val borrowViewModel: CatalogBorrowViewModel,
  private val buttonCreator: CatalogButtons,
  private val context: FragmentActivity,
  private val listener: FragmentListenerType<CatalogFeedEvent>,
  private val onBookSelected: (FeedEntry.FeedEntryOPDS) -> Unit,
  private val services: ServiceDirectoryType,
  private val ownership: CatalogFeedOwnership
) : PagedListAdapter<FeedEntry, CatalogPagedViewHolder>(CatalogPagedAdapterDiffing.comparisonCallback) {

  private val logger =
    LoggerFactory.getLogger(CatalogPagedAdapter::class.java)

  private var viewHolders = 0

  private val registrySubscriptions =
    CompositeDisposable()

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): CatalogPagedViewHolder {
    ++this.viewHolders
    this.logger.trace("creating view holder ($viewHolders)")

    return CatalogPagedViewHolder(
      borrowViewModel = this.borrowViewModel,
      buttonCreator = this.buttonCreator,
      context = this.context,
      listener = this.listener,
      onBookSelected = this.onBookSelected,
      parent = LayoutInflater.from(parent.context).inflate(R.layout.book_cell, parent, false),
      registrySubscriptions = this.registrySubscriptions,
      services = this.services,
      ownership = this.ownership
    )
  }

  override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
    this.logger.trace("detaching from recycler view")

    /*
     * Because individual cells each maintain a subscription to the book registry, we want
     * to aggressively unsubscribe the views when the adapter is detached from the recycler view.
     */

    this.registrySubscriptions.dispose()
    val childCount = recyclerView.childCount
    for (childIndex in 0 until childCount) {
      val holder =
        recyclerView.getChildViewHolder(recyclerView.getChildAt(childIndex))
          as CatalogPagedViewHolder
      holder.unbind()
    }
  }

  override fun onBindViewHolder(
    holder: CatalogPagedViewHolder,
    position: Int
  ) {
    holder.bindTo(this.getItem(position))
  }
}
