package org.nypl.simplified.ui.catalog.withoutGroups

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.ConfigurationCompat
import androidx.databinding.BindingAdapter
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
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

@BindingAdapter("formatOptionalExpiryInfo")
internal fun TextView.formatOptionalExpiryInfo(dateTime: DateTime?) {
  dateTime?.let {
    val locale = ConfigurationCompat.getLocales(context.resources.configuration).get(0)
    val format = DateTimeFormat.forPattern("E, MMM d").withLocale(locale)
    text = context.resources.getString(R.string.catalogBookAvailabilityAvailableUntil, dateTime.toString(format))
  } ?: run {
    visibility = View.GONE
  }
}
