package org.nypl.simplified.ui.catalog

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Space
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.common.util.concurrent.FluentFuture
import org.nypl.simplified.books.covers.BookCoverProviderType
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.feeds.api.FeedGroup
import org.nypl.simplified.futures.FluentFutureExtensions
import org.nypl.simplified.futures.FluentFutureExtensions.map
import org.nypl.simplified.ui.thread.api.UIThreadServiceType
import java.net.URI

/**
 * An adapter that produces swimlanes for feeds that have groups.
 */

class CatalogFeedWithGroupsAdapter(
  private val context: Context,
  private val groups: List<FeedGroup>,
  private val uiThread: UIThreadServiceType,
  private val coverLoader: BookCoverProviderType,
  private val onFeedSelected: (title: String, uri: URI) -> Unit,
  private val onBookSelected: (FeedEntry.FeedEntryOPDS) -> Unit
) : RecyclerView.Adapter<CatalogFeedWithGroupsAdapter.ViewHolder>() {

  private val shortAnimationDuration =
    this.context.resources.getInteger(android.R.integer.config_shortAnimTime)

  private val endSpaceLayoutParams =
    LinearLayout.LayoutParams(
      this.context.resources.getDimensionPixelSize(R.dimen.catalogFeedCoversEndSpace),
      this.context.resources.getDimensionPixelSize(R.dimen.catalogFeedCoversHeight))

  private val spaceLayoutParams =
    LinearLayout.LayoutParams(
      this.context.resources.getDimensionPixelSize(R.dimen.catalogFeedCoversSpace),
      this.context.resources.getDimensionPixelSize(R.dimen.catalogFeedCoversHeight))

  private val coverLayoutParams =
    LinearLayout.LayoutParams(
      this.context.resources.getDimensionPixelSize(R.dimen.catalogFeedCoversWidth),
      this.context.resources.getDimensionPixelSize(R.dimen.catalogFeedCoversHeight))

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): ViewHolder {
    val inflater =
      LayoutInflater.from(parent.context)
    val item =
      inflater.inflate(R.layout.feed_lane, parent, false)
    return this.ViewHolder(item)
  }

  override fun getItemCount(): Int =
    this.groups.size

  override fun onBindViewHolder(
    holder: ViewHolder,
    position: Int
  ) {
    val group = this.groups[position]

    holder.title.text = group.groupTitle
    holder.title.setOnClickListener {
      this.onFeedSelected.invoke(group.groupTitle, group.groupURI)
    }

    holder.progress.visibility = View.VISIBLE
    holder.scrollView.visibility = View.INVISIBLE
    holder.scrollView.scrollX = 0
    holder.covers.removeAllViews()

    /*
     * If the group is empty, there isn't much we can do.
     */

    if (group.groupEntries.isEmpty()) {
      holder.progress.visibility = View.INVISIBLE
      return
    }

    /*
     * Add an end spacer so that book covers at the start of the group have a
     * small margin.
     */

    this.run {
      val space = Space(this.context)
      space.layoutParams = this.endSpaceLayoutParams
      holder.covers.addView(space)
    }

    /*
     * Instantiate a set of image views, one per book cover, and asynchronously
     * load image data into them.
     */

    val futures = mutableListOf<FluentFuture<Unit>>()
    for (i in 0 until group.groupEntries.size) {
      if (i > 0) {
        val space = Space(this.context)
        space.layoutParams = this.spaceLayoutParams
        holder.covers.addView(space)
      }

      val entry = group.groupEntries[i]
      if (entry is FeedEntry.FeedEntryOPDS) {
        val imageView = ImageView(this.context)
        imageView.scaleType =
          ImageView.ScaleType.FIT_XY
        imageView.layoutParams =
          this.coverLayoutParams
        imageView.contentDescription =
          CatalogBookAccessibilityStrings.coverDescription(this.context.resources, entry)
        imageView.setOnClickListener {
          this.onBookSelected.invoke(entry)
        }

        holder.covers.addView(imageView)

        futures.add(FluentFuture.from(this.coverLoader.loadThumbnailInto(
          entry = entry,
          imageView = imageView,
          width = this.coverLayoutParams.width,
          height = this.coverLayoutParams.height
        )))
      }
    }

    /*
     * Add an end spacer so that book covers at the end of the group have a
     * small margin.
     */

    this.run {
      val space = Space(this.context)
      space.layoutParams = this.endSpaceLayoutParams
      holder.covers.addView(space)
    }

    /*
     * From the given list of futures, produce a single future that is completed
     * when all of the futures in the list have either completed or failed. When
     * this aggregate future completes, make the layout containing the covers visible,
     * and hide the progress bar.
     */

    val loadingFuture =
      FluentFutureExtensions.fluentFutureOfAll(futures.toList())
    holder.coversLoading = loadingFuture

    loadingFuture.map {
      this.uiThread.runOnUIThread {
        holder.scrollView.visibility = View.VISIBLE
        holder.scrollView.alpha = 0.0f
        holder.scrollView.animate()
          .alpha(1f)
          .setDuration(this.shortAnimationDuration.toLong())
          .setListener(null)
        holder.progress.visibility = View.INVISIBLE
      }
    }
  }

  inner class ViewHolder(val parent: View) : RecyclerView.ViewHolder(parent) {
    var coversLoading: FluentFuture<List<Unit>>? = null

    val title =
      this.parent.findViewById<TextView>(R.id.feedLaneTitle)
    val progress =
      this.parent.findViewById<ProgressBar>(R.id.feedLaneProgress)
    val scrollView =
      this.parent.findViewById<HorizontalScrollView>(R.id.feedLaneCoversScroll)
    val covers =
      this.scrollView.findViewById<LinearLayout>(R.id.feedLaneCovers)
  }
}