package org.nypl.simplified.books.core

import com.io7m.jfunctional.OptionType
import org.nypl.drm.core.AdobeAdeptLoan
import org.nypl.simplified.http.core.HTTPType
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry

import java.io.File
import java.io.IOException

/**
 *
 * The writable interface supported by book database entries.
 *
 *
 * These are blocking operations that imply disk I/O.
 */

interface BookDatabaseEntryWritableType {

  /**
   * Copy the given file into the directory as the book data. Typically, this
   * will be an EPUB file. This function will instantly fail if `file` is not on
   * the same filesystem as the book database.
   *
   * @param file The file to be copied
   *
   * @return A snapshot of the new database state
   *
   * @throws IOException On I/O errors or lock acquisition failures
   */

  @Throws(IOException::class)
  fun entryCopyInBookFromSameFilesystem(file: File): BookDatabaseEntrySnapshot

  /**
   * Copy the given file into the directory as the book data. Typically, this
   * will be an EPUB file.
   *
   * @param file The file to be copied
   *
   * @return A snapshot of the new database state
   *
   * @throws IOException On I/O errors or lock acquisition failures
   */

  @Throws(IOException::class)
  fun entryCopyInBook(file: File): BookDatabaseEntrySnapshot

  /**
   * Create the book directory if it does not exist.
   *
   * @param entry The feed entry
   * @return A snapshot of the new database state
   * @throws IOException On I/O errors or lock acquisition failures
   */

  @Throws(IOException::class)
  fun entryCreate(entry: OPDSAcquisitionFeedEntry): BookDatabaseEntrySnapshot

  /**
   * Destroy the book directory and all of its contents.
   *
   * @throws IOException On I/O errors or lock acquisition failures
   */

  @Throws(IOException::class)
  fun entryDestroy()

  /**
   * Destroy the book data, if it exists.
   *
   * @return A snapshot of the new database state
   *
   * @throws IOException On I/O errors or lock acquisition failures
   */

  @Throws(IOException::class)
  fun entryDeleteBookData(): BookDatabaseEntrySnapshot

  /**
   * Set the acquisition feed entry of the book
   *
   * @param entry The feed entry
   *
   * @return A snapshot of the new database state
   *
   * @throws IOException On I/O errors or lock acquisition failures
   */

  @Throws(IOException::class)
  fun entrySetFeedData(entry: OPDSAcquisitionFeedEntry): BookDatabaseEntrySnapshot

  /**
   * Set the cover of the book
   *
   * @param cover The cover
   *
   * @return A snapshot of the new database state
   *
   * @throws IOException On I/O errors or lock acquisition failures
   */

  @Throws(IOException::class)
  fun entrySetCover(cover: OptionType<File>): BookDatabaseEntrySnapshot

  /**
   * Set the Adobe rights information for the book.
   *
   * @param loan The loan
   *
   * @return A snapshot of the new database state
   *
   * @throws IOException On I/O errors or lock acquisition failures
   */

  @Throws(IOException::class)
  fun entrySetAdobeRightsInformation(loan: OptionType<AdobeAdeptLoan>): BookDatabaseEntrySnapshot

  /**
   * Set the list of bookmarks to be saved for the book
   *
   * @param bookmarks The list of bookmark annotations to be saved
   *
   * @return A snapshot of the new database state
   *
   * @throws IOException On I/O errors or lock acquisition failures
   */

  @Throws(IOException::class)
  fun entrySetBookmarks(bookmarks: List<BookmarkAnnotation>): BookDatabaseEntrySnapshot

  /**
   * Set a user-created bookmark for the book.
   *
   * @param bookmark The bookmark annotation to be saved
   *
   * @return A snapshot of the new database state
   *
   * @throws IOException On I/O errors or lock acquisition failures
   */

  @Throws(IOException::class)
  fun entryAddBookmark(bookmark: BookmarkAnnotation): BookDatabaseEntrySnapshot

  /**
   * Delete a user-created bookmark from the database
   *
   * @param bookmark The bookmark annotation to be deleted
   *
   * @return A snapshot of the new database state
   *
   * @throws IOException On I/O errors or lock acquisition failures
   */

  @Throws(IOException::class)
  fun entryDeleteBookmark(bookmark: BookmarkAnnotation): BookDatabaseEntrySnapshot

  /**
   * Update the book data based on `entry`. The cover, if any, will be fetched
   * using the http interface `http`, and the new status will be published
   * to `bookStatus`.
   *
   * @param entry            The feed entry
   * @param bookStatus The book status cache
   * @param http         The HTTP interface
   *
   * @return A snapshot of the new database state
   *
   * @throws IOException On I/O errors or lock acquisition failures
   */

  @Throws(IOException::class)
  fun entryUpdateAll(
    entry: OPDSAcquisitionFeedEntry,
    bookStatus: BooksStatusCacheType,
    http: HTTPType): BookDatabaseEntrySnapshot
}
