package org.nypl.simplified.tests.books.formats

import org.junit.Assert
import org.junit.Test
import org.nypl.simplified.books.formats.BookFormatAudioSupportParameters
import org.nypl.simplified.books.formats.BookFormatSupport
import org.nypl.simplified.books.formats.BookFormatSupportParameters
import org.nypl.simplified.books.formats.api.StandardFormatNames

abstract class BookFormatSupportContract {

  /**
   * An empty path is trivially unsupported.
   */

  @Test
  fun testEmptyPathUnsupported() {
    val support =
      BookFormatSupport.create(
        BookFormatSupportParameters(
          supportsPDF = false,
          supportsAdobeDRM = false,
          supportsAudioBooks = null
        )
      )

    Assert.assertFalse(support.isSupportedPath(listOf()))
  }

  /**
   * PDF support is correctly handled.
   */

  @Test
  fun testPDFSupportedUnsupported() {
    val supportWith =
      BookFormatSupport.create(
        BookFormatSupportParameters(
          supportsPDF = true,
          supportsAdobeDRM = false,
          supportsAudioBooks = null
        )
      )
    val supportWithout =
      BookFormatSupport.create(
        BookFormatSupportParameters(
          supportsPDF = false,
          supportsAdobeDRM = false,
          supportsAudioBooks = null
        )
      )

    Assert.assertTrue(
      supportWith.isSupportedPath(
        listOf(
          StandardFormatNames.genericPDFFiles
        )
      )
    )
    Assert.assertFalse(
      supportWithout.isSupportedPath(
        listOf(
          StandardFormatNames.genericPDFFiles
        )
      )
    )
  }

  /**
   * Adobe DRM support is correctly handled.
   */

  @Test
  fun testAdobeSupportedUnsupported() {
    val supportWith =
      BookFormatSupport.create(
        BookFormatSupportParameters(
          supportsPDF = false,
          supportsAdobeDRM = true,
          supportsAudioBooks = null
        )
      )
    val supportWithout =
      BookFormatSupport.create(
        BookFormatSupportParameters(
          supportsPDF = false,
          supportsAdobeDRM = false,
          supportsAudioBooks = null
        )
      )

    Assert.assertTrue(
      supportWith.isSupportedPath(
        listOf(
          StandardFormatNames.adobeACSMFiles,
          StandardFormatNames.genericEPUBFiles
        )
      )
    )
    Assert.assertFalse(
      supportWithout.isSupportedPath(
        listOf(
          StandardFormatNames.adobeACSMFiles,
          StandardFormatNames.genericEPUBFiles
        )
      )
    )
  }

  /**
   * Audio book support is correctly handled.
   */

  @Test
  fun testAudioSupportedUnsupported() {
    val supportWith =
      BookFormatSupport.create(
        BookFormatSupportParameters(
          supportsPDF = false,
          supportsAdobeDRM = false,
          supportsAudioBooks = BookFormatAudioSupportParameters(
            supportsFindawayAudioBooks = false,
            supportsOverdriveAudioBooks = false,
            supportsDPLAAudioBooks = false
          )
        )
      )
    val supportWithout =
      BookFormatSupport.create(
        BookFormatSupportParameters(
          supportsPDF = false,
          supportsAdobeDRM = false,
          supportsAudioBooks = null
        )
      )

    Assert.assertTrue(
      supportWith.isSupportedPath(
        listOf(
          StandardFormatNames.genericAudioBooks.first()
        )
      )
    )
    Assert.assertFalse(
      supportWithout.isSupportedPath(
        listOf(
          StandardFormatNames.genericAudioBooks.first()
        )
      )
    )
  }

  /**
   * Some types can't be final types.
   */

  @Test
  fun testFinalTypes() {
    val supportWith =
      BookFormatSupport.create(
        BookFormatSupportParameters(
          supportsPDF = false,
          supportsAdobeDRM = false,
          supportsAudioBooks = null
        )
      )

    Assert.assertTrue(
      supportWith.isSupportedPath(
        listOf(
          StandardFormatNames.opdsAcquisitionFeedEntry,
          StandardFormatNames.genericEPUBFiles
        )
      )
    )
    Assert.assertFalse(
      supportWith.isSupportedPath(
        listOf(
          StandardFormatNames.genericEPUBFiles,
          StandardFormatNames.opdsAcquisitionFeedEntry
        )
      )
    )
  }

  /**
   * Adobe-encrypted PDFs aren't supported.
   */

  @Test
  fun testAdobePDFUnsupported() {
    val supportWith =
      BookFormatSupport.create(
        BookFormatSupportParameters(
          supportsPDF = true,
          supportsAdobeDRM = true,
          supportsAudioBooks = null
        )
      )

    Assert.assertFalse(
      supportWith.isSupportedPath(
        listOf(
          StandardFormatNames.adobeACSMFiles,
          StandardFormatNames.genericPDFFiles
        )
      )
    )
  }
}
