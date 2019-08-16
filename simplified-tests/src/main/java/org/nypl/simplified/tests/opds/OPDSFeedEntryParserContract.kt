package org.nypl.simplified.tests.opds

import com.io7m.jfunctional.Option
import com.io7m.jfunctional.Some
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import org.junit.Assert
import org.junit.Test
import org.nypl.simplified.books.book_database.api.BookFormats
import org.nypl.simplified.mime.MIMEType
import org.nypl.simplified.opds.core.DRMLicensor
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParser
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParserType
import org.nypl.simplified.opds.core.OPDSAcquisitionPath
import org.nypl.simplified.opds.core.OPDSAvailabilityHeld
import org.nypl.simplified.opds.core.OPDSAvailabilityHeldReady
import org.nypl.simplified.opds.core.OPDSAvailabilityHoldable
import org.nypl.simplified.opds.core.OPDSAvailabilityLoanable
import org.nypl.simplified.opds.core.OPDSAvailabilityLoaned
import org.nypl.simplified.opds.core.OPDSAvailabilityOpenAccess
import org.slf4j.LoggerFactory
import java.io.FileNotFoundException
import java.io.InputStream
import java.net.URI

/**
 * Entry parser contract.
 */

abstract class OPDSFeedEntryParserContract {

  private val parser: OPDSAcquisitionFeedEntryParserType
    get() = OPDSAcquisitionFeedEntryParser.newParser(BookFormats.supportedBookMimeTypes())

  @Test
  @Throws(Exception::class)
  fun testEntryAvailabilityLoanable() {
    val parser = this.parser
    val e = parser.parseEntryStream(URI.create("urn:test"),
      getResource(
        "entry-availability-loanable.xml"))

    val availability = e.availability
    val expected = OPDSAvailabilityLoanable.get()
    Assert.assertEquals(expected, availability)

    Assert.assertEquals(0, e.acquisitions.size.toLong())
  }

  @Test
  @Throws(Exception::class)
  fun testEntryAvailabilityLoanedIndefinite() {
    val parser = this.parser
    val e = parser.parseEntryStream(URI.create("urn:test"),
      getResource(
        "entry-availability-loaned-indefinite.xml"))

    val availability = e.availability

    val expected_start_date = Option.some(
      ISODateTimeFormat.dateTimeParser().parseDateTime("2000-01-01T00:00:00Z"))
    val expected_end_date = Option.none<DateTime>()
    val expected_revoke = Option.some(URI("http://example.com/revoke"))
    val expected = OPDSAvailabilityLoaned.get(
      expected_start_date, expected_end_date, expected_revoke)

    Assert.assertEquals(expected, availability)

    val types =
      e.acquisitionPaths.map(OPDSAcquisitionPath::finalContentType)
        .map(MIMEType::fullType)

    Assert.assertTrue(
      "application/epub+zip is available", types.contains("application/epub+zip"))
  }

  @Test
  @Throws(Exception::class)
  fun testEntryAvailabilityLoanedTimed() {
    val parser = this.parser
    val e = parser.parseEntryStream(URI.create("urn:test"),
      getResource(
        "entry-availability-loaned-timed.xml"))

    val availability = e.availability

    val expected_start_date = Option.some(
      ISODateTimeFormat.dateTimeParser().parseDateTime("2000-01-01T00:00:00Z"))
    val expected_end_date = Option.some(
      ISODateTimeFormat.dateTimeParser().parseDateTime("2010-01-01T00:00:00Z"))
    val expected_revoke = Option.some(URI("http://example.com/revoke"))
    val expected = OPDSAvailabilityLoaned.get(
      expected_start_date, expected_end_date, expected_revoke)

    Assert.assertEquals(expected, availability)

    val types =
      e.acquisitionPaths.map(OPDSAcquisitionPath::finalContentType)
        .map(MIMEType::fullType)
        .toSet()
    LOG.debug("types: {}", types)

    Assert.assertTrue(
      "application/epub+zip is available", types.contains("application/epub+zip"))
    Assert.assertTrue(
      "text/html is available", types.contains("text/html"))
  }

  @Test
  @Throws(Exception::class)
  fun testEntryAvailabilityHoldable() {
    val parser = this.parser
    val e = parser.parseEntryStream(URI.create("urn:test"),
      getResource(
        "entry-availability-holdable.xml"))

    val availability = e.availability
    val expected = OPDSAvailabilityHoldable.get()

    Assert.assertEquals(expected, availability)
    Assert.assertEquals(1, e.acquisitions.size.toLong())

    val types =
      e.acquisitionPaths.map(OPDSAcquisitionPath::finalContentType)
        .map(MIMEType::fullType)
        .toSet()
    LOG.debug("types: {}", types)

    Assert.assertEquals(1, types.size.toLong())
    Assert.assertTrue(
      "application/epub+zip is available", types.contains("application/epub+zip"))
  }

  @Test
  @Throws(Exception::class)
  fun testEntryAvailabilityHeldIndefinite() {
    val parser = this.parser
    val e = parser.parseEntryStream(URI.create("urn:test"),
      getResource(
        "entry-availability-held-indefinite.xml"))

    val availability = e.availability

    val expected_start_date = Option.some(
      ISODateTimeFormat.dateTimeParser().parseDateTime("2000-01-01T00:00:00Z"))
    val queue_position = Option.none<Int>()
    val expected_end_date = Option.none<DateTime>()
    val expected_revoke = Option.some(URI("http://example.com/revoke"))
    val expected = OPDSAvailabilityHeld.get(
      expected_start_date, queue_position, expected_end_date, expected_revoke)

    Assert.assertEquals(expected, availability)
    Assert.assertEquals(1, e.acquisitions.size.toLong())

    val types =
      e.acquisitionPaths.map(OPDSAcquisitionPath::finalContentType)
        .map(MIMEType::fullType)
        .toSet()
    LOG.debug("types: {}", types)

    Assert.assertEquals(1, types.size.toLong())
    Assert.assertTrue(
      "application/epub+zip is available", types.contains("application/epub+zip"))
  }

  @Test
  @Throws(Exception::class)
  fun testEntryAvailabilityHeldTimed() {
    val parser = this.parser
    val e = parser.parseEntryStream(URI.create("urn:test"),
      getResource(
        "entry-availability-held-timed.xml"))

    val availability = e.availability

    val expected_start_date = Option.some(
      ISODateTimeFormat.dateTimeParser().parseDateTime("2000-01-01T00:00:00Z"))
    val expected_end_date = Option.some(
      ISODateTimeFormat.dateTimeParser().parseDateTime("2010-01-01T00:00:00Z"))
    val queue_position = Option.none<Int>()
    val expected_revoke = Option.some(URI("http://example.com/revoke"))
    val expected = OPDSAvailabilityHeld.get(
      expected_start_date, queue_position, expected_end_date, expected_revoke)

    Assert.assertEquals(expected, availability)
    Assert.assertEquals(1, e.acquisitions.size.toLong())

    val types =
      e.acquisitionPaths.map(OPDSAcquisitionPath::finalContentType)
        .map(MIMEType::fullType)
        .toSet()
    LOG.debug("types: {}", types)

    Assert.assertEquals(1, types.size.toLong())
    Assert.assertTrue(
      "application/epub+zip is available", types.contains("application/epub+zip"))
  }

  @Test
  @Throws(Exception::class)
  fun testEntryAvailabilityHeldIndefiniteQueued() {
    val parser = this.parser
    val e = parser.parseEntryStream(URI.create("urn:test"),
      getResource(
        "entry-availability-held-indefinite-queued.xml"))

    val availability = e.availability

    val expected_start_date = Option.some(
      ISODateTimeFormat.dateTimeParser().parseDateTime("2000-01-01T00:00:00Z"))
    val queue_position = Option.some(3)
    val expected_end_date = Option.none<DateTime>()
    val expected_revoke = Option.some(URI("http://example.com/revoke"))
    val expected = OPDSAvailabilityHeld.get(
      expected_start_date, queue_position, expected_end_date, expected_revoke)

    Assert.assertEquals(expected, availability)
    Assert.assertEquals(1, e.acquisitions.size.toLong())

    val types =
      e.acquisitionPaths.map(OPDSAcquisitionPath::finalContentType)
        .map(MIMEType::fullType)
        .toSet()
    LOG.debug("types: {}", types)

    Assert.assertEquals(1, types.size.toLong())
    Assert.assertTrue(
      "application/epub+zip is available", types.contains("application/epub+zip"))
  }

  @Test
  @Throws(Exception::class)
  fun testEntryAvailabilityHeldTimedQueued() {
    val parser = this.parser
    val e = parser.parseEntryStream(URI.create("urn:test"),
      getResource(
        "entry-availability-held-timed-queued.xml"))

    val availability = e.availability

    val expected_start_date = Option.some(
      ISODateTimeFormat.dateTimeParser().parseDateTime("2000-01-01T00:00:00Z"))
    val expected_end_date = Option.some(
      ISODateTimeFormat.dateTimeParser().parseDateTime("2010-01-01T00:00:00Z"))
    val queue_position = Option.some(3)
    val expected_revoke = Option.some(URI("http://example.com/revoke"))
    val expected = OPDSAvailabilityHeld.get(
      expected_start_date, queue_position, expected_end_date, expected_revoke)

    Assert.assertEquals(expected, availability)
    Assert.assertEquals(1, e.acquisitions.size.toLong())

    val types =
      e.acquisitionPaths.map(OPDSAcquisitionPath::finalContentType)
        .map(MIMEType::fullType)
        .toSet()
    LOG.debug("types: {}", types)

    Assert.assertEquals(1, types.size.toLong())
    Assert.assertTrue(
      "application/epub+zip is available", types.contains("application/epub+zip"))
  }

  @Test
  @Throws(Exception::class)
  fun testEntryAvailabilityHeldReady() {
    val parser = this.parser
    val e = parser.parseEntryStream(URI.create("urn:test"),
      getResource(
        "entry-availability-heldready.xml"))

    val availability = e.availability

    val expected_end_date = Option.none<DateTime>()
    val expected_revoke = Option.some(URI("http://example.com/revoke"))
    val expected = OPDSAvailabilityHeldReady.get(expected_end_date, expected_revoke)

    Assert.assertEquals(expected, availability)
    Assert.assertEquals(1, e.acquisitions.size.toLong())

    val types =
      e.acquisitionPaths.map(OPDSAcquisitionPath::finalContentType)
        .map(MIMEType::fullType)
        .toSet()
    LOG.debug("types: {}", types)

    Assert.assertEquals(1, types.size.toLong())
    Assert.assertTrue(
      "application/epub+zip is available", types.contains("application/epub+zip"))
  }

  @Test
  @Throws(Exception::class)
  fun testEntryAvailabilityReservedTimed() {
    val parser = this.parser
    val e = parser.parseEntryStream(URI.create("urn:test"),
      getResource(
        "entry-availability-heldready-timed.xml"))

    val availability = e.availability

    val expected_end_date = Option.some(
      ISODateTimeFormat.dateTimeParser().parseDateTime("2010-01-01T00:00:00Z"))
    val expected_revoke = Option.some(URI("http://example.com/revoke"))
    val expected = OPDSAvailabilityHeldReady.get(expected_end_date, expected_revoke)

    Assert.assertEquals(expected, availability)

    val types =
      e.acquisitionPaths.map(OPDSAcquisitionPath::finalContentType)
        .map(MIMEType::fullType)
        .toSet()
    LOG.debug("types: {}", types)

    Assert.assertEquals(1, types.size.toLong())
    Assert.assertTrue(
      "application/epub+zip is available", types.contains("application/epub+zip"))
  }

  @Test
  @Throws(Exception::class)
  fun testEntryAvailabilityOpenAccess() {
    val parser = this.parser
    val e = parser.parseEntryStream(URI.create("urn:test"),
      getResource(
        "entry-availability-open-access.xml"))

    val availability = e.availability

    val expected_revoke = Option.some(URI("http://example.com/revoke"))
    val expected = OPDSAvailabilityOpenAccess.get(expected_revoke)

    Assert.assertEquals(expected, availability)

    val types =
      e.acquisitionPaths.map(OPDSAcquisitionPath::finalContentType)
        .map(MIMEType::fullType)
        .toSet()
    LOG.debug("types: {}", types)

    Assert.assertEquals(1, types.size.toLong())
    Assert.assertTrue(
      "application/epub+zip is available", types.contains("application/epub+zip"))
  }

  @Test
  @Throws(Exception::class)
  fun testEntryAvailabilityReservedSpecific0() {
    val parser = this.parser
    val e = parser.parseEntryStream(URI.create("urn:test"),
      getResource(
        "entry-availability-heldready-specific0.xml"))

    val availability = e.availability

    val expected_end_date = Option.some(
      ISODateTimeFormat.dateTimeParser().parseDateTime("2015-08-24T00:30:24Z"))
    val expected_revoke = Option.some(URI("http://example.com/revoke"))
    val expected = OPDSAvailabilityHeldReady.get(expected_end_date, expected_revoke)

    Assert.assertEquals(expected, availability)

    val types =
      e.acquisitionPaths.map(OPDSAcquisitionPath::finalContentType)
        .map(MIMEType::fullType)
        .toSet()
    LOG.debug("types: {}", types)

    Assert.assertEquals(1, types.size.toLong())
    Assert.assertTrue(
      "application/epub+zip is available", types.contains("application/epub+zip"))
  }

  @Test
  @Throws(Exception::class)
  fun testEntryNoSupportedFormats() {
    val parser = this.parser
    val e = parser.parseEntryStream(URI.create("urn:test"),
      getResource("entry-no-supported-format.xml"))

    val types =
      e.acquisitionPaths.map(OPDSAcquisitionPath::finalContentType)
        .map(MIMEType::fullType)
        .toSet()
    LOG.debug("types: {}", types)

    Assert.assertEquals(2, types.size.toLong())
    Assert.assertTrue(
      "application/not-a-supported-format is available", types.contains("application/not-a-supported-format"))
    Assert.assertTrue(
      "text/html is available", types.contains("text/html"))
  }

  @Test
  @Throws(Exception::class)
  fun testEntryMultipleFormats0() {
    val parser = this.parser
    val e = parser.parseEntryStream(URI.create("urn:test"),
      getResource("entry-with-formats-0.xml"))

    Assert.assertEquals(3, e.acquisitions.size.toLong())

    val types =
      e.acquisitionPaths.map(OPDSAcquisitionPath::finalContentType)
        .map(MIMEType::fullType)
        .toSet()
    LOG.debug("types: {}", types)

    Assert.assertEquals(3, types.size.toLong())
    Assert.assertTrue(
      "application/epub+zip is available", types.contains("application/epub+zip"))
    Assert.assertTrue(
      "text/html is available", types.contains("text/html"))
    Assert.assertTrue(
      "application/pdf is available", types.contains("application/pdf"))
  }

  @Test
  @Throws(Exception::class)
  fun testEntryMultipleFormats1() {
    val parser = this.parser
    val e = parser.parseEntryStream(URI.create("urn:test"),
      getResource("entry-with-formats-1.xml"))

    Assert.assertEquals(1, e.acquisitions.size.toLong())

    val types =
      e.acquisitionPaths.map(OPDSAcquisitionPath::finalContentType)
        .map(MIMEType::fullType)
        .toSet()
    LOG.debug("types: {}", types)

    Assert.assertEquals(3, types.size.toLong())
    Assert.assertTrue(
      "application/epub+zip is available", types.contains("application/epub+zip"))
    Assert.assertTrue(
      "application/pdf is available", types.contains("application/pdf"))
    Assert.assertTrue(
      "text/html is available", types.contains("text/html"))
  }

  @Test
  @Throws(Exception::class)
  fun testEntryWithDRM() {
    val parser = this.parser
    val e = parser.parseEntryStream(URI.create("urn:test"),
      getResource("entry-with-drm.xml"))

    val licensor_opt = e.licensor
    Assert.assertTrue(licensor_opt.isSome)

    val licensor = (licensor_opt as Some<DRMLicensor>).get()
    Assert.assertEquals(
      "NYPL",
      licensor.vendor)
    Assert.assertEquals(
      "NYNYPL|0000000000|XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX",
      licensor.clientToken)
    Assert.assertEquals(
      Option.some("http://qa.circulation.librarysimplified.org/NYNYPL/AdobeAuth/devices"),
      licensor.deviceManager)
  }

  @Test
  @Throws(Exception::class)
  fun testEntry20190315MobyDick() {
    val parser = this.parser
    val e = parser.parseEntryStream(
      URI.create("urn:example"),
      getResource("2019-03-15-test-case-moby-dick.xml"))

    val types =
      e.acquisitionPaths.map(OPDSAcquisitionPath::finalContentType)
        .map(MIMEType::fullType)
        .toSet()
    LOG.debug("types: {}", types)

    Assert.assertEquals(1, types.size.toLong())
    Assert.assertTrue(
      "application/epub+zip is available", types.contains("application/epub+zip"))
  }

  @Test
  @Throws(Exception::class)
  fun testEntry20190315TimeMachine() {
    val parser = this.parser
    val e = parser.parseEntryStream(
      URI.create("urn:example"),
      getResource("2019-03-15-test-case-time_machine.xml"))

    val types =
      e.acquisitionPaths.map(OPDSAcquisitionPath::finalContentType)
        .map(MIMEType::fullType)
        .toSet()
    LOG.debug("types: {}", types)

    Assert.assertEquals(1, types.size.toLong())
    Assert.assertTrue(
      "application/epub+zip is available", types.contains("application/epub+zip"))
  }

  companion object {

    private val LOG = LoggerFactory.getLogger(OPDSFeedEntryParserContract::class.java)

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
