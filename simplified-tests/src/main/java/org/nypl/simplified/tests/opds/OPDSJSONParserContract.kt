package org.nypl.simplified.tests.opds

import org.junit.Assert
import org.junit.Test
import org.nypl.simplified.books.core.BookFormats
import org.nypl.simplified.mime.MIMEType
import org.nypl.simplified.opds.core.OPDSAcquisition
import org.nypl.simplified.opds.core.OPDSAcquisitionFeed
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParser
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParserType
import org.nypl.simplified.opds.core.OPDSAcquisitionPath
import org.nypl.simplified.opds.core.OPDSFeedParser
import org.nypl.simplified.opds.core.OPDSFeedParserType
import org.nypl.simplified.opds.core.OPDSJSONParser
import org.nypl.simplified.opds.core.OPDSJSONParserType
import org.nypl.simplified.opds.core.OPDSJSONSerializer
import org.nypl.simplified.opds.core.OPDSJSONSerializerType

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.InputStream
import java.net.URI
import java.net.URL

abstract class OPDSJSONParserContract {

  @Throws(Exception::class)
  private fun getResource(
    name: String): InputStream {

    val path = "/org/nypl/simplified/tests/opds/$name"
    val url = OPDSFeedEntryParserContract::class.java.getResource(path)
      ?: throw FileNotFoundException(path)
    return url.openStream()
  }

  @Test
  @Throws(Exception::class)
  fun testCompatibility20180921_1() {

    val json_parser = OPDSJSONParser.newParser()

    val e0 = json_parser.parseAcquisitionFeedEntryFromStream(
      getResource("compatibility-20180921-test-old.json"))

    val e1 = json_parser.parseAcquisitionFeedEntryFromStream(
      getResource("compatibility-20180921-test-new-1.json"))

    run {
      Assert.assertEquals(
        e0.acquisitionPaths.map(OPDSAcquisitionPath::finalContentType).map(MIMEType::fullType),
        e1.acquisitionPaths.map(OPDSAcquisitionPath::finalContentType).map(MIMEType::fullType))

      Assert.assertEquals(e0.acquisitions, e1.acquisitions)
      Assert.assertEquals(e0.availability, e1.availability)
      Assert.assertEquals(e0.authors, e1.authors)
      Assert.assertEquals(e0.categories, e1.categories)
      Assert.assertEquals(e0.cover, e1.cover)
      Assert.assertEquals(e0.groups, e1.groups)
      Assert.assertEquals(e0.id, e1.id)
      Assert.assertEquals(e0.published, e1.published)
      Assert.assertEquals(e0.publisher, e1.publisher)
      Assert.assertEquals(e0.summary, e1.summary)
      Assert.assertEquals(e0.thumbnail, e1.thumbnail)
      Assert.assertEquals(e0.title, e1.title)
    }
  }

  @Test
  @Throws(Exception::class)
  fun testCompatibility20180921_2() {

    val json_parser = OPDSJSONParser.newParser()

    val e0 = json_parser.parseAcquisitionFeedEntryFromStream(
      getResource("compatibility-20180921-test-old.json"))

    val e1 = json_parser.parseAcquisitionFeedEntryFromStream(
      getResource("compatibility-20180921-test-new-0.json"))

    run {
      Assert.assertEquals(
        e0.acquisitionPaths.map(OPDSAcquisitionPath::finalContentType).map(MIMEType::fullType),
        e1.acquisitionPaths.map(OPDSAcquisitionPath::finalContentType).map(MIMEType::fullType))

      Assert.assertEquals(e0.availability, e1.availability)
      Assert.assertEquals(e0.authors, e1.authors)
      Assert.assertEquals(e0.categories, e1.categories)
      Assert.assertEquals(e0.cover, e1.cover)
      Assert.assertEquals(e0.groups, e1.groups)
      Assert.assertEquals(e0.id, e1.id)
      Assert.assertEquals(e0.published, e1.published)
      Assert.assertEquals(e0.publisher, e1.publisher)
      Assert.assertEquals(e0.summary, e1.summary)
      Assert.assertEquals(e0.thumbnail, e1.thumbnail)
      Assert.assertEquals(e0.title, e1.title)
    }
  }
}
