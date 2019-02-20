package org.nypl.simplified.books.reader.bookmarks

import org.nypl.simplified.books.reader.ReaderBookmark
import java.io.Serializable

/**
 * A set of bookmarks for a book.
 *
 * <p>Note: The type is {@link Serializable} purely because the Android API requires this
 * in order pass values of this type between activities. We make absolutely no guarantees
 * that serialized values of this class will be compatible with future releases.</p>
 */

data class ReaderBookmarks(
  val lastRead: ReaderBookmark?,
  val bookmarks: List<ReaderBookmark>): Serializable
