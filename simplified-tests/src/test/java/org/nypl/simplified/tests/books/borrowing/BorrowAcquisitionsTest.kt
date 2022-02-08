package org.nypl.simplified.tests.books.borrowing

import org.junit.Test
import org.junit.jupiter.api.Assertions
import org.nypl.simplified.books.borrowing.BorrowAcquisitions
import org.nypl.simplified.books.formats.BookFormatSupport
import org.nypl.simplified.books.formats.BookFormatSupportParameters
import org.nypl.simplified.books.formats.api.StandardFormatNames
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParser
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParserType
import java.io.FileNotFoundException
import java.io.InputStream
import java.net.URI

class BorrowAcquisitionsTest {

  private val parser: OPDSAcquisitionFeedEntryParserType =
    OPDSAcquisitionFeedEntryParser.newParser()

  private fun getResource(name: String): InputStream {
    val path = "/org/nypl/simplified/tests/books/$name"
    val url = BorrowAcquisitionsTest::class.java.getResource(path)
      ?: throw FileNotFoundException(path)
    return url.openStream()
  }


  @Test
  fun adobeIsPreferredOverAxis() {
    val support =
      BookFormatSupport.create(
        BookFormatSupportParameters(
          supportsLCP = true,
          supportsAudioBooks = null,
          supportsAxisNow = true,
          supportsPDF = false,
          supportsAdobeDRM = true
        )
      )

    val resourceStream = getResource("borrow-multiple-drm.xml")
    val entry = parser.parseEntryStream(URI.create("urn:test"), resourceStream)
    val bestPath = BorrowAcquisitions.pickBestAcquisitionPath(support, entry)
      ?.asMIMETypes().orEmpty()

    Assertions.assertTrue(bestPath.contains(StandardFormatNames.adobeACSMFiles))
  }
}
