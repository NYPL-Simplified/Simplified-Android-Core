package org.nypl.simplified.opds.tests.contracts;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.util.Calendar;

import org.nypl.simplified.opds.core.OPDSNavigationFeed;
import org.nypl.simplified.opds.core.OPDSNavigationFeedBuilderType;
import org.nypl.simplified.opds.core.OPDSNavigationFeedEntry;
import org.nypl.simplified.opds.tests.utilities.TestUtilities;

import com.io7m.jfunctional.Option;

public final class OPDSNavigationFeedContract implements
  OPDSNavigationFeedContractType
{
  public OPDSNavigationFeedContract()
  {
    // Nothing
  }

  @Override public void testSerialization()
    throws Exception
  {
    final OPDSNavigationFeedBuilderType fb =
      OPDSNavigationFeed.newBuilder("id", Calendar.getInstance(), "Title");
    fb.addEntry(OPDSNavigationFeedEntry.newEntry(
      "id",
      "title",
      Calendar.getInstance(),
      Option.some(URI.create("http://example.com")),
      URI.create("http://example.com")));
    fb.addEntry(OPDSNavigationFeedEntry.newEntry(
      "id",
      "title",
      Calendar.getInstance(),
      Option.some(URI.create("http://example.com")),
      URI.create("http://example.com")));
    final OPDSNavigationFeed f0 = fb.build();

    final ByteArrayOutputStream bo = new ByteArrayOutputStream(8192);
    final ObjectOutputStream os = new ObjectOutputStream(bo);
    os.writeObject(f0);
    os.flush();

    final ByteArrayInputStream bi =
      new ByteArrayInputStream(bo.toByteArray());
    final ObjectInputStream is = new ObjectInputStream(bi);
    final OPDSNavigationFeed f1 = (OPDSNavigationFeed) is.readObject();

    TestUtilities.assertEquals(f0, f1);
  }
}
