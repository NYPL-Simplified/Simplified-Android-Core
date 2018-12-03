package org.nypl.simplified.app.catalog

import android.content.Context
import android.content.res.Resources
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.MoreExecutors.directExecutor
import com.io7m.jfunctional.Some
import com.io7m.jfunctional.Unit
import com.io7m.junreachable.UnreachableCodeException
import org.nypl.simplified.app.R
import org.nypl.simplified.app.ScreenSizeControllerType
import org.nypl.simplified.app.Simplified
import org.nypl.simplified.app.ThemeMatcher
import org.nypl.simplified.app.utilities.FadeUtilities
import org.nypl.simplified.app.utilities.UIThread
import org.nypl.simplified.books.core.BookFormats
import org.nypl.simplified.books.core.BookFormats.BookFormatDefinition.BOOK_FORMAT_AUDIO
import org.nypl.simplified.books.core.BookFormats.BookFormatDefinition.BOOK_FORMAT_EPUB
import org.nypl.simplified.books.core.FeedEntryCorrupt
import org.nypl.simplified.books.core.FeedEntryMatcherType
import org.nypl.simplified.books.core.FeedEntryOPDS
import org.nypl.simplified.books.core.FeedGroup
import org.nypl.simplified.books.core.LogUtilities
import org.nypl.simplified.books.covers.BookCoverProviderType
import java.util.ArrayList
import java.util.concurrent.atomic.AtomicInteger

/**
 * A feed lane.
 */

class CatalogFeedLane(
  context: Context,
  private val covers: BookCoverProviderType,
  private val screen: ScreenSizeControllerType,
  private val listener: CatalogFeedLaneListenerType) : LinearLayout(context) {

  private val imageHeight: Int
  private val progress: ProgressBar
  private val scroller: HorizontalScrollView
  private val scrollerContents: ViewGroup
  private val title: TextView
  private val header: RelativeLayout
  private val inflater: LayoutInflater

  init {
    this.orientation = LinearLayout.VERTICAL

    this.inflater = this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    this.inflater.inflate(R.layout.catalog_feed_groups_lane, this, true)

    val account = Simplified.getCurrentAccount()

    this.header = this.findViewById(R.id.feed_header)
    val resID = ThemeMatcher.color(account.mainColor)
    val mainColor = ContextCompat.getColor(this.context, resID)

    (this.header.findViewById<View>(R.id.feed_title) as TextView).setTextColor(mainColor)
    (this.header.findViewById<View>(R.id.feed_more) as TextView).setTextColor(mainColor)

    this.title = this.findViewById(R.id.feed_title)
    this.progress = this.findViewById(R.id.feed_progress)
    this.scroller = this.findViewById(R.id.feed_scroller)
    this.scroller.isHorizontalScrollBarEnabled = false
    this.scrollerContents = this.scroller.findViewById(R.id.feed_scroller_contents)

    val sp = this.scroller.layoutParams
    this.imageHeight = sp.height
  }

  /**
   * Configure the lane for the given group.
   *
   * @param group The group
   */

  fun configureForGroup(group: FeedGroup) {
    this.configureView(group)
  }

  private fun configureView(feedGroup: FeedGroup) {

    this.scroller.visibility = View.INVISIBLE
    this.scroller.post { this.scroller.scrollTo(0, 0) }

    this.scrollerContents.visibility = View.INVISIBLE
    this.progress.visibility = View.VISIBLE

    this.scrollerContents.removeAllViews()
    this.title.text = feedGroup.groupTitle

    this.header.contentDescription = String.format(this.resources.getString(R.string.catalog_accessibility_header_show_more), this.title.text)
    this.header.setOnClickListener { _ -> this.listener.onSelectFeed(feedGroup) }

    val entries = feedGroup.groupEntries
    val coverViews = ArrayList<ImageView?>(entries.size)

    for (index in entries.indices) {
      entries[index].matchFeedEntry(
        object : FeedEntryMatcherType<Unit, UnreachableCodeException> {
          override fun onFeedEntryCorrupt(entry: FeedEntryCorrupt): Unit {
            return this@CatalogFeedLane.addViewForFeedEntryCorrupt(coverViews)
          }

          override fun onFeedEntryOPDS(entry: FeedEntryOPDS): Unit {
            return this@CatalogFeedLane.addViewForFeedEntryOPDS(entry, coverViews)
          }
        })
    }

    val imageWidth = (this@CatalogFeedLane.imageHeight.toDouble() * 0.75).toInt()
    val imagesRemaining = AtomicInteger(entries.size)

    for (index in entries.indices) {
      val imageView = coverViews[index]
      if (imageView == null) {
        continue
      }

      val feedEntry = entries[index]
      val coverCallback = object : FutureCallback<kotlin.Unit> {
        override fun onSuccess(result: kotlin.Unit?) {
          UIThread.runOnUIThread {
            if (imagesRemaining.decrementAndGet() <= 0) {
              this@CatalogFeedLane.onFinishedLoadingAllImages()
            }
          }
        }

        override fun onFailure(exception: Throwable) {
          LOG.debug("could not load image for {}: ", feedEntry.bookID, exception)

          UIThread.runOnUIThread {
            imageView.visibility = View.GONE
            if (imagesRemaining.decrementAndGet() <= 0) {
              this@CatalogFeedLane.onFinishedLoadingAllImages()
            }
          }
        }
      }

      val future =
        this.covers.loadThumbnailInto(
        feedEntry as FeedEntryOPDS,
        imageView,
        imageWidth,
        this.imageHeight)

      Futures.addCallback(future, coverCallback, directExecutor())
    }
  }

  private fun addViewForFeedEntryOPDS(
    entry: FeedEntryOPDS,
    coverViews: ArrayList<ImageView?>): Unit {

    /*
     * The height of the row is known, so assume a roughly 4:3 aspect ratio
     * for cover images and calculate the width of the cover layout in pixels.
     */

    val imageView = ImageView(this.context)
    val imageWidth = (this.imageHeight.toDouble() / 4.0 * 3.0).toInt()
    val layoutParams = LinearLayout.LayoutParams(imageWidth, this.imageHeight)
    layoutParams.setMargins(0, 0, this.screen.screenDPToPixels(8).toInt(), 0)

    imageView.layoutParams = layoutParams
    imageView.contentDescription =
      CatalogBookFormats.contentDescriptionOfEntry(this.resources, entry)
    imageView.setOnClickListener { this.listener.onSelectBook(entry) }
    coverViews.add(imageView)

    this.scrollerContents.addView(imageView)
    return Unit.unit()
  }



  private fun addViewForFeedEntryCorrupt(
    coverViewCollections: ArrayList<ImageView?>): Unit {
    coverViewCollections.add(null)
    return Unit.unit()
  }

  private fun onFinishedLoadingAllImages() {
    LOG.debug("images done")

    this.progress.visibility = View.INVISIBLE
    this.scrollerContents.visibility = View.VISIBLE
    FadeUtilities.fadeIn(this.scroller, FadeUtilities.DEFAULT_FADE_DURATION)
  }

  companion object {
    private val LOG = LogUtilities.getLog(CatalogFeedLane::class.java)
  }
}
