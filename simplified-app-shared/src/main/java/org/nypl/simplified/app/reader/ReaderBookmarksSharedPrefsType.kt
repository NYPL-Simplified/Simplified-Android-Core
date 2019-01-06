package org.nypl.simplified.app.reader

import org.nypl.simplified.books.core.BookID
import org.nypl.simplified.books.reader.ReaderBookLocation
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry

/**
 * The interface exposed by bookmarks,
 * user created and current position, saved to shared prefs.
 */

interface ReaderBookmarksSharedPrefsType {

  /**
   * Retrieve current reading position from shared prefs.
   *
   * @param id The ID of the book
   * @param entry feed entry
   * @return A bookmark, if any
   */
  fun getReadingPosition(id: BookID,
                         entry: OPDSAcquisitionFeedEntry): ReaderBookLocation?

  /**
   * Save the current reading position to shared prefs.
   *
   * @param id The ID of the book
   * @param bookmark The bookmark
   */
  fun saveReadingPosition(id: BookID,
                          bookmark: ReaderBookLocation)

}
