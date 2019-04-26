package org.nypl.simplified.tests.books;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;

import org.junit.Assert;
import org.junit.Test;
import org.nypl.simplified.books.core.BookAcquisitionSelection;
import org.nypl.simplified.books.core.BookFormats;
import org.nypl.simplified.mime.MIMEParser;
import org.nypl.simplified.opds.core.OPDSAcquisition;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParser;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParserType;
import org.nypl.simplified.opds.core.OPDSAcquisitionPath;
import org.nypl.simplified.opds.core.OPDSAcquisitionRelation;
import org.nypl.simplified.tests.opds.OPDSFeedEntryParserContract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

public abstract class BookAcquisitionSelectionContract {

  private static Logger LOG =
    LoggerFactory.getLogger(BookAcquisitionSelectionContract.class);

  private OPDSAcquisitionFeedEntryParserType getParser() {
    return OPDSAcquisitionFeedEntryParser.newParser();
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

    final OptionType<OPDSAcquisitionPath> acquisition_opt =
      BookAcquisitionSelection.INSTANCE.preferredAcquisition(entry.getAcquisitionPaths());

    Assert.assertTrue(acquisition_opt.isSome());

    final OPDSAcquisitionPath acquisition =
      ((Some<OPDSAcquisitionPath>) acquisition_opt).get();

    Assert.assertEquals(
      "https://example.com/Open-Access", acquisition.getNext().getUri().toString());
    Assert.assertEquals(
      "application/epub+zip", acquisition.finalContentType().getFullType());
  }

  @Test
  public void testClassics0()
    throws Exception {
    final OPDSAcquisitionFeedEntryParserType parser = this.getParser();
    final OPDSAcquisitionFeedEntry entry = parser.parseEntryStream(
      BookAcquisitionSelectionContract.getResource("entry-classics-0.xml"));

    final OptionType<OPDSAcquisitionPath> acquisition_opt =
      BookAcquisitionSelection.INSTANCE.preferredAcquisition(entry.getAcquisitionPaths());

    Assert.assertTrue(acquisition_opt.isSome());

    final OPDSAcquisitionPath acquisition =
      ((Some<OPDSAcquisitionPath>) acquisition_opt).get();

    Assert.assertEquals(
      "https://circulation.librarysimplified.org/CLASSICS/works/313322/fulfill/1",
      acquisition.getNext().getUri().toString());
    Assert.assertEquals(
      "application/epub+zip", acquisition.finalContentType().getFullType());
  }

  @Test
  public void testMultipleFormats0()
    throws Exception {
    final OPDSAcquisitionFeedEntryParserType parser = this.getParser();
    final OPDSAcquisitionFeedEntry entry = parser.parseEntryStream(
      BookAcquisitionSelectionContract.getResource("entry-with-formats-0.xml"));

    final OptionType<OPDSAcquisitionPath> acquisition_opt =
      BookAcquisitionSelection.INSTANCE.preferredAcquisition(entry.getAcquisitionPaths());

    Assert.assertTrue(acquisition_opt.isSome());

    final OPDSAcquisitionPath acquisition =
      ((Some<OPDSAcquisitionPath>) acquisition_opt).get();

    Assert.assertEquals(
      "http://qa.circulation.librarysimplified.org/NYNYPL/works/198679/fulfill/2",
      acquisition.getNext().getUri().toString());
    Assert.assertEquals(
      "application/epub+zip", acquisition.finalContentType().getFullType());
  }

  @Test
  public void testMultipleFormats1()
    throws Exception {
    final OPDSAcquisitionFeedEntryParserType parser = this.getParser();
    final OPDSAcquisitionFeedEntry entry = parser.parseEntryStream(
      BookAcquisitionSelectionContract.getResource("entry-with-formats-1.xml"));

    final OptionType<OPDSAcquisitionPath> acquisition_opt =
      BookAcquisitionSelection.INSTANCE.preferredAcquisition(entry.getAcquisitionPaths());

    Assert.assertTrue(acquisition_opt.isSome());

    final OPDSAcquisitionPath acquisition =
      ((Some<OPDSAcquisitionPath>) acquisition_opt).get();

    Assert.assertEquals(
      "http://qa.circulation.librarysimplified.org/NYNYPL/works/Overdrive%20ID/1ac5cc2a-cdc9-46e0-90a4-2de9ada35237/borrow",
      acquisition.getNext().getUri().toString());
    Assert.assertEquals(
      "application/epub+zip", acquisition.finalContentType().getFullType());
  }

  @Test
  public void testBearerTokenPath()
    throws Exception {
    final OPDSAcquisitionFeedEntryParserType parser = this.getParser();
    final OPDSAcquisitionFeedEntry entry = parser.parseEntryStream(
      BookAcquisitionSelectionContract.getResource("entry-with-bearer-token.xml"));

    final OptionType<OPDSAcquisitionPath> acquisition_opt =
      BookAcquisitionSelection.INSTANCE.preferredAcquisition(entry.getAcquisitionPaths());

    Assert.assertTrue(acquisition_opt.isSome());

    final OPDSAcquisitionPath acquisition =
      ((Some<OPDSAcquisitionPath>) acquisition_opt).get();

    Assert.assertEquals(
      "https://circulation.librarysimplified.org/CLASSICS/works/315343/fulfill/17",
      acquisition.getNext().getUri().toString());
    Assert.assertEquals(
      "application/epub+zip", acquisition.finalContentType().getFullType());
  }

  @Test
  public void testNoSupportedFormat()
    throws Exception {
    final OPDSAcquisitionFeedEntryParserType parser = this.getParser();
    final OPDSAcquisitionFeedEntry entry = parser.parseEntryStream(
      BookAcquisitionSelectionContract.getResource("entry-no-supported-format.xml"));

    final OptionType<OPDSAcquisitionPath> acquisition_opt =
      BookAcquisitionSelection.INSTANCE.preferredAcquisition(entry.getAcquisitionPaths());

    Assert.assertFalse(acquisition_opt.isSome());
  }

  @Test
  public void testNoSupportedRelation()
    throws Exception {
    final OPDSAcquisitionFeedEntryParserType parser = this.getParser();
    final OPDSAcquisitionFeedEntry entry = parser.parseEntryStream(
      BookAcquisitionSelectionContract.getResource("entry-no-supported-relations.xml"));

    final OptionType<OPDSAcquisitionPath> acquisition_opt =
      BookAcquisitionSelection.INSTANCE.preferredAcquisition(entry.getAcquisitionPaths());

    Assert.assertFalse(acquisition_opt.isSome());
  }
}
