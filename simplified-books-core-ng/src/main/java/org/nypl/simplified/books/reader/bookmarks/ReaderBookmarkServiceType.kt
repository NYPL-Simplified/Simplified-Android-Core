package org.nypl.simplified.books.reader.bookmarks

/**
 * The reader bookmark service interface.
 */

interface ReaderBookmarkServiceType : AutoCloseable, ReaderBookmarkServiceUsableType {

  override fun close()

}

