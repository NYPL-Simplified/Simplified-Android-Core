package org.nypl.simplified.books.core

import com.io7m.jfunctional.OptionType
import org.nypl.simplified.http.core.HTTPType
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import java.io.File
import java.io.IOException

/**
 * The readable/writable interface supported by book database entries.
 *
 * These are blocking operations that imply disk I/O.
 */

interface BookDatabaseEntryType : BookDatabaseEntryReadableType {

  /**
   * Destroy the book directory and all of its contents.
   *
   * @throws IOException On I/O errors or lock acquisition failures
   */

  @Throws(IOException::class)
  fun entryDestroy()

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

  /**
   * Retrieve a list of all format handles exposed by the database entry.
   *
   * @return A list of all format handles exposed by the database entry
   */

  fun entryFormatHandles(): List<BookDatabaseEntryFormatHandle>

  /**
   * @param clazz The type of format
   * @return A reference to the given format, if one is supported
   */

  fun <T : BookDatabaseEntryFormatHandle> entryFindFormatHandle(clazz: Class<T>): OptionType<T>

  /**
   * @param contentType The MIME type
   * @return A reference to a format handle that has content of the given type, if any
   */

  fun entryFindFormatHandleForContentType(contentType: String): OptionType<BookDatabaseEntryFormatHandle>

}
