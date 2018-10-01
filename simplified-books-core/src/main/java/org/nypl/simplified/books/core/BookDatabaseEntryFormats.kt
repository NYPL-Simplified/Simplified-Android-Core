package org.nypl.simplified.books.core

import com.io7m.jfunctional.OptionType
import com.io7m.jfunctional.Some
import org.nypl.drm.core.AdobeAdeptLoan
import org.nypl.simplified.books.core.BookDatabaseEntryFormatSnapshot.BookDatabaseEntryFormatSnapshotAudioBook
import org.nypl.simplified.books.core.BookDatabaseEntryFormatSnapshot.BookDatabaseEntryFormatSnapshotEPUB
import java.io.File
import java.io.IOException
import java.net.URI

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

    /**
     * Save the manifest and the URI that can be used to fetch more up-to-date copies of it
     * later.
     *
     * @throws IOException On I/O errors or lock acquisition failures
     */

    @Throws(IOException::class)
    abstract fun copyInManifestAndURI(file: File, manifestURI: URI)

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
   * A reference to an audio book manifest.
   */

  data class AudioBookManifestReference(

    /**
     * The URI that can be used to fetch a more recent copy of the manifest.
     */

    val manifestURI: URI,

    /**
     * The most recent copy of the audio book manifest, if any has been fetched.
     */

    val manifestFile: File)

  /**
   * A snapshot of an audio book
   */

  class BookDatabaseEntryFormatSnapshotAudioBook(
    val manifest: OptionType<AudioBookManifestReference>) : BookDatabaseEntryFormatSnapshot() {

    /*
     * Audio books are downloaded if there's a manifest available.
     */

    override val isDownloaded: Boolean
      get() = this.manifest.isSome
  }

  /**
   * @return `true` iff the book data for the format is downloaded
   */

  abstract val isDownloaded: Boolean

}
