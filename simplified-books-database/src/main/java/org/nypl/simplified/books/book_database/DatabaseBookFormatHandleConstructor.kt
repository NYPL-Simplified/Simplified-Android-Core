package org.nypl.simplified.books.book_database

import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle
import org.nypl.simplified.mime.MIMEType

/**
 * A format handle constructor for a particular book format.
 */

internal data class DatabaseBookFormatHandleConstructor(

  /**
   * The precise implementation class of the format. This is used as unique identifier for the
   * database entry format implementation.
   */

  val classType: Class<out BookDatabaseEntryFormatHandle>,

  /**
   * The set of content types that will trigger the creation of a format.
   */

  val supportedContentTypes: Set<MIMEType>,

  /**
   * A function to construct a format given an existing database entry.
   */

  val constructor: (DatabaseFormatHandleParameters) -> BookDatabaseEntryFormatHandle)
