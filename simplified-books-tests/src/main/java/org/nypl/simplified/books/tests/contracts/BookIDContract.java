package org.nypl.simplified.books.tests.contracts;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;
import org.nypl.simplified.books.core.BookID;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryBuilderType;
import org.nypl.simplified.opds.core.OPDSAvailabilityOpenAccess;
import org.nypl.simplified.test.utilities.TestUtilities;

import java.net.URI;
import java.util.Calendar;

/**
 * Default implementation of {@link BookIDContractType}.
 */

public final class BookIDContract implements BookIDContractType
{
  /**
   * Construct a contract.
   */

  public BookIDContract()
  {

  }

  @Override public void testBookIDNew()
  {
    final OptionType<URI> revoke = Option.none();
    final OPDSAcquisitionFeedEntryBuilderType eb =
      OPDSAcquisitionFeedEntry.newBuilder(
        "http://circulation.alpha.librarysimplified"
        + ".org/works/?urn=urn%3Alibrarysimplified"
        + ".org%2Fterms%2Fid%2FOverdrive%2520ID%2F2b3729cd-27ec-42e1-bc51"
        + "-298aaee0af7d",
        "1Q84",
        NullCheck.notNull(Calendar.getInstance()),
        OPDSAvailabilityOpenAccess.get(revoke));
    final OPDSAcquisitionFeedEntry e = eb.build();
    final BookID b = BookID.newIDFromEntry(e);
    System.out.println("book: " + b);
    TestUtilities.assertEquals(
      "7a99601f479c30f66f0949c51bbed2adac0e12eb79ad1319db638e16604400bf",
      b.toString());
  }

  @Override public void testBookID_0()
  {
    final BookID b = BookID.newFromText(
      "http://circulation.alpha.librarysimplified.org/loans/Gutenberg/18405");
    System.out.println(b);
  }
}
