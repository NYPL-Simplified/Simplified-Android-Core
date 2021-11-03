package org.nypl.simplified.books.api

import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import java.io.Serializable
import java.util.regex.Pattern

/**
 * The unique identifier for a given book. This is typically a SHA256 hash of the original book
 * URI and is intended to be safe for use as a file or directory name.
 */
class BookID private constructor(
  private val id: String,
) : Comparable<BookID>, Serializable {
  /**
   * @return The actual PIN value
   */
  override fun toString(): String =
    id

  /**
   * @return The brief form of the ID
   */
  fun brief(): String =
    toString().take(8)

  override fun compareTo(other: BookID): Int {
    return id.compareTo(other.id)
  }

  override fun equals(other: Any?): Boolean {
    return other is BookID && id == other.id
  }

  override fun hashCode(): Int {
    return id.hashCode()
  }

  /**
   * @return If this ID is a new one or an old one.
   */

  val isOldFashion: Boolean
    get() = id.length == 64

  companion object {
    /**
     * The regular expression that defines a valid book ID.
     */
    private val VALID_BOOK_ID: Pattern = Pattern.compile("[a-z0-9]+")

    /**
     * Construct a book ID.
     *
     * @param in_value The raw book ID value
     */
    fun create(in_value: String): BookID {
      require(VALID_BOOK_ID.matcher(in_value).matches()) {
        "Book IDs must be non-empty, alphanumeric lowercase (" + VALID_BOOK_ID.pattern() + ")"
      }

      return BookID(in_value)
    }

    /**
     * Construct a book ID derived from the hash of the given text.
     *
     * @param text The text
     * @return A new book ID
     */

    fun newFromText(text: String): BookID =
      create(text.sha256())

    /**
     * Calculate an old-fashion book ID from the given acquisition feed entry.
     *
     * @param e The entry
     *
     * @return A new book ID
     */
    fun newFromOPDSEntry(
      e: OPDSAcquisitionFeedEntry
    ): BookID {
      return create(e.id.sha256())
    }

    /**
     * Construct a book ID derived from the hashes of the given OPDS entry ID and account ID.
     *
     * The new IDs based on both OPDS entry IDs and account IDs are prefixed with `x`
     * to be easily distinguished from the old ones.
     *
     * @param opdsId The OPDS entry ID
     * @param accountId The account ID
     *
     * @return A new book ID
     */

    fun newFromOPDSAndAccount(
      opdsId: String,
      accountId: AccountID
    ): BookID {
      return create("x" + (opdsId.sha256() + accountId.toString().sha256()).sha256())
    }
  }
}
