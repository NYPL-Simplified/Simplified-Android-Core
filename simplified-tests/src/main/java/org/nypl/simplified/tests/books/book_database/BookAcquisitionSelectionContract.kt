package org.nypl.simplified.tests.books.book_database

import com.io7m.jfunctional.OptionType
import com.io7m.jfunctional.Some

import org.junit.Assert
import org.junit.Test
import org.nypl.simplified.books.book_database.api.BookAcquisitionSelection
import org.nypl.simplified.books.book_database.api.BookFormats
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParser
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParserType
import org.nypl.simplified.opds.core.OPDSAcquisitionPath
import org.nypl.simplified.tests.opds.OPDSFeedEntryParserContract
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.FileNotFoundException
import java.io.InputStream
import java.net.URI
import java.net.URL

abstract class BookAcquisitionSelectionContract {

  private val parser: OPDSAcquisitionFeedEntryParserType
    get() = OPDSAcquisitionFeedEntryParser.newParser(BookFormats.supportedBookMimeTypes())

  @Test
  @Throws(Exception::class)
  fun testOpenAccess() {
    val parser = this.parser
    val entry = parser.parseEntryStream(
      URI.create("urn:example"),
      BookAcquisitionSelectionContract.getResource("entry-availability-open-access.xml"))

    val acquisition_opt =
      BookAcquisitionSelection.preferredAcquisition(entry.acquisitionPaths)

    Assert.assertTrue(acquisition_opt.isSome)

    val acquisition = (acquisition_opt as Some<OPDSAcquisitionPath>).get()

    Assert.assertEquals(
      "https://example.com/Open-Access", acquisition.next.uri.toString())
    Assert.assertEquals(
      "application/epub+zip", acquisition.finalContentType().fullType)
  }

  @Test
  @Throws(Exception::class)
  fun testClassics0() {
    val parser = this.parser
    val entry = parser.parseEntryStream(
      URI.create("urn:example"),
      BookAcquisitionSelectionContract.getResource("entry-classics-0.xml"))

    val acquisition_opt =
      BookAcquisitionSelection.preferredAcquisition(entry.acquisitionPaths)

    Assert.assertTrue(acquisition_opt.isSome)

    val acquisition = (acquisition_opt as Some<OPDSAcquisitionPath>).get()

    Assert.assertEquals(
      "https://circulation.librarysimplified.org/CLASSICS/works/313322/fulfill/1",
      acquisition.next.uri.toString())
    Assert.assertEquals(
      "application/epub+zip", acquisition.finalContentType().fullType)
  }

  @Test
  @Throws(Exception::class)
  fun testMultipleFormats0() {
    val parser = this.parser
    val entry = parser.parseEntryStream(
      URI.create("urn:example"),
      BookAcquisitionSelectionContract.getResource("entry-with-formats-0.xml"))

    val acquisition_opt = BookAcquisitionSelection.preferredAcquisition(entry.acquisitionPaths)

    Assert.assertTrue(acquisition_opt.isSome)

    val acquisition = (acquisition_opt as Some<OPDSAcquisitionPath>).get()

    Assert.assertEquals(
      "http://qa.circulation.librarysimplified.org/NYNYPL/works/198679/fulfill/2",
      acquisition.next.uri.toString())
    Assert.assertEquals(
      "application/epub+zip", acquisition.finalContentType().fullType)
  }

  @Test
  @Throws(Exception::class)
  fun testMultipleFormats1() {
    val parser = this.parser
    val entry = parser.parseEntryStream(
      URI.create("urn:example"),
      BookAcquisitionSelectionContract.getResource("entry-with-formats-1.xml"))

    val acquisition_opt = BookAcquisitionSelection.preferredAcquisition(entry.acquisitionPaths)

    Assert.assertTrue(acquisition_opt.isSome)

    val acquisition = (acquisition_opt as Some<OPDSAcquisitionPath>).get()

    Assert.assertEquals(
      "http://qa.circulation.librarysimplified.org/NYNYPL/works/Overdrive%20ID/1ac5cc2a-cdc9-46e0-90a4-2de9ada35237/borrow",
      acquisition.next.uri.toString())
    Assert.assertEquals(
      "application/epub+zip", acquisition.finalContentType().fullType)
  }

  @Test
  @Throws(Exception::class)
  fun testBearerTokenPath() {
    val parser = this.parser
    val entry = parser.parseEntryStream(
      URI.create("urn:example"),
      BookAcquisitionSelectionContract.getResource("entry-with-bearer-token.xml"))

    val acquisition_opt = BookAcquisitionSelection.preferredAcquisition(entry.acquisitionPaths)

    Assert.assertTrue(acquisition_opt.isSome)

    val acquisition = (acquisition_opt as Some<OPDSAcquisitionPath>).get()

    Assert.assertEquals(
      "https://circulation.librarysimplified.org/CLASSICS/works/315343/fulfill/17",
      acquisition.next.uri.toString())
    Assert.assertEquals(
      "application/epub+zip", acquisition.finalContentType().fullType)
  }

  @Test
  @Throws(Exception::class)
  fun testNoSupportedFormat() {
    val parser = this.parser
    val entry = parser.parseEntryStream(
      URI.create("urn:example"),
      BookAcquisitionSelectionContract.getResource("entry-no-supported-format.xml"))

    val acquisition_opt = BookAcquisitionSelection.preferredAcquisition(entry.acquisitionPaths)

    Assert.assertFalse(acquisition_opt.isSome)
  }

  @Test
  @Throws(Exception::class)
  fun testNoSupportedRelation() {
    val parser = this.parser
    val entry = parser.parseEntryStream(
      URI.create("urn:example"),
      BookAcquisitionSelectionContract.getResource("entry-no-supported-relations.xml"))

    val acquisition_opt = BookAcquisitionSelection.preferredAcquisition(entry.acquisitionPaths)

    Assert.assertFalse(acquisition_opt.isSome)
  }

  companion object {

    private val LOG = LoggerFactory.getLogger(BookAcquisitionSelectionContract::class.java)

    @Throws(Exception::class)
    private fun getResource(
      name: String): InputStream {

      val path = "/org/nypl/simplified/tests/opds/$name"
      val url = OPDSFeedEntryParserContract::class.java.getResource(path)
        ?: throw FileNotFoundException(path)
      return url.openStream()
    }
  }
}
