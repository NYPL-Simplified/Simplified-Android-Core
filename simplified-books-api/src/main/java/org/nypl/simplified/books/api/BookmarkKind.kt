package org.nypl.simplified.books.api

import java.io.Serializable
import java.lang.IllegalArgumentException

/**
 * The kind of bookmarks.
 *
 * <p>Note: The type is {@link Serializable} purely because the Android API requires this
 * in order pass values of this type between activities. We make absolutely no guarantees
 * that serialized values of this class will be compatible with future releases.</p>
 */

sealed class BookmarkKind(
  val motivationURI: String
) : Serializable {

  /**
   * The bookmark represents a last-read location.
   */

  object ReaderBookmarkLastReadLocation :
    BookmarkKind("http://librarysimplified.org/terms/annotation/idling") {
    override fun toString(): String {
      return "ReaderBookmarkLastReadLocation"
    }
  }

  /**
   * The bookmark represents an explicitly created bookmark.
   */

  object ReaderBookmarkExplicit :
    BookmarkKind("http://www.w3.org/ns/oa#bookmarking") {
    override fun toString(): String {
      return "ReaderBookmarkExplicit"
    }
  }

  companion object {

    val obsoleteBookmarkingURI =
      "https://www.w3.org/ns/oa#bookmarking"

    fun ofMotivation(motivationURI: String): BookmarkKind {
      return when (motivationURI) {
        ReaderBookmarkLastReadLocation.motivationURI ->
          ReaderBookmarkLastReadLocation
        ReaderBookmarkExplicit.motivationURI ->
          ReaderBookmarkExplicit
        obsoleteBookmarkingURI ->
          ReaderBookmarkExplicit
        else ->
          throw IllegalArgumentException("Unrecognized motivation: $motivationURI")
      }
    }
  }
}
