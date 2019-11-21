package org.nypl.simplified.ui.catalog

import android.view.View
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

class CatalogFeedWithGroupsLaneViewHolder(
  private val parent: View,
  private val uiThread: UIThreadServiceType,
  private val coverLoader: BookCoverProviderType,
  private val onFeedSelected: (title: String, uri: URI) -> Unit,
  private val onBookSelected: (FeedEntry.FeedEntryOPDS) -> Unit
) : RecyclerView.ViewHolder(parent) {

  private var coversLoading: FluentFuture<List<Unit>>? = null

  private val endSpaceLayoutParams =
    LinearLayout.LayoutParams(
      this.parent.context.resources.getDimensionPixelSize(R.dimen.catalogFeedCoversEndSpace),
      this.parent.context.resources.getDimensionPixelSize(R.dimen.catalogFeedCoversHeight))

  private val spaceLayoutParams =
    LinearLayout.LayoutParams(
      this.parent.context.resources.getDimensionPixelSize(R.dimen.catalogFeedCoversSpace),
      this.parent.context.resources.getDimensionPixelSize(R.dimen.catalogFeedCoversHeight))

  private val coverLayoutParams =
    LinearLayout.LayoutParams(
      this.parent.context.resources.getDimensionPixelSize(R.dimen.catalogFeedCoversWidth),
      this.parent.context.resources.getDimensionPixelSize(R.dimen.catalogFeedCoversHeight))

  private val title =
    this.parent.findViewById<TextView>(R.id.feedLaneTitle)
  private val progress =
    this.parent.findViewById<ProgressBar>(R.id.feedLaneProgress)
  private val scrollView =
    this.parent.findViewById<HorizontalScrollView>(R.id.feedLaneCoversScroll)
  private val covers =
    this.scrollView.findViewById<LinearLayout>(R.id.feedLaneCovers)

  fun bindTo(group: FeedGroup) {
    this.title.text = group.groupTitle
    this.title.setOnClickListener {
      this.onFeedSelected.invoke(group.groupTitle, group.groupURI)
    }

    this.progress.visibility = View.VISIBLE
    this.scrollView.visibility = View.INVISIBLE
    this.scrollView.scrollX = 0
    this.covers.removeAllViews()

    /*
     * If the group is empty, there isn't much we can do.
     */

    if (group.groupEntries.isEmpty()) {
      this.progress.visibility = View.INVISIBLE
      return
    }

    /*
     * Add an end spacer so that book covers at the start of the group have a
     * small margin.
     */

    this.run {
      val space = Space(this.parent.context)
      space.layoutParams = this.endSpaceLayoutParams
      this.covers.addView(space)
    }

    /*
     * Instantiate a set of image views, one per book cover, and asynchronously
     * load image data into them.
     */

    val futures = mutableListOf<FluentFuture<Unit>>()
    for (i in 0 until group.groupEntries.size) {
      if (i > 0) {
        val space = Space(this.parent.context)
        space.layoutParams = this.spaceLayoutParams
        this.covers.addView(space)
      }

      val entry = group.groupEntries[i]
      if (entry is FeedEntry.FeedEntryOPDS) {
        val imageView = ImageView(this.parent.context)
        imageView.scaleType =
          ImageView.ScaleType.FIT_XY
        imageView.layoutParams =
          this.coverLayoutParams
        imageView.contentDescription =
          CatalogBookAccessibilityStrings.coverDescription(this.parent.resources, entry)
        imageView.setOnClickListener {
          this.onBookSelected.invoke(entry)
        }

        this.covers.addView(imageView)

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
      val space = Space(this.parent.context)
      space.layoutParams = this.endSpaceLayoutParams
      this.covers.addView(space)
    }

    /*
     * From the given list of futures, produce a single future that is completed
     * when all of the futures in the list have either completed or failed. When
     * this aggregate future completes, make the layout containing the covers visible,
     * and hide the progress bar.
     */

    val loadingFuture = FluentFutureExtensions.fluentFutureOfAll(futures.toList())
    this.coversLoading = loadingFuture

    loadingFuture.map {
      this.uiThread.runOnUIThread {
        this.scrollView.visibility = View.VISIBLE
        this.progress.visibility = View.INVISIBLE
      }
    }
  }

  fun unbind() {
    this.progress.visibility = View.VISIBLE
    this.scrollView.visibility = View.INVISIBLE
    this.scrollView.scrollX = 0
    this.covers.removeAllViews()

    this.coversLoading =
      this.coversLoading?.let { covers ->
        covers.cancel(true)
        null
      }
  }
}
