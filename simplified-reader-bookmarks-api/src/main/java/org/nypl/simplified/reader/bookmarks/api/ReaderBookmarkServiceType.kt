package org.nypl.simplified.reader.bookmarks.api

/**
 * The reader bookmark service interface.
 */

interface ReaderBookmarkServiceType : AutoCloseable, ReaderBookmarkServiceUsableType {

  override fun close()

}

