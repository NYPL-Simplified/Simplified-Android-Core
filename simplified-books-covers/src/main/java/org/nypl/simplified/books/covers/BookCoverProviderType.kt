package org.nypl.simplified.books.covers

import android.widget.ImageView
import com.google.common.util.concurrent.FluentFuture
import org.nypl.simplified.feeds.api.FeedEntry

/**
 * The type of cover providers.
 */

interface BookCoverProviderType {

  /**
   * Pause loading of any covers. Loading will continue upon calling [loadingThumbnailsContinue].
   */

  fun loadingThumbnailsPause()

  /**
   * Continue loading of covers after having been paused with [loadingThumbnailsPause].
   * Has no effect if loading is not paused.
   */

  fun loadingThumbnailsContinue()

  /**
   * Load or generate a thumbnail based on `entry` into the image view
   * `imageView`, at width `width` and height `height`.
   *
   * Must only be called from the UI thread.
   *
   * @param entry The feed entry
   * @param imageView The image view
   * @param width Use 0 as desired dimension to resize keeping aspect ratio.
   * @param height Use 0 as desired dimension to resize keeping aspect ratio.
   */

  fun loadThumbnailInto(
    entry: FeedEntry.FeedEntryOPDS,
    imageView: ImageView,
    width: Int,
    height: Int
  ): FluentFuture<Unit>

  /**
   * Load or generate a cover based on `entry` into the image view
   * `imageView`, at width `width` and height `height`.
   *
   * Must only be called from the UI thread.
   *
   * @param entry The feed entry
   * @param imageView The image view
   * @param width Use 0 as desired dimension to resize keeping aspect ratio.
   * @param height Use 0 as desired dimension to resize keeping aspect ratio.
   */

  fun loadCoverInto(
    entry: FeedEntry.FeedEntryOPDS,
    imageView: ImageView,
    width: Int,
    height: Int
  ): FluentFuture<Unit>
}
