package org.nypl.simplified.books.core;

import com.io7m.jfunctional.OptionType;
import org.nypl.drm.core.AdobeAdeptLoan;
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
   * @throws IOException On I/O errors or lock acquisition failures
   */

  void copyInBookFromSameFilesystem(
    File file)
    throws IOException;

  /**
   * Copy the given file into the directory as the book data. Typically, this
   * will be an EPUB file.
   *
   * @param file The file to be copied
   *
   * @throws IOException On I/O errors or lock acquisition failures
   */

  void copyInBook(
    File file)
    throws IOException;

  /**
   * Create the book directory if it does not exist.
   *
   * @throws IOException On I/O errors or lock acquisition failures
   */

  void create()
    throws IOException;

  /**
   * Destroy the book directory and all of its contents.
   *
   * @throws IOException On I/O errors or lock acquisition failures
   */

  void destroy()
    throws IOException;

  /**
   * Destroy the book data, if it exists.
   *
   * @throws IOException On I/O errors or lock acquisition failures
   */

  void destroyBookData()
    throws IOException;

  /**
   * Set the acquisition feed entry of the book
   *
   * @param in_entry The feed entry
   *
   * @throws IOException On I/O errors or lock acquisition failures
   */

  void setData(
    OPDSAcquisitionFeedEntry in_entry)
    throws IOException;

  /**
   * Set the cover of the book
   *
   * @param in_cover The cover
   *
   * @throws IOException On I/O errors or lock acquisition failures
   */

  void setCover(
    OptionType<File> in_cover)
    throws IOException;

  /**
   * Set the Adobe rights information for the book.
   *
   * @param loan The loan
   *
   * @throws IOException On I/O errors or lock acquisition failures
   */

  void setAdobeRightsInformation(
    OptionType<AdobeAdeptLoan> loan)
    throws IOException;
}
