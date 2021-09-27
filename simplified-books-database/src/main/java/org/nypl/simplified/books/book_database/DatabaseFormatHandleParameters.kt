package org.nypl.simplified.books.book_database

import android.content.Context
import com.fasterxml.jackson.databind.ObjectMapper
import one.irradia.mime.api.MIMEType
import org.nypl.simplified.books.api.BookFormat
import org.nypl.simplified.books.api.BookID
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryType
import org.nypl.simplified.books.formats.api.BookFormatSupportType
import java.io.File

/**
 * Parameters passed to database format handles.
 */

internal data class DatabaseFormatHandleParameters(

  /**
   * An Android context.
   */

  val context: Context,

  /**
   * The ID of the book to which the owning database entry belongs.
   */

  val bookID: BookID,

  /**
   * The directory containing data for the database entry.
   */

  val directory: File,

  /**
   * A callback to be executed whenever something causes the contents of a format handle
   * to change. In practice, this is used by the book database to update its internal snapshots
   * of book states.
   */

  val onUpdated: (BookFormat) -> Unit,

  /**
   * The database entry that owns the format handle.
   */

  val entry: BookDatabaseEntryType,

  /**
   * The MIME type of the format handle.
   */

  val contentType: MIMEType,

  /**
   * A JSON object mapper.
   */

  val objectMapper: ObjectMapper,

  /**
   * The book format support.
   */

  val bookFormatSupport: BookFormatSupportType
)
