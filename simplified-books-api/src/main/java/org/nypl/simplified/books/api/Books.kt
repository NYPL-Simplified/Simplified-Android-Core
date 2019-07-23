package org.nypl.simplified.books.api

import org.nypl.audiobook.android.api.PlayerPosition
import org.nypl.drm.core.AdobeAdeptLoan
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.books.api.BookFormat.BookFormatAudioBook
import org.nypl.simplified.books.api.BookFormat.BookFormatEPUB
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import java.io.File
import java.net.URI

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

  val formats: List<BookFormat>) {

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

/**
 * The type of book formats. A book format is an immutable snapshot of the current state
 * of a specific format of a book.
 */

sealed class BookFormat {

  /**
   * @return `true` iff the book data for the format is downloaded
   */

  abstract val isDownloaded: Boolean

  /**
   * An EPUB format.
   */

  data class BookFormatEPUB(

    /**
     * The file containing Adobe DRM rights information. Only present if the book has been
     * fulfilled via the DRM system.
     */

    val adobeRightsFile: File?,

    /**
     * The Adobe DRM rights information. Only present if the book has been fulfilled via the DRM
     * system.
     */

    val adobeRights: AdobeAdeptLoan?,

    /**
     * The EPUB file on disk, if one has been downloaded.
     */

    val file: File?,

    /**
     * The last read location of the book, if any.
     */

    val lastReadLocation: Bookmark?,

    /**
     * The list of bookmarks.
     */

    val bookmarks: List<Bookmark>) : BookFormat() {

    override val isDownloaded: Boolean
      get() = this.file != null
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
   * An audio book format.
   */

  data class BookFormatAudioBook(

    /**
     * The current audio book manifest.
     */

    val manifest: AudioBookManifestReference?,

    /**
     * The most recent playback position.
     */

    val position: PlayerPosition?) : BookFormat() {

    /*
     * Audio books are downloaded if there's a manifest available.
     */

    override val isDownloaded: Boolean
      get() = this.manifest != null
  }

  /**
   * A PDF format.
   */

  data class BookFormatPDF(

    /**
     * The last read location of the PDF book, if any.
     */

    val lastReadLocation: Int?,

    /**
     * The PDF file on disk, if one has been downloaded.
     */

    val file: File?) : BookFormat() {

    override val isDownloaded: Boolean
      get() = this.file != null
  }
}