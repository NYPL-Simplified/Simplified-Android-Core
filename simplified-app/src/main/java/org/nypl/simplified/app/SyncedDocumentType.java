package org.nypl.simplified.app;

import java.net.URI;
import java.net.URL;

/**
 * The type of documents that have a locally cached copy but that should also be
 * regularly checked for updates on a remote server.
 */

public interface SyncedDocumentType
{
  /**
   * @return The base name of the document. This should be unique to each
   * instance of the {@link SyncedDocumentType}.
   */

  String documentGetBaseName();

  /**
   * Notify the interface that the URI of the latest version of the EULA is
   * {@code u}.
   *
   * @param u The URI of the latest version of the EULA
   */

  void documentSetLatestURI(URI u);

  /**
   * @return The URL of the <i>currently available</i> document text. Note that
   * this is not the same as the {@link #documentSetLatestURI(URI)}: The
   * document is downloaded from the <i>latest</i> URI and saved to the device
   * storage for reading. The saved version is the <i>currently available</i>
   * one. If the document has never been downloaded, then the URL refers to a
   * bundled Android asset.
   */

  URL documentGetReadableURL();
}
