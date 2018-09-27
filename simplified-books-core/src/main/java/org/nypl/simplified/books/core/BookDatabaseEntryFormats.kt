package org.nypl.simplified.books.core

import com.io7m.jfunctional.OptionType
import org.nypl.drm.core.AdobeAdeptLoan
import org.nypl.simplified.books.core.BookDatabaseEntryFormatSnapshot.BookDatabaseEntryFormatSnapshotAudioBook
import org.nypl.simplified.books.core.BookDatabaseEntryFormatSnapshot.BookDatabaseEntryFormatSnapshotEPUB
import java.io.File
import java.io.IOException

/**
 * The type of book format handles in database entries.
 */

sealed class BookDatabaseEntryFormatHandle {

  /**
   * @return The format definition
   */

  abstract val formatDefinition: BookFormats.BookFormatDefinition

  /**
   * @return A snapshot of the current format
   */

  abstract fun snapshot(): BookDatabaseEntryFormatSnapshot

  /**
   * The interface exposed by the EPUB format in database entries.
   */

  abstract class BookDatabaseEntryFormatHandleEPUB : BookDatabaseEntryFormatHandle() {

    /**
     * Copy the given EPUB file into the directory as the book data.
     *
     * @param file The file to be copied
     *
     * @return A snapshot of the new database state
     *
     * @throws IOException On I/O errors or lock acquisition failures
     */

    @Throws(IOException::class)
    abstract fun copyInBook(file: File): BookDatabaseEntrySnapshot

    /**
     * Destroy the book data, if it exists.
     *
     * @return A snapshot of the new database state
     *
     * @throws IOException On I/O errors or lock acquisition failures
     */

    @Throws(IOException::class)
    abstract fun deleteBookData(): BookDatabaseEntrySnapshot

    /**
     * Set the Adobe rights information for the book.
     *
     * @param loan The loan
     *
     * @return A snapshot of the new database state
     *
     * @throws IOException On I/O errors or lock acquisition failures
     */

    @Throws(IOException::class)
    abstract fun setAdobeRightsInformation(loan: OptionType<AdobeAdeptLoan>): BookDatabaseEntrySnapshot

    abstract override fun snapshot(): BookDatabaseEntryFormatSnapshotEPUB
  }

  /**
   * The interface exposed by the audio book format in database entries.
   */

  abstract class BookDatabaseEntryFormatHandleAudioBook : BookDatabaseEntryFormatHandle() {

    abstract override fun snapshot(): BookDatabaseEntryFormatSnapshotAudioBook

  }

}

/**
 * The type of book format snapshots.
 */

sealed class BookDatabaseEntryFormatSnapshot {

  /**
   * A snapshot of an EPUB
   */

  data class BookDatabaseEntryFormatSnapshotEPUB(

    /**
     * The Adobe rights information, if any
     */

    val adobeRights: OptionType<AdobeAdeptLoan>,

    /**
     * The EPUB file, if one has been downloaded
     */

    val book: OptionType<File>) : BookDatabaseEntryFormatSnapshot() {

    override val isDownloaded: Boolean
      get() = this.book.isSome
  }

  /**
   * A snapshot of an audio book
   */

  class BookDatabaseEntryFormatSnapshotAudioBook() : BookDatabaseEntryFormatSnapshot() {

    /*
     * Audio books are always considered to be downloaded, because the actual downloading
     * of parts generally happens inside audio engines and cannot be otherwise observed.
     */

    override val isDownloaded: Boolean
      get() = true
  }

  /**
   * @return `true` iff the book data for the format is downloaded
   */

  abstract val isDownloaded: Boolean

}
