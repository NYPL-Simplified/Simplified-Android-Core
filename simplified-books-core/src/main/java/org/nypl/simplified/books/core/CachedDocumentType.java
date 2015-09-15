package org.nypl.simplified.books.core;

import java.io.InputStream;

/**
 * The type of documents that may have a locally cached copy, but aren't
 * guaranteed to be available at any particular time.
 */

public interface CachedDocumentType
{
  /**
   * Update the cached copy of the document.
   *
   * @param data The data
   */

  void documentUpdate(InputStream data);
}
