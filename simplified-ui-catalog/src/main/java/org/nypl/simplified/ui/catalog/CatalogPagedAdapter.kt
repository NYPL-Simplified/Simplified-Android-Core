package org.nypl.simplified.ui.catalog

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.disposables.CompositeDisposable
import org.nypl.simplified.books.book_registry.BookRegistryReadableType
import org.nypl.simplified.books.covers.BookCoverProviderType
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.ui.thread.api.UIThreadServiceType
import org.slf4j.LoggerFactory

/**
 * An adapter that handles views for paged lists. This is essentially responsible for
 * configuring the views of infinitely-scrolling feeds.
 */

class CatalogPagedAdapter(
  private val bookCovers: BookCoverProviderType,
  private val bookRegistry: BookRegistryReadableType,
  private val buttonCreator: CatalogButtons,
  private val context: Context,
  private val fragmentManager: FragmentManager,
  private val loginViewModel: CatalogLoginViewModel,
  private val onBookSelected: (FeedEntry.FeedEntryOPDS) -> Unit,
  private val profilesController: ProfilesControllerType,
  private val uiThread: UIThreadServiceType
) : PagedListAdapter<FeedEntry, CatalogPagedViewHolder>(CatalogPagedAdapterDiffing.comparisonCallback) {

  private val logger =
    LoggerFactory.getLogger(CatalogPagedAdapter::class.java)

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
      bookCovers = this.bookCovers,
      bookRegistry = this.bookRegistry,
      buttonCreator = this.buttonCreator,
      compositeDisposable = this.compositeDisposable,
      context = this.context,
      fragmentManager = this.fragmentManager,
      loginViewModel = this.loginViewModel,
      onBookSelected = this.onBookSelected,
      parent = LayoutInflater.from(parent.context).inflate(R.layout.book_cell, parent, false),
      profilesController = this.profilesController,
      uiThread = this.uiThread
    )
  }

  override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
    this.logger.trace("detaching from recycler view")

    /*
     * Because individual cells each maintain a subscription to the book registry, we want
     * to aggressively unsubscribe the views when the adapter is detached from the recycler view.
     */

    this.compositeDisposable.dispose()
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