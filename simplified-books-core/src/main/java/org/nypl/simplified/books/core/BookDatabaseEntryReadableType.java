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
   * @return {@code true} if the book directory exists.
   */

  boolean entryExists();

  /**
   * @return The cover of the book, if any
   *
   * @throws IOException On I/O errors or lock acquisition failures
   */

  OptionType<File> entryGetCover()
    throws IOException;

  /**
   * @return The acquisition feed entry associated with the book
   *
   * @throws IOException On I/O errors or lock acquisition failures
   */

  OPDSAcquisitionFeedEntry entryGetFeedData()
    throws IOException;

  /**
   * @return The database entry directory
   */

  File entryGetDirectory();

  /**
   * @return The ID of the book
   */

  BookID entryGetBookID();

  /**
   * @return A snapshot of the current entry state
   *
   * @throws IOException On I/O errors or lock acquisition failures
   */

  BookDatabaseEntrySnapshot entryGetSnapshot()
    throws IOException;
}
