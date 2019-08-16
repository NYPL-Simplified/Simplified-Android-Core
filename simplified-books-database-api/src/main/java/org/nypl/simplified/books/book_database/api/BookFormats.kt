package org.nypl.simplified.books.book_database.api

import org.nypl.simplified.mime.MIMEParser
import org.nypl.simplified.mime.MIMEType
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import java.util.Collections
import java.util.HashSet

/**
 * Information about the supported book formats.
 */

object BookFormats {

  @JvmStatic
  private val AUDIO_BOOK_MIME_TYPES =
    makeAudioBookMimeTypes()

  @JvmStatic
  private val EPUB_MIME_TYPES =
    makeEPUBMimeTypes()

  @JvmStatic
  private val PDF_MIME_TYPES =
    makePDFMimeTypes()

  @JvmStatic
  private val SUPPORTED_BOOK_MIME_TYPES =
    makeSupportedBookMimeTypes(EPUB_MIME_TYPES, AUDIO_BOOK_MIME_TYPES, PDF_MIME_TYPES)

  @JvmStatic
  private val SUPPORTED_BORROW_MIME_TYPES =
    makeSupportedBorrowMimeTypes(SUPPORTED_BOOK_MIME_TYPES)

  @JvmStatic
  private fun makeSupportedBorrowMimeTypes(bookTypes: Set<MIMEType>): Set<MIMEType> {
    val types = HashSet<MIMEType>(bookTypes.size + 4)
    types.addAll(bookTypes)
    types.add(MIMEParser.parseRaisingException("application/atom+xml"))
    types.add(MIMEParser.parseRaisingException("application/vnd.adobe.adept+xml"))
    types.add(MIMEParser.parseRaisingException("application/vnd.librarysimplified.bearer-token+json"))
    return Collections.unmodifiableSet(types)
  }

  @JvmStatic
  private fun makeEPUBMimeTypes(): Set<MIMEType> {
    val types = HashSet<MIMEType>(1)
    types.add(MIMEParser.parseRaisingException("application/epub+zip"))
    return Collections.unmodifiableSet(types)
  }

  @JvmStatic
  private fun makeAudioBookMimeTypes(): Set<MIMEType> {
    val types = HashSet<MIMEType>(2)
    types.add(MIMEParser.parseRaisingException("application/vnd.librarysimplified.findaway.license+json"))
    types.add(MIMEParser.parseRaisingException("application/audiobook+json"))
    types.add(MIMEParser.parseRaisingException("audio/mpeg"))
    return Collections.unmodifiableSet(types)
  }

  @JvmStatic
  private fun makePDFMimeTypes(): Set<MIMEType> {
    val types = HashSet<MIMEType>(1)
    types.add(MIMEParser.parseRaisingException("application/pdf"))
    return Collections.unmodifiableSet(types)
  }

  @JvmStatic
  private fun makeSupportedBookMimeTypes(
    epub: Set<MIMEType>,
    audioBook: Set<MIMEType>,
    pdf: Set<MIMEType>): Set<MIMEType> {
    val types = HashSet<MIMEType>(epub.size)
    types.addAll(epub)
    types.addAll(audioBook)
    types.addAll(pdf)
    return Collections.unmodifiableSet(types)
  }

  /**
   * @return A set of the supported book format MIME types
   */

  @JvmStatic
  fun supportedBookMimeTypes(): Set<MIMEType> {
    return SUPPORTED_BOOK_MIME_TYPES
  }

  /**
   * @return The set of MIME types for EPUBs
   */

  @JvmStatic
  fun epubMimeTypes(): Set<MIMEType> {
    return EPUB_MIME_TYPES
  }

  /**
   * @return The set of MIME types for EPUBs
   */

  @JvmStatic
  fun audioBookMimeTypes(): Set<MIMEType> {
    return AUDIO_BOOK_MIME_TYPES
  }

  /**
   * @return The set of MIME types for PDFs
   */

  @JvmStatic
  fun pdfMimeTypes(): Set<MIMEType> {
    return PDF_MIME_TYPES
  }

  @JvmStatic
  private val formats = BookFormatDefinition.values()

  /**
   * @return The set of formats that the application knows how to borrow. Note that this is
   * distinct from the set of book formats the application supports: Supported book formats
   * may be saved to disk whilst supported borrow formats include those formats via which
   * the application can traverse to reach a book format that it supports.
   */

  @JvmStatic
  fun supportedBorrowMimeTypes(): Set<MIMEType> {
    return SUPPORTED_BORROW_MIME_TYPES
  }

  /**
   * @return `true` if the given MIME type is a supported borrow type
   */

  @JvmStatic
  fun isSupportedBorrowMimeType(mime: MIMEType): Boolean {
    return supportedBorrowMimeTypes()
      .any { existing -> existing.fullType == mime.fullType }
  }

  /**
   * @return `true` if the given MIME type is a supported final book type
   */

  @JvmStatic
  fun isSupportedBookMimeType(mime: MIMEType): Boolean {
    return supportedBookMimeTypes()
      .any { existing -> existing.fullType == mime.fullType }
  }

  /**
   * @return The probable format of the book in the given OPDS entry
   */

  @JvmStatic
  fun inferFormat(entry: OPDSAcquisitionFeedEntry): BookFormatDefinition? {
    for (path in entry.acquisitionPaths) {
      for (format in this.formats) {
        val formatContentTypes = format.supportedContentTypes()
        if (formatContentTypes.any { existing -> existing.fullType == path.finalContentType().fullType }) {
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
  }


}