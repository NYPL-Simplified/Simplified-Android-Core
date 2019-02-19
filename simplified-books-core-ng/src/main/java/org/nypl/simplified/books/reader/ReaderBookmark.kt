package org.nypl.simplified.books.reader

import com.io7m.jfunctional.Some
import org.joda.time.LocalDateTime
import org.nypl.simplified.books.accounts.AccountID
import org.nypl.simplified.books.book_database.BookID
import org.nypl.simplified.books.book_database.BookIDs
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkKind
import java.io.Serializable
import java.net.URI
import java.nio.charset.Charset
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * The saved data for a bookmark.
 *
 * <p>Note: The type is {@link Serializable} purely because the Android API requires this
 * in order pass values of this type between activities. We make absolutely no guarantees
 * that serialized values of this class will be compatible with future releases.</p>
 */

data class ReaderBookmark(

  /**
   * The identifier of the book taken from the OPDS entry that provided it.
   */

  val opdsId: String,

  /**
   * The location of the bookmark.
   */

  val location: ReaderBookLocation,

  /**
   * The kind of bookmark.
   */

  val kind: ReaderBookmarkKind,

  /**
   * The time the bookmark was created.
   */

  val time: LocalDateTime,

  /**
   * The title of the chapter.
   */

  val chapterTitle: String,

  /**
   * An estimate of the current chapter progress, in the range [0, 1]
   */

  val chapterProgress: Double,

  /**
   * An estimate of the current book progress, in the range [0, 1]
   */

  val bookProgress: Double,

  /**
   * The identifier of the device that created the bookmark, if one is available.
   */

  val deviceID: String?,

  /**
   * The URI of this bookmark, if the bookmark exists on a remote server.
   */

  val uri: URI?): Serializable {

  /**
   * The ID of the book to which the bookmark belongs.
   */

  val book: BookID = BookIDs.newFromText(this.opdsId)

  /**
   * The unique ID of the bookmark.
   */

  val bookmarkId: ReaderBookmarkID = createBookmarkID(this.book, this.location)

  companion object {

    /**
     * Create a bookmark ID from the given account ID, book ID, and location.
     */

    fun createBookmarkID(
      book: BookID,
      location: ReaderBookLocation): ReaderBookmarkID {
      try {
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val utf8 = Charset.forName("UTF-8")
        messageDigest.update(book.value().toByteArray(utf8))
        val cfiOpt = location.contentCFI()
        if (cfiOpt is Some<String>) {
          messageDigest.update(cfiOpt.get().toByteArray(utf8))
        }
        messageDigest.update(location.idRef().toByteArray(utf8))

        val digestResult = messageDigest.digest()
        val builder = StringBuilder(64)
        for (index in digestResult.indices) {
          val bb = digestResult[index]
          builder.append(String.format("%02x", bb))
        }

        return ReaderBookmarkID(builder.toString())
      } catch (e: NoSuchAlgorithmException) {
        throw IllegalStateException(e)
      }
    }
  }

}
