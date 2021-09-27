package org.nypl.simplified.tests.opds;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.nypl.simplified.opds.core.OPDSAcquisition;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.nypl.simplified.opds.core.OPDSJSONParser;
import org.nypl.simplified.opds.core.OPDSJSONParserType;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;

public final class OPDSJSONParserTest {

  private static InputStream getResource(
    final String name)
    throws Exception {

    final String path = "/org/nypl/simplified/tests/opds/" + name;
    final URL url = OPDSFeedEntryParserTest.class.getResource(path);
    if (url == null) {
      throw new FileNotFoundException(path);
    }
    return url.openStream();
  }

  @Test
  public void testCompatibility20180921_1()
    throws Exception {

    final OPDSJSONParserType json_parser =
      OPDSJSONParser.newParser();

    final OPDSAcquisitionFeedEntry e0 =
      json_parser.parseAcquisitionFeedEntryFromStream(
        OPDSJSONParserTest.getResource("compatibility-20180921-test-old.json"));

    final OPDSAcquisitionFeedEntry e1 =
      json_parser.parseAcquisitionFeedEntryFromStream(
        OPDSJSONParserTest.getResource("compatibility-20180921-test-new-1.json"));

    {
      Assertions.assertEquals(e0.getAcquisitions(), e1.getAcquisitions());
      Assertions.assertEquals(e0.getAvailability(), e1.getAvailability());
      Assertions.assertEquals(e0.getAuthors(), e1.getAuthors());
      Assertions.assertEquals(e0.getCategories(), e1.getCategories());
      Assertions.assertEquals(e0.getCover(), e1.getCover());
      Assertions.assertEquals(e0.getGroups(), e1.getGroups());
      Assertions.assertEquals(e0.getID(), e1.getID());
      Assertions.assertEquals(e0.getPublished(), e1.getPublished());
      Assertions.assertEquals(e0.getPublisher(), e1.getPublisher());
      Assertions.assertEquals(e0.getSummary(), e1.getSummary());
      Assertions.assertEquals(e0.getThumbnail(), e1.getThumbnail());
      Assertions.assertEquals(e0.getTitle(), e1.getTitle());
    }
  }

  @Test
  public void testCompatibility20180921_2()
    throws Exception {

    final OPDSJSONParserType json_parser =
      OPDSJSONParser.newParser();

    final OPDSAcquisitionFeedEntry e0 =
      json_parser.parseAcquisitionFeedEntryFromStream(
        OPDSJSONParserTest.getResource("compatibility-20180921-test-old.json"));

    final OPDSAcquisitionFeedEntry e1 =
      json_parser.parseAcquisitionFeedEntryFromStream(
        OPDSJSONParserTest.getResource("compatibility-20180921-test-new-0.json"));

    {
      final OPDSAcquisition e0a = e0.getAcquisitions().get(0);
      final OPDSAcquisition e1a = e1.getAcquisitions().get(0);

      Assertions.assertEquals(e0a.availableFinalContentTypes(), e1a.availableFinalContentTypes());
      Assertions.assertEquals(e0.getAvailability(), e1.getAvailability());
      Assertions.assertEquals(e0.getAuthors(), e1.getAuthors());
      Assertions.assertEquals(e0.getCategories(), e1.getCategories());
      Assertions.assertEquals(e0.getCover(), e1.getCover());
      Assertions.assertEquals(e0.getGroups(), e1.getGroups());
      Assertions.assertEquals(e0.getID(), e1.getID());
      Assertions.assertEquals(e0.getPublished(), e1.getPublished());
      Assertions.assertEquals(e0.getPublisher(), e1.getPublisher());
      Assertions.assertEquals(e0.getSummary(), e1.getSummary());
      Assertions.assertEquals(e0.getThumbnail(), e1.getThumbnail());
      Assertions.assertEquals(e0.getTitle(), e1.getTitle());
    }
  }
}
