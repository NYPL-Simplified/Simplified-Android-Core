package org.nypl.simplified.books.book_database.api

import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
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

  private fun makeEPUBMimeTypes(): Set<String> {
    val types = HashSet<String>(1)
    types.add("application/epub+zip")
    return Collections.unmodifiableSet(types)
  }

  private fun makeAudioBookMimeTypes(): Set<String> {
    val types = HashSet<String>(2)
    types.add("application/vnd.librarysimplified.findaway.license+json")
    types.add("application/audiobook+json")
    types.add("audio/mpeg")
    return Collections.unmodifiableSet(types)
  }

  private fun makePDFMimeTypes(): Set<String> {
    val types = HashSet<String>(1)
    types.add("application/pdf")
    return Collections.unmodifiableSet(types)
  }

  private fun makeSupportedBookMimeTypes(
    epub: Set<String>,
    audioBook: Set<String>,
    pdf: Set<String>
  ): Set<String> {
    val types = HashSet<String>(epub.size)
    types.addAll(epub)
    types.addAll(audioBook)
    types.addAll(pdf)
    return Collections.unmodifiableSet(types)
  }

  /**
   * @return A set of the supported book format MIME types
   */

  fun supportedBookMimeTypes(): Set<String> {
    return SUPPORTED_BOOK_MIME_TYPES
  }

  /**
   * @return The set of MIME types for EPUBs
   */

  fun epubMimeTypes(): Set<String> {
    return EPUB_MIME_TYPES
  }

  /**
   * @return The set of MIME types for EPUBs
   */

  fun audioBookMimeTypes(): Set<String> {
    return AUDIO_BOOK_MIME_TYPES
  }

  /**
   * @return The set of MIME types for PDFs
   */

  fun pdfMimeTypes(): Set<String> {
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
      override fun supportedContentTypes(): Set<String> {
        return epubMimeTypes()
      }
    },

    /**
     * The audio book format.
     */

    BOOK_FORMAT_AUDIO {
      override fun supportedContentTypes(): Set<String> {
        return audioBookMimeTypes()
      }
    },

    /**
     * The PDF format
     */

    BOOK_FORMAT_PDF {
      override fun supportedContentTypes(): Set<String> {
        return pdfMimeTypes()
      }
    };

    /**
     * The content types supported by this format.
     */

    abstract fun supportedContentTypes(): Set<String>
  }
}
