package org.nypl.simplified.books.core;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;

import com.io7m.jfunctional.OptionType;

/**
 * <p>
 * The readable interface supported by book database entries.
 * </p>
 * <p>
 * References are {@link Serializable} and therefore can be passed between
 * processes. However, processes running under different user IDs are not
 * guaranteed to be able to perform any of the operations.
 * </p>
 */

public interface BookDatabaseEntryReadableType extends Serializable
{
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

}
