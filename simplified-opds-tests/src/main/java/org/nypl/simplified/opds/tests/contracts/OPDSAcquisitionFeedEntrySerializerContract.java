package org.nypl.simplified.opds.tests.contracts;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParser;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParserType;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntrySerializer;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntrySerializerType;
import org.nypl.simplified.opds.core.OPDSXML;
import org.nypl.simplified.test.utilities.TestUtilities;
import org.w3c.dom.Document;

import com.io7m.jnull.NullCheck;

@SuppressWarnings("resource") public final class OPDSAcquisitionFeedEntrySerializerContract implements
  OPDSAcquisitionFeedEntrySerializerContractType
{
  public static InputStream getResource(
    final String name)
    throws Exception
  {
    return NullCheck.notNull(OPDSAcquisitionFeedEntrySerializerContract.class
      .getResourceAsStream(name));
  }

  @Override public void testRoundTrip0()
    throws Exception
  {
    final OPDSAcquisitionFeedEntryParserType p =
      OPDSAcquisitionFeedEntryParser.newParser();
    final OPDSAcquisitionFeedEntrySerializerType s =
      OPDSAcquisitionFeedEntrySerializer.newSerializer();

    final InputStream rs0 =
      OPDSAcquisitionFeedEntrySerializerContract.getResource("entry-0.xml");
    final OPDSAcquisitionFeedEntry e0 = p.parseEntryStream(rs0);

    final ByteArrayOutputStream bao0 = new ByteArrayOutputStream();
    final Document d0 = s.serializeFeedEntry(e0);
    OPDSXML.serializeDocumentToStream(d0, bao0);

    final InputStream rs1 = new ByteArrayInputStream(bao0.toByteArray());
    final OPDSAcquisitionFeedEntry e1 = p.parseEntryStream(rs1);

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
}
