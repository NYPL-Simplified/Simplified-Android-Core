package org.nypl.simplified.tests.books;

import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;

import org.junit.Assert;
import org.junit.Test;
import org.nypl.simplified.books.core.BookAcquisitionSelection;
import org.nypl.simplified.books.book_database.BookFormats;
import org.nypl.simplified.opds.core.OPDSAcquisition;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParser;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParserType;
import org.nypl.simplified.tests.opds.OPDSFeedEntryParserContract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;

public abstract class BookAcquisitionSelectionContract {

  private static Logger LOG =
    LoggerFactory.getLogger(BookAcquisitionSelectionContract.class);

  private OPDSAcquisitionFeedEntryParserType getParser() {
    return OPDSAcquisitionFeedEntryParser.newParser(BookFormats.Companion.supportedBookMimeTypes());
  }

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
  public void testOpenAccess()
    throws Exception {
    final OPDSAcquisitionFeedEntryParserType parser = this.getParser();
    final OPDSAcquisitionFeedEntry entry = parser.parseEntryStream(
      BookAcquisitionSelectionContract.getResource("entry-availability-open-access.xml"));

    final OptionType<OPDSAcquisition> acquisition_opt =
      BookAcquisitionSelection.INSTANCE.preferredAcquisition(entry.getAcquisitions());

    Assert.assertTrue(acquisition_opt.isSome());

    final OPDSAcquisition acquisition =
      ((Some<OPDSAcquisition>) acquisition_opt).get();

    Assert.assertEquals("https://example.com/Open-Access", acquisition.getUri().toString());
    Assert.assertEquals(1, acquisition.availableFinalContentTypes().size());
    Assert.assertTrue(acquisition.availableFinalContentTypes().contains("application/epub+zip"));
  }

  @Test
  public void testClassics0()
    throws Exception {
    final OPDSAcquisitionFeedEntryParserType parser = this.getParser();
    final OPDSAcquisitionFeedEntry entry = parser.parseEntryStream(
      BookAcquisitionSelectionContract.getResource("entry-classics-0.xml"));

    final OptionType<OPDSAcquisition> acquisition_opt =
      BookAcquisitionSelection.INSTANCE.preferredAcquisition(entry.getAcquisitions());

    Assert.assertTrue(acquisition_opt.isSome());

    final OPDSAcquisition acquisition =
      ((Some<OPDSAcquisition>) acquisition_opt).get();

    Assert.assertEquals(
      "https://circulation.librarysimplified.org/CLASSICS/works/313322/fulfill/1",
      acquisition.getUri().toString());
    Assert.assertEquals(1, acquisition.availableFinalContentTypes().size());
    Assert.assertTrue(acquisition.availableFinalContentTypes().contains("application/epub+zip"));
  }

  @Test
  public void testMultipleFormats0()
    throws Exception {
    final OPDSAcquisitionFeedEntryParserType parser = this.getParser();
    final OPDSAcquisitionFeedEntry entry = parser.parseEntryStream(
      BookAcquisitionSelectionContract.getResource("entry-with-formats-0.xml"));

    final OptionType<OPDSAcquisition> acquisition_opt =
      BookAcquisitionSelection.INSTANCE.preferredAcquisition(entry.getAcquisitions());

    Assert.assertTrue(acquisition_opt.isSome());

    final OPDSAcquisition acquisition =
      ((Some<OPDSAcquisition>) acquisition_opt).get();

    Assert.assertEquals(
      "http://qa.circulation.librarysimplified.org/NYNYPL/works/198679/fulfill/2",
      acquisition.getUri().toString());
    Assert.assertEquals(1, acquisition.availableFinalContentTypes().size());
    Assert.assertTrue(acquisition.availableFinalContentTypes().contains("application/epub+zip"));
  }

  @Test
  public void testMultipleFormats1()
    throws Exception {
    final OPDSAcquisitionFeedEntryParserType parser = this.getParser();
    final OPDSAcquisitionFeedEntry entry = parser.parseEntryStream(
      BookAcquisitionSelectionContract.getResource("entry-with-formats-1.xml"));

    final OptionType<OPDSAcquisition> acquisition_opt =
      BookAcquisitionSelection.INSTANCE.preferredAcquisition(entry.getAcquisitions());

    Assert.assertTrue(acquisition_opt.isSome());

    final OPDSAcquisition acquisition =
      ((Some<OPDSAcquisition>) acquisition_opt).get();

    Assert.assertEquals(
      "http://qa.circulation.librarysimplified.org/NYNYPL/works/Overdrive%20ID/1ac5cc2a-cdc9-46e0-90a4-2de9ada35237/borrow",
      acquisition.getUri().toString());
    Assert.assertEquals(3, acquisition.availableFinalContentTypes().size());
    Assert.assertTrue(acquisition.availableFinalContentTypes().contains("application/epub+zip"));
  }

  @Test
  public void testNoSupportedFormat()
    throws Exception {
    final OPDSAcquisitionFeedEntryParserType parser = this.getParser();
    final OPDSAcquisitionFeedEntry entry = parser.parseEntryStream(
      BookAcquisitionSelectionContract.getResource("entry-no-supported-format.xml"));

    final OptionType<OPDSAcquisition> acquisition_opt =
      BookAcquisitionSelection.INSTANCE.preferredAcquisition(entry.getAcquisitions());

    Assert.assertFalse(acquisition_opt.isSome());
  }

  @Test
  public void testNoSupportedRelation()
    throws Exception {
    final OPDSAcquisitionFeedEntryParserType parser = this.getParser();
    final OPDSAcquisitionFeedEntry entry = parser.parseEntryStream(
      BookAcquisitionSelectionContract.getResource("entry-no-supported-relations.xml"));

    final OptionType<OPDSAcquisition> acquisition_opt =
      BookAcquisitionSelection.INSTANCE.preferredAcquisition(entry.getAcquisitions());

    Assert.assertFalse(acquisition_opt.isSome());
  }
}
