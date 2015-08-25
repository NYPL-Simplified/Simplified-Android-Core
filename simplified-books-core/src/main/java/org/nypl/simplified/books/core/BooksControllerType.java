package org.nypl.simplified.books.core;

import com.io7m.jfunctional.OptionType;
import org.nypl.simplified.opds.core.OPDSAcquisition;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;

import java.net.URI;
import java.util.Calendar;

/**
 * Interface to the book management functions.
 */

public interface BooksControllerType
{
  /**
   * @return A reference to the book status cache that can be used to
   * subscribe/unsubscribe to book status updates.
   */

  BooksStatusCacheType bookGetStatusCache();

  /**
   * Borrow the given book, delivering the results to the given {@code
   * listener}.
   *
   * @param id       The book ID
   * @param acq      The specific acquisition relation
   * @param eo       The feed entry
   * @param listener The listener
   */

  void bookBorrow(
    BookID id,
    OPDSAcquisition acq,
    OPDSAcquisitionFeedEntry eo,
    BookBorrowListenerType listener);

  /**
   * Delete the actual book file for the given book, if any.
   *
   * @param id The book ID
   */

  void bookDeleteData(
    BookID id);

  /**
   * Cancel the download of the book with the given {@code id}.
   *
   * @param id The book ID
   */

  void bookDownloadCancel(
    BookID id);

  /**
   * Acknowledge the failed download of book {@code id}, if any.
   *
   * @param id The book ID
   */

  void bookDownloadAcknowledge(
    BookID id);

  /**
   * Retrieve an acquisition feed of  books on the current account, delivering
   * the results to the given {@code listener}.
   *
   * @param in_uri          The URI that will be used for the feed
   * @param in_id           The ID that will be used for the feed
   * @param in_updated      The time that will be used for the "last updated"
   *                        time in the feed
   * @param in_title        The title that will be used for the feed
   * @param in_facet_active The facet that will be active in the feed
   * @param in_facet_group  The facet group
   * @param in_facet_titles A facet title provider
   * @param in_search       The search that will be performed on the feed
   * @param in_selection    The type of feed that will be generated
   * @param in_listener     The listener that will receive the feed
   */

  void booksGetFeed(
    URI in_uri,
    String in_id,
    Calendar in_updated,
    String in_title,
    FeedFacetPseudo.FacetType in_facet_active,
    String in_facet_group,
    FeedFacetPseudoTitleProviderType in_facet_titles,
    OptionType<String> in_search,
    BooksFeedSelection in_selection,
    BookFeedListenerType in_listener);

  /**
   * Update metadata for the given book.
   *
   * @param id The book ID
   * @param e  The feed entry
   */

  void bookUpdateMetadata(
    BookID id,
    OPDSAcquisitionFeedEntry e);

  /**
   * Revoke a loan or hold for the given book.
   *
   * @param id The book ID
   */

  void bookRevoke(
    BookID id);

  /**
   * Load the latest book status from disk, and broadcast it to any and all
   * observers.
   *
   * @param id The book ID
   */

  void bookGetLatestStatusFromDisk(BookID id);
}
