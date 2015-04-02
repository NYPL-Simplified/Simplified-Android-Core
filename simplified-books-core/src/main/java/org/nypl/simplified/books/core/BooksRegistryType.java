package org.nypl.simplified.books.core;

import java.net.URI;

public interface BooksRegistryType extends BooksStatusCacheType
{
  void bookDownloadOpenAccess(
    BookID id,
    String title,
    URI uri);

  void bookDownloadCancel(
    BookID id);

  void bookDownloadAcknowledge(
    BookID id);
}
