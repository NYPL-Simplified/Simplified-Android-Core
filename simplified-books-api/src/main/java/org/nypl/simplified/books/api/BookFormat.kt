package org.nypl.simplified.books.api

import one.irradia.mime.api.MIMEType
import org.librarysimplified.audiobook.api.PlayerPosition
import java.io.File
import java.net.URI

/**
 * The type of book formats. A book format is an immutable snapshot of the current state
 * of a specific format of a book.
 */

sealed class BookFormat {

  /**
   * @return The DRM information
   */

  abstract val drmInformation: BookDRMInformation

  /**
   * @return The content type of the book format
   */

  abstract val contentType: MIMEType

  /**
   * @return `true` iff the book data for the format is downloaded
   */

  abstract val isDownloaded: Boolean

  /**
   * An EPUB format.
   */

  data class BookFormatEPUB(
    override val drmInformation: BookDRMInformation,

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

    val bookmarks: List<Bookmark>,

    override val contentType: MIMEType
  ) : BookFormat() {

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

    val manifestFile: File
  )

  /**
   * An audio book format.
   */

  data class BookFormatAudioBook(
    override val drmInformation: BookDRMInformation,

    /**
     * The current audio book manifest.
     */

    val manifest: AudioBookManifestReference?,

    /**
     * The most recent playback position.
     */

    val position: PlayerPosition?,

    override val contentType: MIMEType
  ) : BookFormat() {

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
    override val drmInformation: BookDRMInformation,

    /**
     * The last read location of the PDF book, if any.
     */

    val lastReadLocation: Int?,

    /**
     * The PDF file on disk, if one has been downloaded.
     */

    val file: File?,

    override val contentType: MIMEType
  ) : BookFormat() {
    override val isDownloaded: Boolean
      get() = this.file != null
  }
}
