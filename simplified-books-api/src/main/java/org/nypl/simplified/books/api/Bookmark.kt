package org.nypl.simplified.books.api

import org.joda.time.LocalDateTime
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

data class Bookmark(

  /**
   * The identifier of the book taken from the OPDS entry that provided it.
   */

  val opdsId: String,

  /**
   * The location of the bookmark.
   */

  val location: BookLocation,

  /**
   * The kind of bookmark.
   */

  val kind: BookmarkKind,

  /**
   * The time the bookmark was created.
   */

  val time: LocalDateTime,

  /**
   * The title of the chapter.
   */

  val chapterTitle: String,

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

  val uri: URI?
) : Serializable {

  /**
   * An estimate of the current chapter progress, in the range [0, 1]
   */

  val chapterProgress: Double =
    this.location.progress?.chapterProgress ?: 0.0

  /**
   * The ID of the book to which the bookmark belongs.
   */

  val book: BookID = BookIDs.newFromText(this.opdsId)

  /**
   * The unique ID of the bookmark.
   */

  val bookmarkId: BookmarkID = createBookmarkID(this.book, this.location, this.kind)

  /**
   * Convenience function to convert a bookmark to a last-read-location kind.
   */

  fun toLastReadLocation(): Bookmark {
    return this.copy(kind = BookmarkKind.ReaderBookmarkLastReadLocation)
  }

  /**
   * Convenience function to convert a bookmark to an explicit kind.
   */

  fun toExplicit(): Bookmark {
    return this.copy(kind = BookmarkKind.ReaderBookmarkExplicit)
  }

  companion object {

    /**
     * Create a bookmark ID from the given book ID, location, and kind.
     */

    fun createBookmarkID(
      book: BookID,
      location: BookLocation,
      kind: BookmarkKind
    ): BookmarkID {
      try {
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val utf8 = Charset.forName("UTF-8")
        messageDigest.update(book.value().toByteArray(utf8))

        val chapterProgress = location.progress
        if (chapterProgress != null) {
          messageDigest.update(chapterProgress.chapterIndex.toString().toByteArray(utf8))
          val truncatedProgress = String.format("%.6f", chapterProgress.chapterProgress)
          messageDigest.update(truncatedProgress.toByteArray(utf8))
        }

        val cfi = location.contentCFI
        if (cfi != null) {
          messageDigest.update(cfi.toByteArray(utf8))
        }
        val idRef = location.idRef
        if (idRef != null) {
          messageDigest.update(idRef.toByteArray(utf8))
        }
        messageDigest.update(kind.motivationURI.toByteArray(utf8))

        val digestResult = messageDigest.digest()
        val builder = StringBuilder(64)
        for (index in digestResult.indices) {
          val bb = digestResult[index]
          builder.append(String.format("%02x", bb))
        }

        return BookmarkID(builder.toString())
      } catch (e: NoSuchAlgorithmException) {
        throw IllegalStateException(e)
      }
    }
  }
}
