package org.nypl.simplified.books.core

import com.io7m.jfunctional.OptionType
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry

import java.io.File
import java.io.IOException

/**
 *
 *  The readable interface supported by book database entries.
 */

interface BookDatabaseEntryReadableType {

  /**
   * @return `true` if the book directory exists.
   */

  fun entryExists(): Boolean

  /**
   * @return The cover of the book, if any
   *
   * @throws IOException On I/O errors or lock acquisition failures
   */

  @Throws(IOException::class)
  fun entryGetCover(): OptionType<File>

  /**
   * @return The acquisition feed entry associated with the book
   *
   * @throws IOException On I/O errors or lock acquisition failures
   */

  @Throws(IOException::class)
  fun entryGetFeedData(): OPDSAcquisitionFeedEntry

  /**
   * @return A read-only snapshot of the list of bookmarks associated with the user and book
   *
   * @throws IOException On I/O errors or lock acquisition failures
   */

  @Throws(IOException::class)
  fun entryGetBookmarks(): List<BookmarkAnnotation>

  /**
   * @return The database entry directory
   */

  fun entryGetDirectory(): File

  /**
   * @return The ID of the book
   */

  fun entryGetBookID(): BookID

  /**
   * @return A snapshot of the current entry state
   *
   * @throws IOException On I/O errors or lock acquisition failures
   */

  @Throws(IOException::class)
  fun entryGetSnapshot(): BookDatabaseEntrySnapshot
}
