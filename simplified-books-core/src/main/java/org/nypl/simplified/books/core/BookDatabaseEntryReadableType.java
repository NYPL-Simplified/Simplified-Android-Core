package org.nypl.simplified.books.core;

import com.io7m.jfunctional.OptionType;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;

import java.io.File;
import java.io.IOException;

/**
 * <p> The readable interface supported by book database entries. </p>
 */

public interface BookDatabaseEntryReadableType
{
  /**
   * @return <tt>true</tt> if the book directory exists.
   */

  boolean exists();

  /**
   * @return The cover of the book, if any
   *
   * @throws IOException On I/O errors or lock acquisition failures
   */

  OptionType<File> getCover()
    throws IOException;

  /**
   * @return The acquisition feed entry associated with the book
   *
   * @throws IOException On I/O errors or lock acquisition failures
   */

  OPDSAcquisitionFeedEntry getData()
    throws IOException;

  /**
   * @return The database entry directory
   */

  File getDirectory();

  /**
   * @return The ID of the book
   */

  BookID getID();

  /**
   * @return A snapshot of the current book state
   *
   * @throws IOException On I/O errors or lock acquisition failures
   */

  BookSnapshot getSnapshot()
    throws IOException;
}
