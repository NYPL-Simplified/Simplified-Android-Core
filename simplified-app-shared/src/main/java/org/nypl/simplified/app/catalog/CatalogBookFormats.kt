package org.nypl.simplified.app.catalog

import android.content.res.Resources
import org.nypl.simplified.app.R
import org.nypl.simplified.books.book_database.api.BookFormats.BookFormatDefinition.BOOK_FORMAT_AUDIO
import org.nypl.simplified.books.book_database.api.BookFormats.BookFormatDefinition.BOOK_FORMAT_EPUB
import org.nypl.simplified.books.book_database.api.BookFormats.BookFormatDefinition.BOOK_FORMAT_PDF
import org.nypl.simplified.feeds.api.FeedEntry.FeedEntryOPDS

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
    entry: FeedEntryOPDS
  ): String {
    return when (entry.probableFormat) {
      BOOK_FORMAT_EPUB -> {
        resources.getString(
          R.string.catalog_accessibility_cover_epub,
          entry.feedEntry.title,
          entry.feedEntry.authorsCommaSeparated)
      }
      BOOK_FORMAT_AUDIO -> {
        resources.getString(
          R.string.catalog_accessibility_cover_audiobook,
          entry.feedEntry.title,
          entry.feedEntry.authorsCommaSeparated)
      }
      BOOK_FORMAT_PDF -> {
        entry.feedEntry.title
      }
      null -> {
        entry.feedEntry.title
      }
    }
  }
}
