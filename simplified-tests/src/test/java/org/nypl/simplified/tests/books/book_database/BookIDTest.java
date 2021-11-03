package org.nypl.simplified.tests.books.book_database;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.nypl.simplified.books.api.BookID;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryBuilderType;
import org.nypl.simplified.opds.core.OPDSAvailabilityOpenAccess;

import java.net.URI;

import kotlin.Suppress;

public final class BookIDTest {

  @Test
  public void testBookIDNewFromOPDSEntry() {
    final OptionType<URI> revoke = Option.none();
    final OPDSAcquisitionFeedEntryBuilderType eb =
      OPDSAcquisitionFeedEntry.newBuilder(
        "http://circulation.alpha.librarysimplified"
          + ".org/works/?urn=urn%3Alibrarysimplified"
          + ".org%2Fterms%2Fid%2FOverdrive%2520ID%2F2b3729cd-27ec-42e1-bc51"
          + "-298aaee0af7d",
        "1Q84",
        NullCheck.notNull(DateTime.now()),
        OPDSAvailabilityOpenAccess.get(revoke));
    final OPDSAcquisitionFeedEntry e = eb.build();
    final BookID b = BookID.Companion.newFromOPDSEntry(e);
    System.out.println("book: " + b);
    Assertions.assertEquals(
      "7a99601f479c30f66f0949c51bbed2adac0e12eb79ad1319db638e16604400bf",
      b.toString());
  }

  @Test
  public void testBookID_0() {
    final BookID b = BookID.Companion.newFromText(
      "http://circulation.alpha.librarysimplified.org/loans/Gutenberg/18405");
    System.out.println(b);
  }

  @Test
  public void testBookIDNotValid0() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> {
      BookID.Companion.create("");
    });
  }

  @Test
  public void testBookIDNotValid1() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> {
      BookID.Companion.create("_");
    });
  }

  @Test
  public void testBookIDNotValid2() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> {
      BookID.Companion.create(" ");
    });
  }

  @Test
  public void testBookIDValid0() {
    Assertions.assertEquals(
      "x577ed4e5ebb4c40e2f4a42ed1c268153624d7e03c09f9b54137518e82a078c1d",
      BookID.Companion.create("x577ed4e5ebb4c40e2f4a42ed1c268153624d7e03c09f9b54137518e82a078c1d").toString());
  }
}
