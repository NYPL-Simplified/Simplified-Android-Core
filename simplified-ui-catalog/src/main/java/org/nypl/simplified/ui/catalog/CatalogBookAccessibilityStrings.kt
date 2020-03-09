package org.nypl.simplified.ui.catalog

import android.content.res.Resources
import org.nypl.simplified.books.book_database.api.BookFormats
import org.nypl.simplified.feeds.api.FeedEntry

/**
 * Strings related to accessibility.
 */

object CatalogBookAccessibilityStrings {

  /**
   * The content description for cover images.
   */

  fun coverDescription(
    resources: Resources,
    feedEntry: FeedEntry.FeedEntryOPDS
  ): String {
    return when (feedEntry.probableFormat) {
      BookFormats.BookFormatDefinition.BOOK_FORMAT_EPUB -> {
        resources.getString(
          R.string.catalogAccessibilityCoverEpub,
          feedEntry.feedEntry.title,
          feedEntry.feedEntry.authorsCommaSeparated
        )
      }
      BookFormats.BookFormatDefinition.BOOK_FORMAT_AUDIO -> {
        resources.getString(
          R.string.catalogAccessibilityCoverAudiobook,
          feedEntry.feedEntry.title,
          feedEntry.feedEntry.authorsCommaSeparated
        )
      }
      BookFormats.BookFormatDefinition.BOOK_FORMAT_PDF -> {
        resources.getString(
          R.string.catalogAccessibilityCoverPdf,
          feedEntry.feedEntry.title,
          feedEntry.feedEntry.authorsCommaSeparated
        )
      }
      null -> {
        resources.getString(
          R.string.catalogAccessibilityCoverUnknownFormat,
          feedEntry.feedEntry.title,
          feedEntry.feedEntry.authorsCommaSeparated
        )
      }
    }
  }
}
