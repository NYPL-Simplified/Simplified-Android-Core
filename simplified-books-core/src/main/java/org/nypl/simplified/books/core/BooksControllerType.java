package org.nypl.simplified.books.core;

import java.net.URI;
import java.util.Calendar;

import org.nypl.simplified.opds.core.OPDSAcquisition;

/**
 * Interface to the book management functions.
 */

public interface BooksControllerType extends BooksStatusCacheType
{
  /**
   * Borrow the given book, delivering the results to the given
   * <tt>listener</tt>.
   */

  void bookBorrow(
    BookID id,
    OPDSAcquisition acq,
    BookBorrowListenerType listener);

  /**
   * Acknowledge the completed download of the book with the given <tt>id</tt>
   * .
   */

  void bookDownloadAcknowledge(
    BookID id);

  /**
   * Cancel the download of the book with the given <tt>id</tt>.
   */

  void bookDownloadCancel(
    BookID id);

  /**
   * Download the given open access book (which is assumed to have already
   * been borrowed with
   * {@link #bookBorrow(BookID, OPDSAcquisition, BookBorrowListenerType)}).
   */

  void bookDownloadOpenAccess(
    BookID id,
    String title,
    URI uri);

  /**
   * Retrieve an acquisition feed of all owned/borrowed books on the current
   * account, delivering the results to the given <tt>listener</tt>.
   */

  void booksGetAcquisitionFeed(
    URI in_uri,
    String in_id,
    Calendar in_updated,
    String in_title,
    BookAcquisitionFeedListenerType in_listener);
}
