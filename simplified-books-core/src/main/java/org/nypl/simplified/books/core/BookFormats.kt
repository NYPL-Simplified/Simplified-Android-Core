package org.nypl.simplified.books.core

import com.io7m.jfunctional.Option
import com.io7m.jfunctional.OptionType
import com.io7m.junreachable.UnreachableCodeException
import org.nypl.simplified.mime.MIMEType
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry

import java.util.Collections
import java.util.HashSet

/**
 * Information about the supported book formats.
 */

class BookFormats private constructor() {

  init {
    throw UnreachableCodeException()
  }

  companion object {

    private val AUDIO_BOOK_MIME_TYPES =
      makeAudioBookMimeTypes()
    private val EPUB_MIME_TYPES =
      makeEPUBMimeTypes()
    private val SUPPORTED_BOOK_MIME_TYPES =
      makeSupportedBookMimeTypes(EPUB_MIME_TYPES, AUDIO_BOOK_MIME_TYPES)
    private val SUPPORTED_BORROW_MIME_TYPES =
      makeSupportedBorrowMimeTypes(SUPPORTED_BOOK_MIME_TYPES)

    private fun makeSupportedBorrowMimeTypes(bookTypes: Set<String>): Set<String> {
      val types = HashSet<String>(bookTypes.size + 4)
      types.addAll(bookTypes)
      types.add("application/atom+xml")
      types.add("application/vnd.adobe.adept+xml")
      types.add("application/vnd.librarysimplified.bearer-token+json")
      return Collections.unmodifiableSet(types)
    }

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

    private fun makeSupportedBookMimeTypes(
      epub: Set<String>,
      audioBook: Set<String>): Set<String> {
      val types = HashSet<String>(epub.size)
      types.addAll(epub)
      types.addAll(audioBook)
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
     * @return The set of formats that the application knows how to borrow. Note that this is
     * distinct from the set of book formats the application supports: Supported book formats
     * may be saved to disk whilst supported borrow formats include those formats via which
     * the application can traverse to reach a book format that it supports.
     */

    fun supportedBorrowMimeTypes(): Set<String> {
      return SUPPORTED_BORROW_MIME_TYPES
    }

    /**
     * @return `true` if the given MIME type is a supported borrow type
     */

    fun isSupportedBorrowMimeType(mime: MIMEType): Boolean {
      return supportedBorrowMimeTypes().contains(mime.fullType)
    }

    /**
     * @return `true` if the given MIME type is a supported final book type
     */

    fun isSupportedBookMimeType(mime: MIMEType): Boolean {
      return supportedBookMimeTypes().contains(mime.fullType)
    }

    private val formats = BookFormatDefinition.values()

    /**
     * @return The probable format of the book in the given OPDS entry
     */

    fun inferFormat(entry: OPDSAcquisitionFeedEntry): OptionType<BookFormatDefinition> {
      for (path in entry.acquisitionPaths) {
        for (format in this.formats) {
          val formatContentTypes = format.supportedContentTypes()
          if (formatContentTypes.contains(path.finalContentType().fullType)) {
            return Option.some(format)
          }
        }
      }

      return Option.none()
    }
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
    };

    /**
     * The content types supported by this format.
     */

    abstract fun supportedContentTypes(): Set<String>
  }


}
