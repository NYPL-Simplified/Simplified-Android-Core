package org.nypl.simplified.app.catalog

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.nypl.simplified.app.R
import org.nypl.simplified.app.ScreenSizeInformationType
import org.nypl.simplified.books.book_database.api.BookFormats.BookFormatDefinition.BOOK_FORMAT_AUDIO
import org.nypl.simplified.books.book_database.api.BookFormats.BookFormatDefinition.BOOK_FORMAT_EPUB
import org.nypl.simplified.books.book_database.api.BookFormats.BookFormatDefinition.BOOK_FORMAT_PDF
import org.nypl.simplified.books.covers.BookCoverBadge
import org.nypl.simplified.books.covers.BookCoverBadgeLookupType
import org.nypl.simplified.feeds.api.FeedEntry.FeedEntryOPDS

/**
 * The images used to add badges to book covers.
 */

class CatalogCoverBadgeImages private constructor(
  private val screenSize: ScreenSizeInformationType,
  private val backgroundColorRGBA: () -> Int,
  private val audioBookIcon: Bitmap) : BookCoverBadgeLookupType {

  override fun badgeForEntry(
    entry: FeedEntryOPDS): BookCoverBadge? {
    return when (entry.probableFormat) {
      BOOK_FORMAT_EPUB -> {
        null
      }
      BOOK_FORMAT_AUDIO -> {
        BookCoverBadge(
          bitmap = this.audioBookIcon,
          width = this.screenSize.screenDPToPixels(24).toInt(),
          height = this.screenSize.screenDPToPixels(24).toInt(),
          backgroundColorRGBA = { this.backgroundColorRGBA() })
      }
      BOOK_FORMAT_PDF -> {
        null
      }
      null -> {
        null
      }
    }
  }

  companion object {

    /**
     * Create a new set of badge images.
     */

    fun create(
      resources: Resources,
      backgroundColorRGBA: () -> Int,
      screenSize: ScreenSizeInformationType): BookCoverBadgeLookupType {
      val audioBookIcon = BitmapFactory.decodeResource(resources, R.drawable.audiobook_icon)
      return CatalogCoverBadgeImages(
        audioBookIcon = audioBookIcon,
        backgroundColorRGBA = backgroundColorRGBA,
        screenSize = screenSize)
    }
  }
}
