package org.nypl.simplified.books.core

import com.io7m.jfunctional.OptionType
import org.nypl.drm.core.AdobeAdeptLoan
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import java.io.File

/**
 * A snapshot of the most recent state of a book in the database.
 */

data class BookDatabaseEntrySnapshot(

  /**
   * @return The book ID
   */

  val bookID: BookID,

  /**
   * @return The Adobe DRM rights, if any
   */

  val adobeRights: OptionType<AdobeAdeptLoan>,

  /**
   * @return The book file (typically an EPUB), if any
   */

  val book: OptionType<File>,

  /**
   * @return The cover image, if any
   */

  val cover: OptionType<File>,

  /**
   * @return The acquisition feed entry
   */

  val entry: OPDSAcquisitionFeedEntry) {

  override fun toString(): String {
    val sb = StringBuilder("BookDatabaseEntrySnapshot{")
    sb.append("adobe_rights=").append(this.adobeRights)
    sb.append(", id=").append(this.bookID)
    sb.append(", book=").append(this.book)
    sb.append(", cover=").append(this.cover)
    sb.append(", entry=").append(this.entry.availability)
    sb.append('}')
    return sb.toString()
  }
}
