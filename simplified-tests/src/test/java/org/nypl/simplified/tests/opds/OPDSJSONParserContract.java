package org.nypl.simplified.tests.opds;

import org.junit.Assert;
import org.junit.Test;
import org.nypl.simplified.opds.core.OPDSAcquisition;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.nypl.simplified.opds.core.OPDSJSONParser;
import org.nypl.simplified.opds.core.OPDSJSONParserType;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;

public abstract class OPDSJSONParserContract {

  private static InputStream getResource(
    final String name)
    throws Exception {

    final String path = "/org/nypl/simplified/tests/opds/" + name;
    final URL url = OPDSFeedEntryParserContract.class.getResource(path);
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
        OPDSJSONParserContract.getResource("compatibility-20180921-test-old.json"));

    final OPDSAcquisitionFeedEntry e1 =
      json_parser.parseAcquisitionFeedEntryFromStream(
        OPDSJSONParserContract.getResource("compatibility-20180921-test-new-1.json"));

    {
      Assert.assertEquals(e0.getAcquisitions(), e1.getAcquisitions());
      Assert.assertEquals(e0.getAvailability(), e1.getAvailability());
      Assert.assertEquals(e0.getAuthors(), e1.getAuthors());
      Assert.assertEquals(e0.getCategories(), e1.getCategories());
      Assert.assertEquals(e0.getCover(), e1.getCover());
      Assert.assertEquals(e0.getGroups(), e1.getGroups());
      Assert.assertEquals(e0.getID(), e1.getID());
      Assert.assertEquals(e0.getPublished(), e1.getPublished());
      Assert.assertEquals(e0.getPublisher(), e1.getPublisher());
      Assert.assertEquals(e0.getSummary(), e1.getSummary());
      Assert.assertEquals(e0.getThumbnail(), e1.getThumbnail());
      Assert.assertEquals(e0.getTitle(), e1.getTitle());
    }
  }

  @Test
  public void testCompatibility20180921_2()
    throws Exception {

    final OPDSJSONParserType json_parser =
      OPDSJSONParser.newParser();

    final OPDSAcquisitionFeedEntry e0 =
      json_parser.parseAcquisitionFeedEntryFromStream(
        OPDSJSONParserContract.getResource("compatibility-20180921-test-old.json"));

    final OPDSAcquisitionFeedEntry e1 =
      json_parser.parseAcquisitionFeedEntryFromStream(
        OPDSJSONParserContract.getResource("compatibility-20180921-test-new-0.json"));

    {
      final OPDSAcquisition e0a = e0.getAcquisitions().get(0);
      final OPDSAcquisition e1a = e1.getAcquisitions().get(0);

      Assert.assertEquals(e0a.availableFinalContentTypes(), e1a.availableFinalContentTypes());
      Assert.assertEquals(e0.getAvailability(), e1.getAvailability());
      Assert.assertEquals(e0.getAuthors(), e1.getAuthors());
      Assert.assertEquals(e0.getCategories(), e1.getCategories());
      Assert.assertEquals(e0.getCover(), e1.getCover());
      Assert.assertEquals(e0.getGroups(), e1.getGroups());
      Assert.assertEquals(e0.getID(), e1.getID());
      Assert.assertEquals(e0.getPublished(), e1.getPublished());
      Assert.assertEquals(e0.getPublisher(), e1.getPublisher());
      Assert.assertEquals(e0.getSummary(), e1.getSummary());
      Assert.assertEquals(e0.getThumbnail(), e1.getThumbnail());
      Assert.assertEquals(e0.getTitle(), e1.getTitle());
    }
  }
}
