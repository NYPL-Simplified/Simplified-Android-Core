package org.nypl.simplified.books.core;

import java.io.Serializable;

/**
 * <p>
 * A book database entry reference.
 * </p>
 * <p>
 * References are {@link Serializable} and therefore can be passed between
 * processes. However, processes running under different user IDs are not
 * guaranteed to be able to perform any of the operations.
 * </p>
 */

public interface BookDatabaseEntryType extends
  BookDatabaseEntryReadableType,
  BookDatabaseEntryWritableType
{
  // No extra functions
}
