package org.nypl.simplified.tests.opds;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;

import org.junit.Assert;
import org.junit.Test;
import org.nypl.simplified.books.core.BookFormats;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParser;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParserType;
import org.nypl.simplified.opds.core.OPDSAvailabilityHeld;
import org.nypl.simplified.opds.core.OPDSAvailabilityHeldReady;
import org.nypl.simplified.opds.core.OPDSAvailabilityHoldable;
import org.nypl.simplified.opds.core.OPDSAvailabilityLoanable;
import org.nypl.simplified.opds.core.OPDSAvailabilityLoaned;
import org.nypl.simplified.opds.core.OPDSAvailabilityOpenAccess;
import org.nypl.simplified.opds.core.OPDSAvailabilityType;
import org.nypl.simplified.rfc3339.core.RFC3339Formatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Calendar;

/**
 * Entry parser contract.
 */

public abstract class OPDSFeedEntryParserContract {

  private static Logger LOG =
    LoggerFactory.getLogger(OPDSFeedEntryParserContract.class);

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
  public void testEntryAvailabilityLoanable()
      throws Exception {
    final OPDSAcquisitionFeedEntryParserType parser = this.getParser();
    final OPDSAcquisitionFeedEntry e = parser.parseEntryStream(
        OPDSFeedEntryParserContract.getResource(
            "entry-availability-loanable.xml"));

    final OPDSAvailabilityType availability = e.getAvailability();
    final OPDSAvailabilityLoanable expected = OPDSAvailabilityLoanable.get();
    Assert.assertEquals(expected, availability);
    Assert.assertTrue("No available acquisitions", e.getAcquisitions().isEmpty());
  }

  @Test
  public void testEntryAvailabilityLoanedIndefinite()
      throws Exception {
    final OPDSAcquisitionFeedEntryParserType parser = this.getParser();
    final OPDSAcquisitionFeedEntry e = parser.parseEntryStream(
        OPDSFeedEntryParserContract.getResource(
            "entry-availability-loaned-indefinite.xml"));

    final OPDSAvailabilityType availability = e.getAvailability();

    final OptionType<Calendar> expected_start_date = Option.some(
        RFC3339Formatter.parseRFC3339Date("2000-01-01T00:00:00Z"));
    final OptionType<Calendar> expected_end_date = Option.none();
    final OptionType<URI> expected_revoke =
        Option.some(new URI("http://example.com/revoke"));
    final OPDSAvailabilityLoaned expected = OPDSAvailabilityLoaned.get(
        expected_start_date, expected_end_date, expected_revoke);

    Assert.assertEquals(expected, availability);
    Assert.assertFalse("Available acquisitions", e.getAcquisitions().isEmpty());
  }

  @Test
  public void testEntryAvailabilityLoanedTimed()
      throws Exception {
    final OPDSAcquisitionFeedEntryParserType parser = this.getParser();
    final OPDSAcquisitionFeedEntry e = parser.parseEntryStream(
        OPDSFeedEntryParserContract.getResource(
            "entry-availability-loaned-timed.xml"));

    final OPDSAvailabilityType availability = e.getAvailability();

    final OptionType<Calendar> expected_start_date = Option.some(
        RFC3339Formatter.parseRFC3339Date("2000-01-01T00:00:00Z"));
    final OptionType<Calendar> expected_end_date = Option.some(
        RFC3339Formatter.parseRFC3339Date("2010-01-01T00:00:00Z"));
    final OptionType<URI> expected_revoke =
        Option.some(new URI("http://example.com/revoke"));
    final OPDSAvailabilityLoaned expected = OPDSAvailabilityLoaned.get(
        expected_start_date, expected_end_date, expected_revoke);

    Assert.assertEquals(expected, availability);
    Assert.assertFalse("Available acquisitions", e.getAcquisitions().isEmpty());
  }

  @Test
  public void testEntryAvailabilityHoldable()
      throws Exception {
    final OPDSAcquisitionFeedEntryParserType parser = this.getParser();
    final OPDSAcquisitionFeedEntry e = parser.parseEntryStream(
        OPDSFeedEntryParserContract.getResource(
            "entry-availability-holdable.xml"));

    final OPDSAvailabilityType availability = e.getAvailability();
    final OPDSAvailabilityHoldable expected = OPDSAvailabilityHoldable.get();

    Assert.assertEquals(expected, availability);
    Assert.assertFalse("Available acquisitions", e.getAcquisitions().isEmpty());
  }

  @Test
  public void testEntryAvailabilityHeldIndefinite()
      throws Exception {
    final OPDSAcquisitionFeedEntryParserType parser = this.getParser();
    final OPDSAcquisitionFeedEntry e = parser.parseEntryStream(
        OPDSFeedEntryParserContract.getResource(
            "entry-availability-held-indefinite.xml"));

    final OPDSAvailabilityType availability = e.getAvailability();

    final OptionType<Calendar> expected_start_date = Option.some(
        RFC3339Formatter.parseRFC3339Date("2000-01-01T00:00:00Z"));
    final OptionType<Integer> queue_position = Option.none();
    final OptionType<Calendar> expected_end_date = Option.none();
    final OptionType<URI> expected_revoke =
        Option.some(new URI("http://example.com/revoke"));
    final OPDSAvailabilityHeld expected = OPDSAvailabilityHeld.get(
        expected_start_date, queue_position, expected_end_date, expected_revoke);

    Assert.assertEquals(expected, availability);
    Assert.assertFalse("Available acquisitions", e.getAcquisitions().isEmpty());
  }

  @Test
  public void testEntryAvailabilityHeldTimed()
      throws Exception {
    final OPDSAcquisitionFeedEntryParserType parser = this.getParser();
    final OPDSAcquisitionFeedEntry e = parser.parseEntryStream(
        OPDSFeedEntryParserContract.getResource(
            "entry-availability-held-timed.xml"));

    final OPDSAvailabilityType availability = e.getAvailability();

    final OptionType<Calendar> expected_start_date = Option.some(
        RFC3339Formatter.parseRFC3339Date("2000-01-01T00:00:00Z"));
    final OptionType<Calendar> expected_end_date = Option.some(
        RFC3339Formatter.parseRFC3339Date("2010-01-01T00:00:00Z"));
    final OptionType<Integer> queue_position = Option.none();
    final OptionType<URI> expected_revoke =
        Option.some(new URI("http://example.com/revoke"));
    final OPDSAvailabilityHeld expected = OPDSAvailabilityHeld.get(
        expected_start_date, queue_position, expected_end_date, expected_revoke);

    Assert.assertEquals(expected, availability);
    Assert.assertFalse("Available acquisitions", e.getAcquisitions().isEmpty());
  }

  @Test
  public void testEntryAvailabilityHeldIndefiniteQueued()
      throws Exception {
    final OPDSAcquisitionFeedEntryParserType parser = this.getParser();
    final OPDSAcquisitionFeedEntry e = parser.parseEntryStream(
        OPDSFeedEntryParserContract.getResource(
            "entry-availability-held-indefinite-queued.xml"));

    final OPDSAvailabilityType availability = e.getAvailability();

    final OptionType<Calendar> expected_start_date = Option.some(
        RFC3339Formatter.parseRFC3339Date("2000-01-01T00:00:00Z"));
    final OptionType<Integer> queue_position = Option.some(3);
    final OptionType<Calendar> expected_end_date = Option.none();
    final OptionType<URI> expected_revoke =
        Option.some(new URI("http://example.com/revoke"));
    final OPDSAvailabilityHeld expected = OPDSAvailabilityHeld.get(
        expected_start_date, queue_position, expected_end_date, expected_revoke);

    Assert.assertEquals(expected, availability);
    Assert.assertFalse("Available acquisitions", e.getAcquisitions().isEmpty());
  }

  @Test
  public void testEntryAvailabilityHeldTimedQueued()
      throws Exception {
    final OPDSAcquisitionFeedEntryParserType parser = this.getParser();
    final OPDSAcquisitionFeedEntry e = parser.parseEntryStream(
        OPDSFeedEntryParserContract.getResource(
            "entry-availability-held-timed-queued.xml"));

    final OPDSAvailabilityType availability = e.getAvailability();

    final OptionType<Calendar> expected_start_date = Option.some(
        RFC3339Formatter.parseRFC3339Date("2000-01-01T00:00:00Z"));
    final OptionType<Calendar> expected_end_date = Option.some(
        RFC3339Formatter.parseRFC3339Date("2010-01-01T00:00:00Z"));
    final OptionType<Integer> queue_position = Option.some(3);
    final OptionType<URI> expected_revoke =
        Option.some(new URI("http://example.com/revoke"));
    final OPDSAvailabilityHeld expected = OPDSAvailabilityHeld.get(
        expected_start_date, queue_position, expected_end_date, expected_revoke);

    Assert.assertEquals(expected, availability);
    Assert.assertFalse("Available acquisitions", e.getAcquisitions().isEmpty());
  }

  @Test
  public void testEntryAvailabilityHeldReady()
      throws Exception {
    final OPDSAcquisitionFeedEntryParserType parser = this.getParser();
    final OPDSAcquisitionFeedEntry e = parser.parseEntryStream(
        OPDSFeedEntryParserContract.getResource(
            "entry-availability-heldready.xml"));

    final OPDSAvailabilityType availability = e.getAvailability();

    final OptionType<Calendar> expected_end_date = Option.none();
    final OptionType<URI> expected_revoke =
        Option.some(new URI("http://example.com/revoke"));
    final OPDSAvailabilityHeldReady expected =
        OPDSAvailabilityHeldReady.get(expected_end_date, expected_revoke);

    Assert.assertEquals(expected, availability);
    Assert.assertFalse("Available acquisitions", e.getAcquisitions().isEmpty());
  }

  @Test
  public void testEntryAvailabilityReservedTimed()
      throws Exception {
    final OPDSAcquisitionFeedEntryParserType parser = this.getParser();
    final OPDSAcquisitionFeedEntry e = parser.parseEntryStream(
        OPDSFeedEntryParserContract.getResource(
            "entry-availability-heldready-timed.xml"));

    final OPDSAvailabilityType availability = e.getAvailability();

    final OptionType<Calendar> expected_end_date = Option.some(
        RFC3339Formatter.parseRFC3339Date("2010-01-01T00:00:00Z"));
    final OptionType<URI> expected_revoke =
        Option.some(new URI("http://example.com/revoke"));
    final OPDSAvailabilityHeldReady expected =
        OPDSAvailabilityHeldReady.get(expected_end_date, expected_revoke);

    Assert.assertEquals(expected, availability);
    Assert.assertFalse("Available acquisitions", e.getAcquisitions().isEmpty());
  }

  @Test
  public void testEntryAvailabilityOpenAccess()
      throws Exception {
    final OPDSAcquisitionFeedEntryParserType parser = this.getParser();
    final OPDSAcquisitionFeedEntry e = parser.parseEntryStream(
        OPDSFeedEntryParserContract.getResource(
            "entry-availability-open-access.xml"));

    final OPDSAvailabilityType availability = e.getAvailability();

    final OptionType<URI> expected_revoke =
        Option.some(new URI("http://example.com/revoke"));
    final OPDSAvailabilityOpenAccess expected =
        OPDSAvailabilityOpenAccess.get(expected_revoke);

    Assert.assertEquals(expected, availability);
    Assert.assertFalse("Available acquisitions", e.getAcquisitions().isEmpty());
  }

  @Test
  public void testEntryAvailabilityReservedSpecific0()
      throws Exception {
    final OPDSAcquisitionFeedEntryParserType parser = this.getParser();
    final OPDSAcquisitionFeedEntry e = parser.parseEntryStream(
        OPDSFeedEntryParserContract.getResource(
          "entry-availability-heldready-specific0.xml"));

    final OPDSAvailabilityType availability = e.getAvailability();

    final OptionType<Calendar> expected_end_date = Option.some(
        RFC3339Formatter.parseRFC3339Date("2015-08-24T00:30:24Z"));
    final OptionType<URI> expected_revoke =
        Option.some(new URI("http://example.com/revoke"));
    final OPDSAvailabilityHeldReady expected =
        OPDSAvailabilityHeldReady.get(expected_end_date, expected_revoke);

    Assert.assertEquals(expected, availability);
    Assert.assertFalse("Available acquisitions", e.getAcquisitions().isEmpty());
  }

  @Test
  public void testEntryNoSupportedFormats()
    throws Exception {
    final OPDSAcquisitionFeedEntryParserType parser = this.getParser();
    final OPDSAcquisitionFeedEntry e = parser.parseEntryStream(
      OPDSFeedEntryParserContract.getResource("entry-no-supported-format.xml"));

    Assert.assertTrue("No available acquisitions", e.getAcquisitions().isEmpty());
  }

  private OPDSAcquisitionFeedEntryParserType getParser() {
    return OPDSAcquisitionFeedEntryParser.newParser(BookFormats.supportedBookMimeTypes());
  }
}
