package org.nypl.simplified.books.core;

import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;

/**
 * Book management interface.
 */

public interface BooksType
{
  BookID bookAdd(
    final OPDSAcquisitionFeedEntry e);
}
