package org.nypl.simplified.opds.tests.contracts;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.util.Calendar;

import org.nypl.simplified.opds.core.OPDSAcquisition;
import org.nypl.simplified.opds.core.OPDSAcquisition.Type;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeed;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedBuilderType;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryBuilderType;
import org.nypl.simplified.opds.tests.utilities.TestUtilities;

@SuppressWarnings("null") public final class OPDSAcquisitionFeedContract implements
  OPDSAcquisitionFeedContractType
{
  public OPDSAcquisitionFeedContract()
  {
    // Nothing
  }

  @Override public void testSerialization()
    throws Exception
  {
    final URI uri = URI.create("http://example.com/base");

    final OPDSAcquisitionFeedBuilderType fb =
      OPDSAcquisitionFeed.newBuilder(
        uri,
        "id",
        Calendar.getInstance(),
        "Title");

    final OPDSAcquisitionFeedEntryBuilderType eb =
      OPDSAcquisitionFeedEntry.newBuilder(
        "id",
        "title",
        Calendar.getInstance(),
        "published");
    eb.addAuthor("Author");
    eb.addAcquisition(new OPDSAcquisition(Type.ACQUISITION_BUY, URI
      .create("http://example.com")));

    fb.addEntry(eb.build());
    fb.addEntry(eb.build());

    final OPDSAcquisitionFeed f0 = fb.build();
    TestUtilities.assertEquals(uri, f0.getFeedURI());

    final ByteArrayOutputStream bo = new ByteArrayOutputStream(8192);
    final ObjectOutputStream os = new ObjectOutputStream(bo);
    os.writeObject(f0);
    os.flush();

    final ByteArrayInputStream bi =
      new ByteArrayInputStream(bo.toByteArray());
    final ObjectInputStream is = new ObjectInputStream(bi);
    final OPDSAcquisitionFeed f1 = (OPDSAcquisitionFeed) is.readObject();

    TestUtilities.assertEquals(f0, f1);
  }
}
