package org.nypl.simplified.tests.books.book_database

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.nypl.simplified.books.book_database.api.BookAcquisitionSelection
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParser
import org.nypl.simplified.opds.core.OPDSFeedParser
import org.nypl.simplified.opds.core.OPDSGroup
import org.nypl.simplified.tests.opds.OPDSFeedEntryParserTest
import java.io.FileNotFoundException
import java.io.InputStream
import java.net.URI

class BookAcquisitionSelectionTest {

  @Throws(Exception::class)
  private fun getResource(
    name: String
  ): InputStream? {
    val path = "/org/nypl/simplified/tests/opds/$name"
    val url =
      OPDSFeedEntryParserTest::class.java.getResource(path) ?: throw FileNotFoundException(path)
    return url.openStream()
  }

  @Test
  fun testExpectedFromFeed() {
    val uri = URI.create("http://www.example.com/")
    val parser = OPDSFeedParser.newParser(OPDSAcquisitionFeedEntryParser.newParser())

    val feed =
      this.getResource("dpla-test-feed.xml").use { stream ->
        parser.parse(uri, stream)
      }

    val groupEntry: Map.Entry<String, OPDSGroup> = feed.feedGroups.entries.iterator().next()
    val group = groupEntry.value
    val entry = group.groupEntries[0]

    val acquisition = BookAcquisitionSelection.preferredAcquisition(entry.acquisitions)
    Assertions.assertNotNull(acquisition)
  }
}
