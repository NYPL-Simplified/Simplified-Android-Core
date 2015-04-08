package org.nypl.simplified.books.core;

import java.net.URI;
import java.util.Calendar;

import org.nypl.simplified.opds.core.OPDSAcquisition;

public interface BooksRegistryType extends BooksStatusCacheType
{
  void bookBorrow(
    BookID id,
    OPDSAcquisition acq,
    BookBorrowListenerType listener);

  void bookDownloadAcknowledge(
    BookID id);

  void bookDownloadCancel(
    BookID id);

  void bookDownloadOpenAccess(
    BookID id,
    String title,
    URI uri);

  void booksGetAcquisitionFeed(
    URI in_uri,
    String in_id,
    Calendar in_updated,
    String in_title,
    BookAcquisitionFeedListenerType in_listener);
}
