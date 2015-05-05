package org.nypl.simplified.books.core;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;

import com.io7m.jfunctional.OptionType;

/**
 * <p>
 * The writable interface supported by book database entries.
 * </p>
 * <p>
 * References are {@link Serializable} and therefore can be passed between
 * processes. However, processes running under different user IDs are not
 * guaranteed to be able to perform any of the operations.
 * </p>
 */

public interface BookDatabaseEntryWritableType extends Serializable
{
  /**
   * Copy the given file into the directory as the book data. Typically, this
   * will be an EPUB file. This function will instantly fail if `file` is not
   * on the same filesystem as the book database.
   *
   * @throws IOException
   *           On I/O errors or lock acquisition failures
   */

  void copyInBookFromSameFilesystem(
    File file)
    throws IOException;

  /**
   * Create the book directory if it does not exist.
   *
   * @throws IOException
   *           On I/O errors or lock acquisition failures
   */

  void create()
    throws IOException;

  /**
   * Destroy the book directory and all of its contents.
   *
   * @throws IOException
   *           On I/O errors or lock acquisition failures
   */

  void destroy()
    throws IOException;

  /**
   * Destroy the book data, if it exists.
   *
   * @throws IOException
   *           On I/O errors or lock acquisition failures
   */

  void destroyBookData()
    throws IOException;

  /**
   * Set the cover and acquisition feed entry of the book
   *
   * @throws IOException
   *           On I/O errors or lock acquisition failures
   */

  void setData(
    OptionType<File> in_cover,
    OPDSAcquisitionFeedEntry in_entry)
    throws IOException;

  /**
   * Set the download ID of the book.
   *
   * @throws IOException
   *           On I/O errors or lock acquisition failures
   */

  void setDownloadID(
    long did)
    throws IOException;

}
