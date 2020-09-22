package org.nypl.simplified.books.book_database.api

import one.irradia.mime.api.MIMEType
import org.librarysimplified.audiobook.api.PlayerPosition
import org.nypl.simplified.books.api.Book
import org.nypl.simplified.books.api.BookDRMKind
import org.nypl.simplified.books.api.BookFormat
import org.nypl.simplified.books.api.BookFormat.BookFormatAudioBook
import org.nypl.simplified.books.api.BookFormat.BookFormatEPUB
import org.nypl.simplified.books.api.BookFormat.BookFormatPDF
import org.nypl.simplified.books.api.Bookmark
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleAudioBook
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleEPUB
import org.nypl.simplified.books.book_database.api.BookFormats.BookFormatDefinition.BOOK_FORMAT_AUDIO
import org.nypl.simplified.books.book_database.api.BookFormats.BookFormatDefinition.BOOK_FORMAT_EPUB
import org.nypl.simplified.books.book_database.api.BookFormats.BookFormatDefinition.BOOK_FORMAT_PDF
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import java.io.File
import java.io.IOException
import java.net.URI
import javax.annotation.concurrent.ThreadSafe

/**
 * An entry in the book database.
 *
 * Implementations are required to be safe to use from multiple threads.
 */

@ThreadSafe
interface BookDatabaseEntryType {

  /**
   * @return The most recent book value for the entry
   */

  val book: Book

  /**
   * Copy the cover file into the database.
   *
   * @param file The cover file
   * @throws BookDatabaseException On errors
   */

  @Throws(BookDatabaseException::class)
  fun setCover(file: File)

  /**
   * Copy the thumbnail file into the database.
   *
   * @param file The cover file
   * @throws BookDatabaseException On errors
   */

  @Throws(BookDatabaseException::class)
  fun setThumbnail(file: File)

  /**
   * Copy the OPDS entry into the database.
   *
   * @param opdsEntry The OPDS entry
   * @throws BookDatabaseException On errors
   */

  @Throws(BookDatabaseException::class)
  fun writeOPDSEntry(opdsEntry: OPDSAcquisitionFeedEntry)

  /**
   * Delete the entry.
   *
   * @throws BookDatabaseException On errors
   */

  @Throws(BookDatabaseException::class)
  fun delete()

  /**
   * @return A temporary file that can be used to stage data to be copied into the database
   */

  @Throws(IOException::class)
  fun temporaryFile(): File

  /**
   * Retrieve a list of all format handles exposed by the database entry.
   *
   * @return A list of all format handles exposed by the database entry
   */

  val formatHandles: List<BookDatabaseEntryFormatHandle>

  /**
   * @param clazz The type of format
   * @return A reference to the given format, if one is supported
   */

  fun <T : BookDatabaseEntryFormatHandle> findFormatHandle(clazz: Class<T>): T? {
    return this.formatHandles.find { handle -> clazz.isAssignableFrom(handle.javaClass) } as T?
  }

  /**
   * @param contentType The MIME type
   * @return A reference to a format handle that has content of the given type, if any
   */

  fun findFormatHandleForContentType(contentType: MIMEType): BookDatabaseEntryFormatHandle? {
    return this.formatHandles
      .find { handle ->
        handle.formatDefinition.supportedContentTypes().any { type ->
          type.fullType == contentType.fullType
        }
      }
  }

  /**
   * @return The "preferred" format for the given type, if any
   */

  fun findPreferredFormatHandle(): BookDatabaseEntryFormatHandle? {
    val formats = mutableListOf<BookDatabaseEntryFormatHandle>()
    val handles = this.formatHandles
    formats.addAll(handles.filterIsInstance(BookDatabaseEntryFormatHandleEPUB::class.java))
    formats.addAll(handles.filterIsInstance(BookDatabaseEntryFormatHandleAudioBook::class.java))
    formats.addAll(handles)
    return formats.firstOrNull()
  }
}

/**
 * The type of book format handles in database entries.
 */

sealed class BookDatabaseEntryFormatHandle {

  /**
   * The DRM information handle
   */

  abstract val drmInformationHandle: BookDRMInformationHandle

  /**
   * Change the DRM type to the specified kind. Note that this will invalidate any existing
   * DRM information handle for the format.
   *
   * @throws IOException On I/O errors
   */

  @Throws(IOException::class)
  abstract fun setDRMKind(kind: BookDRMKind)

  /**
   * @return The format definition
   */

  abstract val formatDefinition: BookFormats.BookFormatDefinition

  /**
   * @return The most recent format information
   */

  abstract val format: BookFormat

  /**
   * Destroy the book data, if it exists.
   *
   * @throws IOException On I/O errors
   */

  @Throws(IOException::class)
  abstract fun deleteBookData()

  /**
   * The interface exposed by the EPUB format in database entries.
   */

  abstract class BookDatabaseEntryFormatHandleEPUB : BookDatabaseEntryFormatHandle() {

    abstract override val format: BookFormatEPUB

    override val formatDefinition: BookFormats.BookFormatDefinition =
      BOOK_FORMAT_EPUB

    /**
     * Copy the given EPUB file into the directory as the book data.
     *
     * @param file The file to be copied
     *
     * @throws IOException On I/O errors
     */

    @Throws(IOException::class)
    abstract fun copyInBook(file: File)

    /**
     * Set the last read location for the book.
     *
     * @param bookmark The location
     *
     * @throws IOException On I/O errors
     */

    @Throws(IOException::class)
    abstract fun setLastReadLocation(bookmark: Bookmark?)

    /**
     * Set the bookmarks for the book.
     *
     * @param bookmarks The list of bookmarks
     *
     * @throws IOException On I/O errors
     */

    @Throws(IOException::class)
    abstract fun setBookmarks(bookmarks: List<Bookmark>)
  }

  /**
   * The interface exposed by the PDF format in database entries.
   */

  abstract class BookDatabaseEntryFormatHandlePDF : BookDatabaseEntryFormatHandle() {

    abstract override val format: BookFormatPDF

    override val formatDefinition: BookFormats.BookFormatDefinition =
      BOOK_FORMAT_PDF

    /**
     * Copy the given PDF file into the directory as the book data.
     *
     * @param file The file to be copied
     *
     * @throws IOException On I/O errors
     */

    @Throws(IOException::class)
    abstract fun copyInBook(file: File)

    /**
     * Set the last read location for the PDF book.
     *
     * @param pageNumber The Int page number of the PDF book
     *
     * @throws IOException On I/O errors
     */
    @Throws(IOException::class)
    abstract fun setLastReadLocation(pageNumber: Int?)
  }

  /**
   * The interface exposed by the audio book format in database entries.
   */

  abstract class BookDatabaseEntryFormatHandleAudioBook : BookDatabaseEntryFormatHandle() {

    abstract override val format: BookFormatAudioBook

    override val formatDefinition: BookFormats.BookFormatDefinition =
      BOOK_FORMAT_AUDIO

    /**
     * Save the manifest and the URI that can be used to fetch more up-to-date copies of it
     * later.
     *
     * @throws IOException On I/O errors or lock acquisition failures
     */

    @Throws(IOException::class)
    abstract fun copyInManifestAndURI(
      data: ByteArray,
      manifestURI: URI
    )

    /**
     * Save the given player position to the database.
     *
     * @throws IOException On I/O errors or lock acquisition failures
     */

    @Throws(IOException::class)
    abstract fun savePlayerPosition(position: PlayerPosition)

    /**
     * Clear the saved player position in the database.
     *
     * @throws IOException On I/O errors or lock acquisition failures
     */

    @Throws(IOException::class)
    abstract fun clearPlayerPosition()
  }
}
