package org.nypl.simplified.books.core

import com.io7m.jfunctional.Option
import com.io7m.jfunctional.OptionType
import org.nypl.simplified.books.core.BookDatabaseEntryFormatSnapshot.BookDatabaseEntryFormatSnapshotAudioBook
import org.nypl.simplified.books.core.BookDatabaseEntryFormatSnapshot.BookDatabaseEntryFormatSnapshotEPUB
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import java.io.File

/**
 * A snapshot of the most recent state of a book in the database.
 */

data class BookDatabaseEntrySnapshot(

  /**
   * @return The book ID
   */

  val bookID: BookID,

  /**
   * @return The cover image, if any
   */

  val cover: OptionType<File>,

  /**
   * @return The acquisition feed entry
   */

  val entry: OPDSAcquisitionFeedEntry,

  /**
   * @return A snapshot of all formats within the database entry
   */

  val formats: List<BookDatabaseEntryFormatSnapshot>) {

  /**
   * If any format is downloaded, then the book as a whole is currently considered to be downloaded
   */

  val isDownloaded: Boolean
    get() = this.formats.any { format -> format.isDownloaded }

  /**
   * @return The format of the given type, if one is present
   */

  fun <T : BookDatabaseEntryFormatSnapshot> findFormat(clazz: Class<T>): OptionType<T> {
    return Option.of(this.formats.find { format -> clazz.isAssignableFrom(format.javaClass) } as T?)
  }

  /**
   * @return The "preferred" format for the given type, if any
   */

  fun findPreferredFormat(): OptionType<BookDatabaseEntryFormatSnapshot> {
    val formats = mutableListOf<BookDatabaseEntryFormatSnapshot>()
    formats.addAll(this.formats.filterIsInstance(
      BookDatabaseEntryFormatSnapshotEPUB::class.java))
    formats.addAll(this.formats.filterIsInstance(
      BookDatabaseEntryFormatSnapshotAudioBook::class.java))
    formats.addAll(this.formats)
    return Option.of(formats.firstOrNull())
  }
}
