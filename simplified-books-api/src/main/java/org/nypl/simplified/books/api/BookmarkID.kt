package org.nypl.simplified.books.api

import java.io.Serializable
import java.util.regex.Pattern

/**
 * A unique ID for a bookmark.
 *
 * <p>Note: The type is {@link Serializable} purely because the Android API requires this
 * in order pass values of this type between activities. We make absolutely no guarantees
 * that serialized values of this class will be compatible with future releases.</p>
 */

data class BookmarkID(val value: String) : Serializable {

  init {
    if (!VALID_BOOKMARK_ID.matcher(value).matches()) {
      throw IllegalArgumentException(
        "Bookmark IDs must be non-empty, alphanumeric lowercase (" + VALID_BOOKMARK_ID.pattern() + ")"
      )
    }
  }

  companion object {

    /**
     * The regular expression that defines a valid bookmark ID.
     */

    var VALID_BOOKMARK_ID: Pattern = Pattern.compile("[a-z0-9]+")
  }
}
