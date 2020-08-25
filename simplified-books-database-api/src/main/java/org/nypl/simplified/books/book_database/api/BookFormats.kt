package org.nypl.simplified.books.book_database.api

import one.irradia.mime.api.MIMEType
import one.irradia.mime.vanilla.MIMEParser
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry

/**
 * Information about the supported book formats.
 */

object BookFormats {

  private fun mimeOf(name: String): MIMEType =
    MIMEParser.parseRaisingException(name)

  private fun mimesOfList(names: List<String>): Set<MIMEType> =
    names.map(this::mimeOf).toSet()

  private fun mimesOf(vararg names: String): Set<MIMEType> =
    mimesOfList(names.toList())

  private fun <T> unionOf(
    vararg sets: Set<T>
  ): Set<T> {
    val union = mutableSetOf<T>()
    sets.forEach { union.addAll(it) }
    return union.toSet()
  }

  private val FINDAWAY_AUDIO_BOOKS =
    mimesOf("application/vnd.librarysimplified.findaway.license+json")

  private val GENERIC_AUDIO_BOOKS =
    mimesOf(
      "application/audiobook+json",
      "audio/mpeg"
    )

  private val OVERDRIVE_AUDIO_BOOKS =
    mimesOf(
      "application/vnd.overdrive.circulation.api+json;profile=audiobook"
    )

  private val AUDIO_BOOK_MIME_TYPES =
    unionOf(
      FINDAWAY_AUDIO_BOOKS,
      GENERIC_AUDIO_BOOKS,
      OVERDRIVE_AUDIO_BOOKS
    )

  private val EPUB_MIME_TYPES =
    mimesOf(
      "application/epub+zip"
    )

  private val PDF_MIME_TYPES =
    mimesOf(
      "application/pdf"
    )

  private val SUPPORTED_BOOK_MIME_TYPES =
    unionOf(
      EPUB_MIME_TYPES,
      AUDIO_BOOK_MIME_TYPES,
      PDF_MIME_TYPES
    )

  /**
   * @return A set of the MIME types that identify generic audio books
   */

  fun audioBookGenericMimeTypes(): Set<MIMEType> =
    GENERIC_AUDIO_BOOKS

  /**
   * @return A set of the MIME types that identify Overdrive audio books
   */

  fun audioBookOverdriveMimeTypes(): Set<MIMEType> =
    OVERDRIVE_AUDIO_BOOKS

  /**
   * @return A set of the MIME types that identify Findaway audio books
   */

  fun audioBookFindawayMimeTypes(): Set<MIMEType> =
    FINDAWAY_AUDIO_BOOKS

  /**
   * @return A set of the supported book format MIME types
   */

  fun supportedBookMimeTypes(): Set<MIMEType> =
    SUPPORTED_BOOK_MIME_TYPES

  /**
   * @return The set of MIME types for EPUBs
   */

  fun epubMimeTypes(): Set<MIMEType> =
    EPUB_MIME_TYPES

  /**
   * @return The set of MIME types for EPUBs
   */

  fun audioBookMimeTypes(): Set<MIMEType> =
    AUDIO_BOOK_MIME_TYPES

  /**
   * @return The set of MIME types for PDFs
   */

  fun pdfMimeTypes(): Set<MIMEType> =
    PDF_MIME_TYPES

  private val formats = BookFormatDefinition.values()

  /**
   * @return The probable format of the book in the given OPDS entry
   */

  fun inferFormat(entry: OPDSAcquisitionFeedEntry): BookFormatDefinition? {
    for (acquisition in entry.acquisitions) {
      for (format in formats) {
        val available = acquisition.availableFinalContentTypes()
        if (available.any { format.supports(it) }) {
          return format
        }
      }
    }
    return null
  }

  /**
   * The type of available book formats.
   */

  enum class BookFormatDefinition {

    /**
     * The EPUB format.
     */

    BOOK_FORMAT_EPUB {
      override val shortName: String = "epub"

      override fun supportedContentTypes(): Set<MIMEType> {
        return epubMimeTypes()
      }
    },

    /**
     * The audio book format.
     */

    BOOK_FORMAT_AUDIO {
      override val shortName: String = "audiobook"

      override fun supportedContentTypes(): Set<MIMEType> {
        return audioBookMimeTypes()
      }
    },

    /**
     * The PDF format
     */

    BOOK_FORMAT_PDF {
      override val shortName: String = "pdf"

      override fun supportedContentTypes(): Set<MIMEType> {
        return pdfMimeTypes()
      }
    };

    /**
     * The short name of the format
     */

    abstract val shortName: String

    /**
     * The content types supported by this format.
     */

    abstract fun supportedContentTypes(): Set<MIMEType>

    /**
     * @return `true` if the format handle supports content of the given content type
     */

    fun supports(
      contentType: MIMEType
    ): Boolean {
      return this.supportedContentTypes().any { supportedType ->
        contentType.fullType == supportedType.fullType
      }
    }
  }
}
