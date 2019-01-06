package org.nypl.simplified.app.catalog

import android.content.res.Resources
import com.io7m.jfunctional.Some
import org.nypl.simplified.app.R
import org.nypl.simplified.books.core.BookFormats
import org.nypl.simplified.books.core.BookFormats.BookFormatDefinition.BOOK_FORMAT_AUDIO
import org.nypl.simplified.books.core.BookFormats.BookFormatDefinition.BOOK_FORMAT_EPUB
import org.nypl.simplified.books.feeds.FeedEntryOPDS

/**
 * Functions over book formats.
 */

object CatalogBookFormats {

  /**
   * @return A useful content description for the given OPDS entry
   */

  @JvmStatic
  fun contentDescriptionOfEntry(
    resources: Resources,
    entry: FeedEntryOPDS): String {
    val formatOpt = entry.probableFormat
    return if (formatOpt is Some<BookFormats.BookFormatDefinition>) {
      val format = formatOpt.get()
      when (format) {
        null,
        BOOK_FORMAT_EPUB ->
          resources.getString(
            R.string.catalog_accessibility_cover_epub,
            entry.feedEntry.title,
            entry.feedEntry.authors,
            entry.feedEntry.authorsCommaSeparated)
        BOOK_FORMAT_AUDIO ->
          resources.getString(
            R.string.catalog_accessibility_cover_audiobook,
            entry.feedEntry.title,
            entry.feedEntry.authorsCommaSeparated)
      }
    } else {
      entry.feedEntry.title
    }
  }
}
