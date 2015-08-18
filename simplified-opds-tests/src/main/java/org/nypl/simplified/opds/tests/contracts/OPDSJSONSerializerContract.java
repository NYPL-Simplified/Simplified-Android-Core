package org.nypl.simplified.opds.tests.contracts;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

import org.nypl.simplified.opds.core.OPDSAcquisitionFeed;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParser;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParserType;
import org.nypl.simplified.opds.core.OPDSFeedParser;
import org.nypl.simplified.opds.core.OPDSFeedParserType;
import org.nypl.simplified.opds.core.OPDSJSONParser;
import org.nypl.simplified.opds.core.OPDSJSONParserType;
import org.nypl.simplified.opds.core.OPDSJSONSerializer;
import org.nypl.simplified.opds.core.OPDSJSONSerializerType;
import org.nypl.simplified.test.utilities.TestUtilities;

import com.io7m.jnull.NullCheck;

@SuppressWarnings("resource") public final class OPDSJSONSerializerContract implements
  OPDSJSONSerializerContractType
{
  public static InputStream getResource(
    final String name)
    throws Exception
  {
    return NullCheck.notNull(OPDSJSONSerializerContract.class
      .getResourceAsStream(name));
  }

  @Override public void testRoundTrip0()
    throws Exception
  {
    final OPDSAcquisitionFeedEntryParserType p =
      OPDSAcquisitionFeedEntryParser.newParser();
    final OPDSJSONParserType jp = OPDSJSONParser.newParser();

    final OPDSJSONSerializerType s = OPDSJSONSerializer.newSerializer();

    final InputStream rs0 =
      OPDSJSONSerializerContract.getResource("entry-0.xml");
    final OPDSAcquisitionFeedEntry e0 = p.parseEntryStream(rs0);

    final ByteArrayOutputStream bao0 = new ByteArrayOutputStream();
    s.serializeToStream(s.serializeFeedEntry(e0), bao0);
    s.serializeToStream(s.serializeFeedEntry(e0), System.out);

    final InputStream rs1 = new ByteArrayInputStream(bao0.toByteArray());
    final OPDSAcquisitionFeedEntry e1 =
      jp.parseAcquisitionFeedEntryFromStream(rs1);

    {
      TestUtilities.assertEquals(e0.getAcquisitions(), e1.getAcquisitions());
      TestUtilities.assertEquals(e0.getAuthors(), e1.getAuthors());
      TestUtilities.assertEquals(e0.getCategories(), e1.getCategories());
      TestUtilities.assertEquals(e0.getCover(), e1.getCover());
      TestUtilities.assertEquals(e0.getGroups(), e1.getGroups());
      TestUtilities.assertEquals(e0.getID(), e1.getID());
      TestUtilities.assertEquals(e0.getPublished(), e1.getPublished());
      TestUtilities.assertEquals(e0.getPublisher(), e1.getPublisher());
      TestUtilities.assertEquals(e0.getSummary(), e1.getSummary());
      TestUtilities.assertEquals(e0.getThumbnail(), e1.getThumbnail());
      TestUtilities.assertEquals(e0.getTitle(), e1.getTitle());
      // Forget comparing instances of Calendar, no implementation gets this
      // right
      // TestUtilities.assertEquals(e0.getUpdated(), e1.getUpdated());
    }
  }

  @Override public void testRoundTrip1()
    throws Exception
  {
    final OPDSAcquisitionFeedEntryParserType ep =
      OPDSAcquisitionFeedEntryParser.newParser();
    final OPDSFeedParserType p = OPDSFeedParser.newParser(ep);
    final OPDSJSONParserType jp = OPDSJSONParser.newParser();

    final OPDSJSONSerializerType s = OPDSJSONSerializer.newSerializer();

    final InputStream rs0 =
      OPDSJSONSerializerContract.getResource("loans.xml");
    final OPDSAcquisitionFeed fe0 =
      p.parse(new URI("http://example.com"), rs0);

    final ByteArrayOutputStream bao0 = new ByteArrayOutputStream();
    s.serializeToStream(s.serializeFeed(fe0), bao0);
    s.serializeToStream(s.serializeFeed(fe0), System.out);

    final InputStream rs1 = new ByteArrayInputStream(bao0.toByteArray());
    final OPDSAcquisitionFeed fe1 = jp.parseAcquisitionFeedFromStream(rs1);

    {
      final List<OPDSAcquisitionFeedEntry> fe0e = fe0.getFeedEntries();
      final List<OPDSAcquisitionFeedEntry> fe1e = fe1.getFeedEntries();
      for (int index = 0; index < fe0e.size(); ++index) {
        final OPDSAcquisitionFeedEntry e0 = fe0e.get(index);
        final OPDSAcquisitionFeedEntry e1 = fe1e.get(index);
        TestUtilities
          .assertEquals(e0.getAcquisitions(), e1.getAcquisitions());
        TestUtilities.assertEquals(e0.getAuthors(), e1.getAuthors());
        TestUtilities.assertEquals(e0.getCategories(), e1.getCategories());
        TestUtilities.assertEquals(e0.getCover(), e1.getCover());
        TestUtilities.assertEquals(e0.getGroups(), e1.getGroups());
        TestUtilities.assertEquals(e0.getID(), e1.getID());
        TestUtilities.assertEquals(e0.getPublisher(), e1.getPublisher());
        TestUtilities.assertEquals(e0.getSummary(), e1.getSummary());
        TestUtilities.assertEquals(e0.getThumbnail(), e1.getThumbnail());
        TestUtilities.assertEquals(e0.getTitle(), e1.getTitle());
        // Forget comparing instances of Calendar, no implementation gets this
        // right
        // TestUtilities.assertEquals(e0.getUpdated(), e1.getUpdated());
        // TestUtilities.assertEquals(e0.getPublished(), e1.getPublished());
      }
    }
  }
}
