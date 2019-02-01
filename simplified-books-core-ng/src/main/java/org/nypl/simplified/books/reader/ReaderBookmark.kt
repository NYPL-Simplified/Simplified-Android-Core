package org.nypl.simplified.books.reader

import org.joda.time.LocalDateTime
import org.nypl.simplified.books.book_database.BookID
import java.io.Serializable

/**
 * The saved data for a bookmark.
 *
 * <p>Note: The type is {@link Serializable} purely because the Android API requires this
 * in order pass values of this type between activities. We make absolutely no guarantees
 * that serialized values of this class will be compatible with future releases.</p>
 */

data class ReaderBookmark(

  /**
   * The ID of the book to which the bookmark belongs.
   */

  val book: BookID,

  /**
   * The location of the bookmark.
   */

  val location: ReaderBookLocation,

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

  val deviceID: String?): Serializable
