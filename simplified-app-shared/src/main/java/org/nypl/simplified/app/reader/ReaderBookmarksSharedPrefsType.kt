package org.nypl.simplified.app.reader

import org.nypl.simplified.books.core.BookID
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

  /**
   * Retrieve list of user-created bookmarks.
   *
   * @param id The ID of the book
   * @param entry feed entry
   * @return A list bookmark annotations, if any
   */

//TODO just testing saving to shared prefs
//  fun getUserBookmarks(id: BookID,
//                       entry: OPDSAcquisitionFeedEntry): List<NYPLAnnotation>?
//
//  /**
//   * Save the list of user-created bookmarks to shared prefs.
//   *
//   * @param id The ID of the book
//   * @param bookmarks The list of bookmark annotations
//   */
//  fun saveUserBookmarks(id: BookID,
//                        bookmarks: List<NYPLAnnotation>)

}
