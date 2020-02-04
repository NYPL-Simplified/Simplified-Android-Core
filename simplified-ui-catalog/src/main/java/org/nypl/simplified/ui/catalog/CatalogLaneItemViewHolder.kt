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
  private val view: View,
  private val coverLoader: BookCoverProviderType,
  private val onBookSelected: (FeedEntry.FeedEntryOPDS) -> Unit
) : RecyclerView.ViewHolder(view) {

  private val imageView = view.findViewById<ImageView>(R.id.coverImage)
  private val targetHeight =
    view.resources.getDimensionPixelSize(org.nypl.simplified.books.covers.R.dimen.cover_thumbnail_height)

  fun bindTo(entry: FeedEntry.FeedEntryOPDS) {
    view.contentDescription =
      CatalogBookAccessibilityStrings.coverDescription(view.resources, entry)

    view.setOnClickListener {
      onBookSelected.invoke(entry)
    }

    coverLoader.loadThumbnailInto(
      entry, imageView, 0, targetHeight
    )
  }
}
