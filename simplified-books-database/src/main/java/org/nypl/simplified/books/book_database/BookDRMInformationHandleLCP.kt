package org.nypl.simplified.books.book_database

import org.nypl.simplified.books.api.BookDRMInformation
import org.nypl.simplified.books.api.BookDRMKind
import org.nypl.simplified.books.book_database.api.BookDRMInformationHandle
import org.nypl.simplified.books.book_database.api.BookFormats
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * An information handle for LCP.
 */

class BookDRMInformationHandleLCP(
  directory: File,
  format: BookFormats.BookFormatDefinition
) : BookDRMInformationHandle.LCPHandle(), BookDRMInformationHandleBase {

  private val closed = AtomicBoolean(false)

  init {
    BookDRMInformationHandles.writeDRMInfo(
      directory = directory,
      format = format,
      kind = BookDRMKind.LCP
    )
  }

  override val info: BookDRMInformation.LCP
    get() {
      check(!this.closed.get()) { "Handle must not have been closed" }
      return BookDRMInformation.LCP()
    }

  override fun close() {
    this.closed.compareAndSet(false, true)
  }
}
