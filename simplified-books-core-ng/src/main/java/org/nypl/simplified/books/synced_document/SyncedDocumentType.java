package org.nypl.simplified.books.synced_document;

import java.net.URL;

/**
 * The type of documents that have a locally cached copy but that should also be
 * regularly checked for updates on a remote server.
 */

public interface SyncedDocumentType
{
  /**
   * Notify the interface that the URL of the latest version of the document is
   * {@code u}.
   *
   * @param u The URL of the latest version of the document
   */

  void documentSetLatestURL(URL u);

  /**
   * @return The URL of the <i>currently available</i> document text. Note that
   * this is not the same as the {@link #documentSetLatestURL(URL)}: The
   * document is downloaded from the <i>latest</i> URL and saved to disk for
   * reading. The saved version is the <i>currently available</i> one.
   */

  URL documentGetReadableURL();
}
