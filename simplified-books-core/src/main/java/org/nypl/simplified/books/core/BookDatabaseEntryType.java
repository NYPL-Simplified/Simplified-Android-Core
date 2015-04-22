package org.nypl.simplified.books.core;

import java.io.File;
import java.io.IOException;

import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;

import com.io7m.jfunctional.OptionType;

/**
 * A book database entry.
 */

public interface BookDatabaseEntryType
{
  /**
   * Copy the given file into the directory as the book data. Typically, this
   * will be an EPUB file.
   *
   * @throws IOException
   *           On I/O errors or lock acquisition failures
   */

  void copyInBook(
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
   * @return <tt>true</tt> if the book directory exists.
   */

  boolean exists();

  /**
   * @return The acquisition feed entry associated with the book
   * @throws IOException
   *           On I/O errors or lock acquisition failures
   */

  OPDSAcquisitionFeedEntry getData()
    throws IOException;

  /**
   * @return The database entry directory
   */

  File getDirectory();

  /**
   * @return The download ID associated with the book, if any
   * @throws IOException
   *           On I/O errors or lock acquisition failures
   */

  OptionType<Long> getDownloadID()
    throws IOException;

  /**
   * @return The ID of the book
   */

  BookID getID();

  /**
   * @return A snapshot of the current book state
   * @throws IOException
   *           On I/O errors or lock acquisition failures
   */

  BookSnapshot getSnapshot()
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
