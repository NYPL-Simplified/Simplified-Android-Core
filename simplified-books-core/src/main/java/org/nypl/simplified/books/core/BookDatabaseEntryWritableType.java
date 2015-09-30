package org.nypl.simplified.books.core;

import com.io7m.jfunctional.OptionType;
import org.nypl.drm.core.AdobeAdeptLoan;
import org.nypl.simplified.http.core.HTTPType;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;

import java.io.File;
import java.io.IOException;

/**
 * <p>The writable interface supported by book database entries.</p>
 *
 * <p>These are blocking operations that imply disk I/O.</p>
 */

public interface BookDatabaseEntryWritableType
{
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

  BookDatabaseEntrySnapshot entryCopyInBookFromSameFilesystem(
    File file)
    throws IOException;

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

  BookDatabaseEntrySnapshot entryCopyInBook(
    File file)
    throws IOException;

  /**
   * Create the book directory if it does not exist.
   *
   * @param in_entry The feed entry
   * @return A snapshot of the new database state
   * @throws IOException On I/O errors or lock acquisition failures
   */

  BookDatabaseEntrySnapshot entryCreate(OPDSAcquisitionFeedEntry in_entry)
    throws IOException;

  /**
   * Destroy the book directory and all of its contents.
   *
   * @throws IOException On I/O errors or lock acquisition failures
   */

  void entryDestroy()
    throws IOException;

  /**
   * Destroy the book data, if it exists.
   *
   * @return A snapshot of the new database state
   *
   * @throws IOException On I/O errors or lock acquisition failures
   */

  BookDatabaseEntrySnapshot entryDeleteBookData()
    throws IOException;

  /**
   * Set the acquisition feed entry of the book
   *
   * @param in_entry The feed entry
   *
   * @return A snapshot of the new database state
   *
   * @throws IOException On I/O errors or lock acquisition failures
   */

  BookDatabaseEntrySnapshot entrySetFeedData(
    OPDSAcquisitionFeedEntry in_entry)
    throws IOException;

  /**
   * Set the cover of the book
   *
   * @param in_cover The cover
   *
   * @return A snapshot of the new database state
   *
   * @throws IOException On I/O errors or lock acquisition failures
   */

  BookDatabaseEntrySnapshot entrySetCover(
    OptionType<File> in_cover)
    throws IOException;

  /**
   * Set the Adobe rights information for the book.
   *
   * @param loan The loan
   *
   * @return A snapshot of the new database state
   *
   * @throws IOException On I/O errors or lock acquisition failures
   */

  BookDatabaseEntrySnapshot entrySetAdobeRightsInformation(
    OptionType<AdobeAdeptLoan> loan)
    throws IOException;

  /**
   * Update the book data based on {@code e}. The cover, if any, will be fetched
   * using the http interface {@code http}, and the new status will be published
   * to {@code books_status}.
   *
   * @param e            The feed entry
   * @param books_status The book status cache
   * @param http         The HTTP interface
   *
   * @return A snapshot of the new database state
   *
   * @throws IOException On I/O errors or lock acquisition failures
   */

  BookDatabaseEntrySnapshot entryUpdateAll(
    OPDSAcquisitionFeedEntry e,
    BooksStatusCacheType books_status,
    HTTPType http)
    throws IOException;
}
