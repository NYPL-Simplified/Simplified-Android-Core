package org.nypl.simplified.ui.catalog.withoutGroups

import android.content.Context
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import org.nypl.simplified.books.book_database.api.BookFormats
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.ui.catalog.CatalogBookAccessibilityStrings
import org.nypl.simplified.ui.catalog.R

@BindingAdapter("coverContentDescriptionForItem")
internal fun ImageView.coverContentDescriptionForItem(entry: FeedEntry.FeedEntryOPDS) {
  contentDescription = CatalogBookAccessibilityStrings.coverDescription(context.resources, entry)
}

@BindingAdapter("formatLabelForEntry")
internal fun TextView.formatLabelForEntry(entry: FeedEntry.FeedEntryOPDS) {
  text = when (entry.probableFormat) {
    BookFormats.BookFormatDefinition.BOOK_FORMAT_EPUB ->
      context.getString(R.string.catalogBookFormatEPUB)
    BookFormats.BookFormatDefinition.BOOK_FORMAT_AUDIO ->
      context.getString(R.string.catalogBookFormatAudioBook)
    BookFormats.BookFormatDefinition.BOOK_FORMAT_PDF ->
      context.getString(R.string.catalogBookFormatPDF)
    null -> ""
  }
}

@BindingAdapter("setupThumbnail")
internal fun ImageView.setupThumbnail(loadThumbnail: (Context) -> Unit) {
  loadThumbnail(context)
}
