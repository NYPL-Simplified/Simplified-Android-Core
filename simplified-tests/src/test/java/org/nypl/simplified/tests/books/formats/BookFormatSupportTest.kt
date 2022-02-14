package org.nypl.simplified.tests.books.formats

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.nypl.simplified.books.api.BookDRMKind
import org.nypl.simplified.books.formats.BookFormatAudioSupportParameters
import org.nypl.simplified.books.formats.BookFormatSupport
import org.nypl.simplified.books.formats.BookFormatSupportParameters
import org.nypl.simplified.books.formats.api.StandardFormatNames

class BookFormatSupportTest {

  /**
   * An empty path is trivially unsupported.
   */

  @Test
  fun testEmptyPathUnsupported() {
    val support =
      BookFormatSupport.create(
        BookFormatSupportParameters(
          supportsPDF = false,
          supportsLCP = false,
          supportsAdobeDRM = false,
          supportsAxisNow = false,
          supportsAudioBooks = null
        )
      )

    Assertions.assertFalse(support.isSupportedPath(listOf()))
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
          supportsLCP = false,
          supportsAdobeDRM = false,
          supportsAxisNow = false,
          supportsAudioBooks = null
        )
      )
    val supportWithout =
      BookFormatSupport.create(
        BookFormatSupportParameters(
          supportsPDF = false,
          supportsLCP = false,
          supportsAdobeDRM = false,
          supportsAxisNow = false,
          supportsAudioBooks = null
        )
      )

    Assertions.assertTrue(
      supportWith.isSupportedPath(
        listOf(
          StandardFormatNames.genericPDFFiles
        )
      )
    )
    Assertions.assertFalse(
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
          supportsLCP = false,
          supportsAdobeDRM = true,
          supportsAxisNow = false,
          supportsAudioBooks = null
        )
      )
    val supportWithout =
      BookFormatSupport.create(
        BookFormatSupportParameters(
          supportsPDF = false,
          supportsLCP = false,
          supportsAdobeDRM = false,
          supportsAxisNow = false,
          supportsAudioBooks = null
        )
      )

    Assertions.assertTrue(
      supportWith.isSupportedPath(
        listOf(
          StandardFormatNames.adobeACSMFiles,
          StandardFormatNames.genericEPUBFiles
        )
      )
    )
    Assertions.assertFalse(
      supportWithout.isSupportedPath(
        listOf(
          StandardFormatNames.adobeACSMFiles,
          StandardFormatNames.genericEPUBFiles
        )
      )
    )
  }

  /**
   * Acquisition paths through ACS are associated with BookDRMKind.ACS.
   */

  @Test
  fun testDRMKindAdobe() {
    val support =
      BookFormatSupport.create(
        BookFormatSupportParameters(
          supportsPDF = false,
          supportsLCP = false,
          supportsAdobeDRM = true,
          supportsAxisNow = false,
          supportsAudioBooks = null
        )
      )
    Assertions.assertEquals(
      BookDRMKind.ACS,
      support.getDRMKind(
        listOf(
          StandardFormatNames.adobeACSMFiles,
          StandardFormatNames.genericEPUBFiles
        )
      )
    )
  }

  /**
   * Acquisition paths through AxisNow are associated with BookDRMKind.AXIS.
   */

  @Test
  fun testDRMKindAxis() {
    val support =
      BookFormatSupport.create(
        BookFormatSupportParameters(
          supportsPDF = false,
          supportsLCP = false,
          supportsAdobeDRM = false,
          supportsAxisNow = true,
          supportsAudioBooks = null
        )
      )
    Assertions.assertEquals(
      BookDRMKind.AXIS,
      support.getDRMKind(
        listOf(
          StandardFormatNames.axisNow
        )
      )
    )
  }

  @Test
  fun testDRMKindNone() {
    val support =
      BookFormatSupport.create(
        BookFormatSupportParameters(
          supportsPDF = false,
          supportsLCP = false,
          supportsAdobeDRM = false,
          supportsAxisNow = false,
          supportsAudioBooks = null
        )
      )
    Assertions.assertEquals(
      BookDRMKind.NONE,
      support.getDRMKind(
        listOf(
          StandardFormatNames.genericEPUBFiles
        )
      )
    )
  }

  /**
   * AxisNow DRM support is correctly handled.
   */

  @Test
  fun testAxisNowSupportedUnsupported() {
    val supportWith =
      BookFormatSupport.create(
        BookFormatSupportParameters(
          supportsPDF = false,
          supportsLCP = false,
          supportsAdobeDRM = false,
          supportsAxisNow = true,
          supportsAudioBooks = null
        )
      )
    val supportWithout =
      BookFormatSupport.create(
        BookFormatSupportParameters(
          supportsPDF = false,
          supportsLCP = false,
          supportsAdobeDRM = false,
          supportsAxisNow = false,
          supportsAudioBooks = null
        )
      )

    Assertions.assertTrue(
      supportWith.isSupportedPath(
        listOf(
          StandardFormatNames.axisNow
        )
      )
    )
    Assertions.assertFalse(
      supportWithout.isSupportedPath(
        listOf(
          StandardFormatNames.axisNow
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
          supportsLCP = false,
          supportsAxisNow = false,
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
          supportsAxisNow = false,
          supportsLCP = false,
          supportsAudioBooks = null
        )
      )

    Assertions.assertTrue(
      supportWith.isSupportedPath(
        listOf(
          StandardFormatNames.genericAudioBooks.first()
        )
      )
    )
    Assertions.assertFalse(
      supportWithout.isSupportedPath(
        listOf(
          StandardFormatNames.genericAudioBooks.first()
        )
      )
    )
  }

  /**
   * DPLA audio book support is correctly handled.
   */

  @Test
  fun testAudioSupportedDPLA() {
    val supportWith =
      BookFormatSupport.create(
        BookFormatSupportParameters(
          supportsPDF = false,
          supportsAdobeDRM = false,
          supportsAxisNow = false,
          supportsLCP = false,
          supportsAudioBooks = BookFormatAudioSupportParameters(
            supportsFindawayAudioBooks = false,
            supportsOverdriveAudioBooks = false,
            supportsDPLAAudioBooks = true
          )
        )
      )
    val supportWithout =
      BookFormatSupport.create(
        BookFormatSupportParameters(
          supportsPDF = false,
          supportsAdobeDRM = false,
          supportsLCP = false,
          supportsAxisNow = false,
          supportsAudioBooks = null
        )
      )

    Assertions.assertTrue(
      supportWith.isSupportedPath(
        listOf(
          StandardFormatNames.dplaAudioBooks
        )
      )
    )
    Assertions.assertFalse(
      supportWithout.isSupportedPath(
        listOf(
          StandardFormatNames.dplaAudioBooks
        )
      )
    )
  }

  /**
   * Overdrive audio book support is correctly handled.
   */

  @Test
  fun testAudioSupportedOverdrive() {
    val supportWith =
      BookFormatSupport.create(
        BookFormatSupportParameters(
          supportsPDF = false,
          supportsAdobeDRM = false,
          supportsAxisNow = false,
          supportsLCP = false,
          supportsAudioBooks = BookFormatAudioSupportParameters(
            supportsFindawayAudioBooks = false,
            supportsOverdriveAudioBooks = true,
            supportsDPLAAudioBooks = false
          )
        )
      )
    val supportWithout =
      BookFormatSupport.create(
        BookFormatSupportParameters(
          supportsPDF = false,
          supportsAdobeDRM = false,
          supportsAxisNow = false,
          supportsLCP = false,
          supportsAudioBooks = null
        )
      )

    Assertions.assertTrue(
      supportWith.isSupportedPath(
        listOf(
          StandardFormatNames.overdriveAudioBooks
        )
      )
    )
    Assertions.assertFalse(
      supportWithout.isSupportedPath(
        listOf(
          StandardFormatNames.overdriveAudioBooks
        )
      )
    )
  }

  /**
   * Findaway audio book support is correctly handled.
   */

  @Test
  fun testAudioSupportedFindaway() {
    val supportWith =
      BookFormatSupport.create(
        BookFormatSupportParameters(
          supportsPDF = false,
          supportsAdobeDRM = false,
          supportsAxisNow = false,
          supportsLCP = false,
          supportsAudioBooks = BookFormatAudioSupportParameters(
            supportsFindawayAudioBooks = true,
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
          supportsAxisNow = false,
          supportsLCP = false,
          supportsAudioBooks = null
        )
      )

    Assertions.assertTrue(
      supportWith.isSupportedPath(
        listOf(
          StandardFormatNames.findawayAudioBooks
        )
      )
    )
    Assertions.assertFalse(
      supportWithout.isSupportedPath(
        listOf(
          StandardFormatNames.findawayAudioBooks
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
          supportsAxisNow = false,
          supportsLCP = false,
          supportsAudioBooks = null
        )
      )

    Assertions.assertTrue(
      supportWith.isSupportedPath(
        listOf(
          StandardFormatNames.opdsAcquisitionFeedEntry,
          StandardFormatNames.genericEPUBFiles
        )
      )
    )
    Assertions.assertFalse(
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
          supportsAxisNow = false,
          supportsLCP = false,
          supportsAudioBooks = null
        )
      )

    Assertions.assertFalse(
      supportWith.isSupportedPath(
        listOf(
          StandardFormatNames.adobeACSMFiles,
          StandardFormatNames.genericPDFFiles
        )
      )
    )
  }
}
