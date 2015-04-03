package org.nypl.simplified.books.core;

import java.net.URI;

import org.nypl.simplified.opds.core.OPDSAcquisition;

public interface BooksRegistryType extends BooksStatusCacheType
{
  void bookBorrow(
    BookID id,
    OPDSAcquisition acq,
    BookBorrowListenerType listener);

  void bookDownloadOpenAccess(
    BookID id,
    String title,
    URI uri);

  void bookDownloadCancel(
    BookID id);

  void bookDownloadAcknowledge(
    BookID id);
}
