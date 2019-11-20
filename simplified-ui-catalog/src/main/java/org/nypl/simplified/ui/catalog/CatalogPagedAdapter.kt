package org.nypl.simplified.ui.catalog

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.nypl.simplified.books.covers.BookCoverProviderType
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.futures.FluentFutureExtensions.map
import org.nypl.simplified.ui.thread.api.UIThreadServiceType

/**
 * An adapter that handles views for paged lists. This is essentially responsible for
 * configuring the views of infinitely-scrolling feeds.
 */

class CatalogPagedAdapter(
  private val context: Context,
  private val covers: BookCoverProviderType,
  private val uiThread: UIThreadServiceType
) : PagedListAdapter<FeedEntry, CatalogPagedAdapter.ViewHolder>(CatalogPagedAdapterDiffing.comparisonCallback) {

  private val shortAnimationDuration =
    this.context.resources.getInteger(android.R.integer.config_shortAnimTime)

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): ViewHolder {
    return this.ViewHolder(LayoutInflater.from(parent.context)
      .inflate(R.layout.book_cell, parent, false))
  }

  override fun onBindViewHolder(
    holder: ViewHolder,
    position: Int
  ) {
    return when (val item = this.getItem(position)) {
      is FeedEntry.FeedEntryCorrupt -> {

      }

      is FeedEntry.FeedEntryOPDS -> {
        holder.loading.visibility = View.INVISIBLE
        holder.idle.visibility = View.VISIBLE

        holder.idleCover.visibility = View.INVISIBLE
        holder.idleProgress.visibility = View.VISIBLE
        holder.idleText.text = item.feedEntry.title

        this.covers.loadThumbnailInto(
          item,
          holder.idleCover,
          holder.idleCover.layoutParams.width,
          holder.idleCover.layoutParams.height
        ).map {
          this.uiThread.runOnUIThread {
            holder.idleProgress.visibility = View.INVISIBLE
            holder.idleCover.visibility = View.VISIBLE
            holder.idleCover.alpha = 0.0f
            holder.idleCover.animate()
              .alpha(1f)
              .setDuration(this.shortAnimationDuration.toLong())
              .setListener(null)
          }
        }
        Unit
      }

      null -> {
        holder.loading.visibility = View.VISIBLE
        holder.idle.visibility = View.INVISIBLE
      }
    }
  }

  inner class ViewHolder(val parent: View) : RecyclerView.ViewHolder(parent) {
    val idle =
      this.parent.findViewById<ViewGroup>(R.id.bookCellIdle)
    val idleCover =
      this.parent.findViewById<ImageView>(R.id.bookCellCover)
    val idleProgress =
      this.parent.findViewById<ProgressBar>(R.id.bookCellCoverProgress)
    val idleText =
      this.idle.findViewById<TextView>(R.id.bookCellTitle)

    val loading =
      this.parent.findViewById<ViewGroup>(R.id.bookCellEndLoading)
    val loadingProgress =
      this.loading.findViewById<ProgressBar>(R.id.bookCellEndLoadingProgress)
  }

  companion object {

  }
}