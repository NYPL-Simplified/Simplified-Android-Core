package org.nypl.simplified.ui.catalog

import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import org.nypl.simplified.books.covers.BookCoverProviderType
import org.nypl.simplified.feeds.api.FeedEntry

/**
 * This adapter displays a list of feed items in a catalog lane.
 *
 * @see CatalogLaneItemViewHolder
 */
class CatalogLaneItemViewHolder(
  private val parent: View,
  private val coverLoader: BookCoverProviderType,
  private val onBookSelected: (FeedEntry.FeedEntryOPDS) -> Unit
) : RecyclerView.ViewHolder(parent) {

  private val imageView = parent as ImageView
  private val imageWidth =
    parent.resources.getDimensionPixelSize(R.dimen.catalogFeedCoversWidth)
  private val imageHeight =
    parent.resources.getDimensionPixelSize(R.dimen.catalogFeedCoversHeight)

  fun bindTo(entry: FeedEntry.FeedEntryOPDS) {
    imageView.contentDescription =
      CatalogBookAccessibilityStrings.coverDescription(parent.resources, entry)

    imageView.setOnClickListener {
      onBookSelected.invoke(entry)
    }

    coverLoader.loadThumbnailInto(
      entry = entry,
      imageView = imageView,
      width = imageWidth,
      height = imageHeight
    )
  }
}
