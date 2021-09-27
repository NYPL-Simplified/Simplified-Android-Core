package org.nypl.simplified.books.api

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
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

data class Bookmark private constructor(

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

  val time: DateTime,

  /**
   * The title of the chapter.
   */

  val chapterTitle: String,

  /**
   * An estimate of the current book progress, in the range [0, 1]
   */

  @Deprecated("Use progress information from the BookLocation")
  val bookProgress: Double?,

  /**
   * The identifier of the device that created the bookmark, if one is available.
   */

  val deviceID: String,

  /**
   * The URI of this bookmark, if the bookmark exists on a remote server.
   */

  val uri: URI?
) : Serializable {

  init {
    check(this.time.zone == DateTimeZone.UTC) {
      "Bookmark time zones must be UTC"
    }
  }

  /**
   * An estimate of the current chapter progress, in the range [0, 1]
   */

  val chapterProgress: Double =
    when (this.location) {
      is BookLocation.BookLocationR2 -> this.location.progress.chapterProgress
      is BookLocation.BookLocationR1 -> this.location.progress ?: 0.0
    }

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
     * Create a bookmark.
     */

    fun create(
      opdsId: String,
      location: BookLocation,
      kind: BookmarkKind,
      time: DateTime,
      chapterTitle: String,
      bookProgress: Double?,
      deviceID: String,
      uri: URI?
    ): Bookmark {
      return Bookmark(
        opdsId = opdsId,
        location = location,
        kind = kind,
        time = ensureUTC(time),
        chapterTitle = chapterTitle,
        bookProgress = bookProgress,
        deviceID = deviceID,
        uri = uri
      )
    }

    /**
     * Ensure a timestamp has a UTC timezone.
     */

    private fun ensureUTC(
      dateTime: DateTime
    ): DateTime {
      return dateTime.toDateTime(DateTimeZone.UTC)
    }

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

        when (location) {
          is BookLocation.BookLocationR2 -> {
            val chapterProgress = location.progress
            messageDigest.update(chapterProgress.chapterHref.toByteArray(utf8))
            val truncatedProgress = String.format("%.6f", chapterProgress.chapterProgress)
            messageDigest.update(truncatedProgress.toByteArray(utf8))
          }
          is BookLocation.BookLocationR1 -> {
            val cfi = location.contentCFI
            if (cfi != null) {
              messageDigest.update(cfi.toByteArray(utf8))
            }
            val idRef = location.idRef
            if (idRef != null) {
              messageDigest.update(idRef.toByteArray(utf8))
            }
          }
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
