package org.nypl.simplified.books.api

import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.books.api.BookFormat.BookFormatAudioBook
import org.nypl.simplified.books.api.BookFormat.BookFormatEPUB
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import java.io.File

/**
 * A known book. A book is "known" if the user has at some point tried to borrow and/or download
 * the book. A book ceases to be known when the loan for it has been revoked and the local file(s)
 * deleted.
 */

data class Book(

  /**
   * The unique ID of the book.
   */

  val id: BookID,

  /**
   * The account that owns the book.
   */

  val account: AccountID,

  /**
   * The cover file, if any.
   */

  val cover: File?,

  /**
   * The thumbnail file, if any.
   */

  val thumbnail: File?,

  /**
   * @return The acquisition feed entry
   */

  val entry: OPDSAcquisitionFeedEntry,

  /**
   * The available formats.
   */

  val formats: List<BookFormat>

) {

  /**
   * If any format is downloaded, then the book as a whole is currently considered to be downloaded
   */

  val isDownloaded: Boolean
    get() = this.formats.any { format -> format.isDownloaded }

  /**
   * @return The format of the given type, if one is present
   */

  fun <T : BookFormat> findFormat(clazz: Class<T>): T? {
    return this.formats.find { format -> clazz.isAssignableFrom(format.javaClass) } as T?
  }

  /**
   * @return The "preferred" format for the given type, if any
   */

  fun findPreferredFormat(): BookFormat? {
    val formats = mutableListOf<BookFormat>()
    formats.addAll(this.formats.filterIsInstance(BookFormatEPUB::class.java))
    formats.addAll(this.formats.filterIsInstance(BookFormatAudioBook::class.java))
    formats.addAll(this.formats)
    return formats.firstOrNull()
  }
}
