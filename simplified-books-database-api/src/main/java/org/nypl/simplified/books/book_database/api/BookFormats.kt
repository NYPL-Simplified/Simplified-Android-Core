package org.nypl.simplified.books.book_database.api

import one.irradia.mime.api.MIMEType
import one.irradia.mime.vanilla.MIMEParser
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import java.lang.IllegalStateException
import java.util.Collections
import java.util.HashSet

/**
 * Information about the supported book formats.
 */

object BookFormats {

  private val AUDIO_BOOK_MIME_TYPES =
    makeAudioBookMimeTypes()
  private val EPUB_MIME_TYPES =
    makeEPUBMimeTypes()
  private val PDF_MIME_TYPES =
    makePDFMimeTypes()
  private val SUPPORTED_BOOK_MIME_TYPES =
    makeSupportedBookMimeTypes(EPUB_MIME_TYPES, AUDIO_BOOK_MIME_TYPES, PDF_MIME_TYPES)

  private fun makeEPUBMimeTypes(): Set<MIMEType> {
    val types = HashSet<MIMEType>(1)
    types.add(MIMEParser.parseRaisingException("application/epub+zip"))
    return Collections.unmodifiableSet(types)
  }

  private fun makeAudioBookMimeTypes(): Set<MIMEType> {
    try {
      val types = HashSet<MIMEType>(2)
      types.add(
        MIMEParser.parseRaisingException(
          "application/vnd.librarysimplified.findaway.license+json"))
      types.add(
        MIMEParser.parseRaisingException("application/audiobook+json"))
      types.add(
        MIMEParser.parseRaisingException("audio/mpeg"))
      return Collections.unmodifiableSet(types)
    } catch (e: Exception) {
      throw IllegalStateException(e)
    }
  }

  private fun makePDFMimeTypes(): Set<MIMEType> {
    val types = HashSet<MIMEType>(1)
    types.add(MIMEParser.parseRaisingException("application/pdf"))
    return Collections.unmodifiableSet(types)
  }

  private fun makeSupportedBookMimeTypes(
    epub: Set<MIMEType>,
    audioBook: Set<MIMEType>,
    pdf: Set<MIMEType>
  ): Set<MIMEType> {
    val types = HashSet<MIMEType>(epub.size)
    types.addAll(epub)
    types.addAll(audioBook)
    types.addAll(pdf)
    return Collections.unmodifiableSet(types)
  }

  /**
   * @return A set of the supported book format MIME types
   */

  fun supportedBookMimeTypes(): Set<MIMEType> {
    return SUPPORTED_BOOK_MIME_TYPES
  }

  /**
   * @return The set of MIME types for EPUBs
   */

  fun epubMimeTypes(): Set<MIMEType> {
    return EPUB_MIME_TYPES
  }

  /**
   * @return The set of MIME types for EPUBs
   */

  fun audioBookMimeTypes(): Set<MIMEType> {
    return AUDIO_BOOK_MIME_TYPES
  }

  /**
   * @return The set of MIME types for PDFs
   */

  fun pdfMimeTypes(): Set<MIMEType> {
    return PDF_MIME_TYPES
  }

  private val formats = BookFormatDefinition.values()

  /**
   * @return The probable format of the book in the given OPDS entry
   */

  fun inferFormat(entry: OPDSAcquisitionFeedEntry): BookFormatDefinition? {
    for (acquisition in entry.acquisitions) {
      for (format in formats) {
        val formatContentTypes = format.supportedContentTypes()
        val bookAvailable = acquisition.availableFinalContentTypes()
        if (formatContentTypes.intersect(bookAvailable).isNotEmpty()) {
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
      override fun supportedContentTypes(): Set<MIMEType> {
        return epubMimeTypes()
      }
    },

    /**
     * The audio book format.
     */

    BOOK_FORMAT_AUDIO {
      override fun supportedContentTypes(): Set<MIMEType> {
        return audioBookMimeTypes()
      }
    },

    /**
     * The PDF format
     */

    BOOK_FORMAT_PDF {
      override fun supportedContentTypes(): Set<MIMEType> {
        return pdfMimeTypes()
      }
    };

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
