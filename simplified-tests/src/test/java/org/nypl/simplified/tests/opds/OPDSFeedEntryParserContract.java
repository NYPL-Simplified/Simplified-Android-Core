package org.nypl.simplified.tests.opds;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Assert;
import org.junit.Test;
import org.nypl.simplified.opds.core.DRMLicensor;
import org.nypl.simplified.opds.core.OPDSAcquisition;
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
import org.nypl.simplified.opds.core.OPDSIndirectAcquisition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

import one.irradia.mime.api.MIMEType;
import one.irradia.mime.vanilla.MIMEParser;

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
    final OPDSAcquisitionFeedEntry e = parser.parseEntryStream(URI.create("urn:test"),
      OPDSFeedEntryParserContract.getResource(
          "entry-availability-loanable.xml"));

    final OPDSAvailabilityType availability = e.getAvailability();
    final OPDSAvailabilityLoanable expected = OPDSAvailabilityLoanable.get();
    Assert.assertEquals(expected, availability);

    Assert.assertEquals(1, e.getAcquisitions().size());
  }

  @Test
  public void testEntryAvailabilityLoanedIndefinite()
      throws Exception {
    final OPDSAcquisitionFeedEntryParserType parser = this.getParser();
    final OPDSAcquisitionFeedEntry e = parser.parseEntryStream(URI.create("urn:test"),
      OPDSFeedEntryParserContract.getResource(
          "entry-availability-loaned-indefinite.xml"));

    final OPDSAvailabilityType availability = e.getAvailability();

    final OptionType<DateTime> expected_start_date = Option.some(
        ISODateTimeFormat.dateTimeParser().parseDateTime("2000-01-01T00:00:00Z"));
    final OptionType<DateTime> expected_end_date = Option.none();
    final OptionType<URI> expected_revoke =
        Option.some(new URI("http://example.com/revoke"));
    final OPDSAvailabilityLoaned expected = OPDSAvailabilityLoaned.get(
        expected_start_date, expected_end_date, expected_revoke);

    Assert.assertEquals(expected, availability);

    Assert.assertEquals(1, e.getAcquisitions().size());
    final OPDSAcquisition acquisition = e.getAcquisitions().get(0);
    Assert.assertTrue(
      "application/epub+zip is available",
      OPDSIndirectAcquisition.Companion.findTypeInOptional(
        mimeOf("application/epub+zip"),
        acquisition.getIndirectAcquisitions()).isSome());
  }

  @Test
  public void testEntryAvailabilityLoanedTimed()
      throws Exception {
    final OPDSAcquisitionFeedEntryParserType parser = this.getParser();
    final OPDSAcquisitionFeedEntry e = parser.parseEntryStream(URI.create("urn:test"),
      OPDSFeedEntryParserContract.getResource(
          "entry-availability-loaned-timed.xml"));

    final OPDSAvailabilityType availability = e.getAvailability();

    final OptionType<DateTime> expected_start_date = Option.some(
        ISODateTimeFormat.dateTimeParser().parseDateTime("2000-01-01T00:00:00Z"));
    final OptionType<DateTime> expected_end_date = Option.some(
        ISODateTimeFormat.dateTimeParser().parseDateTime("2010-01-01T00:00:00Z"));
    final OptionType<URI> expected_revoke =
        Option.some(new URI("http://example.com/revoke"));
    final OPDSAvailabilityLoaned expected = OPDSAvailabilityLoaned.get(
        expected_start_date, expected_end_date, expected_revoke);

    Assert.assertEquals(expected, availability);

    Assert.assertEquals(1, e.getAcquisitions().size());
    final OPDSAcquisition acquisition = e.getAcquisitions().get(0);
    Assert.assertTrue(
      "application/epub+zip is available",
      OPDSIndirectAcquisition.Companion.findTypeInOptional(
        mimeOf("application/epub+zip"),
        acquisition.getIndirectAcquisitions()).isSome());
  }

  @Test
  public void testEntryAvailabilityHoldable()
      throws Exception {
    final OPDSAcquisitionFeedEntryParserType parser = this.getParser();
    final OPDSAcquisitionFeedEntry e = parser.parseEntryStream(URI.create("urn:test"),
      OPDSFeedEntryParserContract.getResource(
          "entry-availability-holdable.xml"));

    final OPDSAvailabilityType availability = e.getAvailability();
    final OPDSAvailabilityHoldable expected = OPDSAvailabilityHoldable.get();

    Assert.assertEquals(expected, availability);
    Assert.assertEquals(1, e.getAcquisitions().size());

    final OPDSAcquisition acquisition = e.getAcquisitions().get(0);
    Assert.assertTrue(
      "application/epub+zip is available",
      OPDSIndirectAcquisition.Companion.findTypeInOptional(
        mimeOf("application/epub+zip"),
        acquisition.getIndirectAcquisitions()).isSome());
  }

  @Test
  public void testEntryAvailabilityHeldIndefinite()
      throws Exception {
    final OPDSAcquisitionFeedEntryParserType parser = this.getParser();
    final OPDSAcquisitionFeedEntry e = parser.parseEntryStream(URI.create("urn:test"),
      OPDSFeedEntryParserContract.getResource(
          "entry-availability-held-indefinite.xml"));

    final OPDSAvailabilityType availability = e.getAvailability();

    final OptionType<DateTime> expected_start_date = Option.some(
        ISODateTimeFormat.dateTimeParser().parseDateTime("2000-01-01T00:00:00Z"));
    final OptionType<Integer> queue_position = Option.none();
    final OptionType<DateTime> expected_end_date = Option.none();
    final OptionType<URI> expected_revoke =
        Option.some(new URI("http://example.com/revoke"));
    final OPDSAvailabilityHeld expected = OPDSAvailabilityHeld.get(
        expected_start_date, queue_position, expected_end_date, expected_revoke);

    Assert.assertEquals(expected, availability);
    Assert.assertEquals(1, e.getAcquisitions().size());

    final OPDSAcquisition acquisition = e.getAcquisitions().get(0);
    Assert.assertTrue(
      "application/epub+zip is available",
      OPDSIndirectAcquisition.Companion.findTypeInOptional(
        mimeOf("application/epub+zip"),
        acquisition.getIndirectAcquisitions()).isSome());
  }

  @Test
  public void testEntryAvailabilityHeldTimed()
      throws Exception {
    final OPDSAcquisitionFeedEntryParserType parser = this.getParser();
    final OPDSAcquisitionFeedEntry e = parser.parseEntryStream(URI.create("urn:test"),
      OPDSFeedEntryParserContract.getResource(
          "entry-availability-held-timed.xml"));

    final OPDSAvailabilityType availability = e.getAvailability();

    final OptionType<DateTime> expected_start_date = Option.some(
        ISODateTimeFormat.dateTimeParser().parseDateTime("2000-01-01T00:00:00Z"));
    final OptionType<DateTime> expected_end_date = Option.some(
        ISODateTimeFormat.dateTimeParser().parseDateTime("2010-01-01T00:00:00Z"));
    final OptionType<Integer> queue_position = Option.none();
    final OptionType<URI> expected_revoke =
        Option.some(new URI("http://example.com/revoke"));
    final OPDSAvailabilityHeld expected = OPDSAvailabilityHeld.get(
        expected_start_date, queue_position, expected_end_date, expected_revoke);

    Assert.assertEquals(expected, availability);
    Assert.assertEquals(1, e.getAcquisitions().size());

    final OPDSAcquisition acquisition = e.getAcquisitions().get(0);
    Assert.assertTrue(
      "application/epub+zip is available",
      OPDSIndirectAcquisition.Companion.findTypeInOptional(
        mimeOf("application/epub+zip"),
        acquisition.getIndirectAcquisitions()).isSome());
  }

  @Test
  public void testEntryAvailabilityHeldIndefiniteQueued()
      throws Exception {
    final OPDSAcquisitionFeedEntryParserType parser = this.getParser();
    final OPDSAcquisitionFeedEntry e = parser.parseEntryStream(URI.create("urn:test"),
      OPDSFeedEntryParserContract.getResource(
          "entry-availability-held-indefinite-queued.xml"));

    final OPDSAvailabilityType availability = e.getAvailability();

    final OptionType<DateTime> expected_start_date = Option.some(
        ISODateTimeFormat.dateTimeParser().parseDateTime("2000-01-01T00:00:00Z"));
    final OptionType<Integer> queue_position = Option.some(3);
    final OptionType<DateTime> expected_end_date = Option.none();
    final OptionType<URI> expected_revoke =
        Option.some(new URI("http://example.com/revoke"));
    final OPDSAvailabilityHeld expected = OPDSAvailabilityHeld.get(
        expected_start_date, queue_position, expected_end_date, expected_revoke);

    Assert.assertEquals(expected, availability);
    Assert.assertEquals(1, e.getAcquisitions().size());

    final OPDSAcquisition acquisition = e.getAcquisitions().get(0);
    Assert.assertTrue(
      "application/epub+zip is available",
      OPDSIndirectAcquisition.Companion.findTypeInOptional(
        mimeOf("application/epub+zip"),
        acquisition.getIndirectAcquisitions()).isSome());
  }

  @Test
  public void testEntryAvailabilityHeldTimedQueued()
      throws Exception {
    final OPDSAcquisitionFeedEntryParserType parser = this.getParser();
    final OPDSAcquisitionFeedEntry e = parser.parseEntryStream(URI.create("urn:test"),
      OPDSFeedEntryParserContract.getResource(
          "entry-availability-held-timed-queued.xml"));

    final OPDSAvailabilityType availability = e.getAvailability();

    final OptionType<DateTime> expected_start_date = Option.some(
        ISODateTimeFormat.dateTimeParser().parseDateTime("2000-01-01T00:00:00Z"));
    final OptionType<DateTime> expected_end_date = Option.some(
        ISODateTimeFormat.dateTimeParser().parseDateTime("2010-01-01T00:00:00Z"));
    final OptionType<Integer> queue_position = Option.some(3);
    final OptionType<URI> expected_revoke =
        Option.some(new URI("http://example.com/revoke"));
    final OPDSAvailabilityHeld expected = OPDSAvailabilityHeld.get(
        expected_start_date, queue_position, expected_end_date, expected_revoke);

    Assert.assertEquals(expected, availability);
    Assert.assertEquals(1, e.getAcquisitions().size());

    final OPDSAcquisition acquisition = e.getAcquisitions().get(0);
    Assert.assertTrue(
      "application/epub+zip is available",
      OPDSIndirectAcquisition.Companion.findTypeInOptional(
        mimeOf("application/epub+zip"),
        acquisition.getIndirectAcquisitions()).isSome());
  }

  @Test
  public void testEntryAvailabilityHeldReady()
      throws Exception {
    final OPDSAcquisitionFeedEntryParserType parser = this.getParser();
    final OPDSAcquisitionFeedEntry e = parser.parseEntryStream(URI.create("urn:test"),
      OPDSFeedEntryParserContract.getResource(
          "entry-availability-heldready.xml"));

    final OPDSAvailabilityType availability = e.getAvailability();

    final OptionType<DateTime> expected_end_date = Option.none();
    final OptionType<URI> expected_revoke =
        Option.some(new URI("http://example.com/revoke"));
    final OPDSAvailabilityHeldReady expected =
        OPDSAvailabilityHeldReady.get(expected_end_date, expected_revoke);

    Assert.assertEquals(expected, availability);
    Assert.assertEquals(1, e.getAcquisitions().size());

    final OPDSAcquisition acquisition = e.getAcquisitions().get(0);
    Assert.assertTrue(
      "application/epub+zip is available",
      OPDSIndirectAcquisition.Companion.findTypeInOptional(
        mimeOf("application/epub+zip"),
        acquisition.getIndirectAcquisitions()).isSome());
  }

  @Test
  public void testEntryAvailabilityReservedTimed()
      throws Exception {
    final OPDSAcquisitionFeedEntryParserType parser = this.getParser();
    final OPDSAcquisitionFeedEntry e = parser.parseEntryStream(URI.create("urn:test"),
      OPDSFeedEntryParserContract.getResource(
          "entry-availability-heldready-timed.xml"));

    final OPDSAvailabilityType availability = e.getAvailability();

    final OptionType<DateTime> expected_end_date = Option.some(
        ISODateTimeFormat.dateTimeParser().parseDateTime("2010-01-01T00:00:00Z"));
    final OptionType<URI> expected_revoke =
        Option.some(new URI("http://example.com/revoke"));
    final OPDSAvailabilityHeldReady expected =
        OPDSAvailabilityHeldReady.get(expected_end_date, expected_revoke);

    Assert.assertEquals(expected, availability);
    Assert.assertEquals(1, e.getAcquisitions().size());

    final OPDSAcquisition acquisition = e.getAcquisitions().get(0);
    Assert.assertTrue(
      "application/epub+zip is available",
      OPDSIndirectAcquisition.Companion.findTypeInOptional(
        mimeOf("application/epub+zip"),
        acquisition.getIndirectAcquisitions()).isSome());
  }

  @Test
  public void testEntryAvailabilityOpenAccess()
      throws Exception {
    final OPDSAcquisitionFeedEntryParserType parser = this.getParser();
    final OPDSAcquisitionFeedEntry e = parser.parseEntryStream(URI.create("urn:test"),
      OPDSFeedEntryParserContract.getResource(
          "entry-availability-open-access.xml"));

    final OPDSAvailabilityType availability = e.getAvailability();

    final OptionType<URI> expected_revoke =
        Option.some(new URI("http://example.com/revoke"));
    final OPDSAvailabilityOpenAccess expected =
        OPDSAvailabilityOpenAccess.get(expected_revoke);

    Assert.assertEquals(expected, availability);
    Assert.assertEquals(1, e.getAcquisitions().size());

    final OPDSAcquisition acquisition = e.getAcquisitions().get(0);
    Assert.assertEquals(0, acquisition.getIndirectAcquisitions().size());
    Assert.assertEquals(mimeOf("application/epub+zip"), acquisition.getType());
  }

  @Test
  public void testEntryAvailabilityReservedSpecific0()
      throws Exception {
    final OPDSAcquisitionFeedEntryParserType parser = this.getParser();
    final OPDSAcquisitionFeedEntry e = parser.parseEntryStream(URI.create("urn:test"),
      OPDSFeedEntryParserContract.getResource(
        "entry-availability-heldready-specific0.xml"));

    final OPDSAvailabilityType availability = e.getAvailability();

    final OptionType<DateTime> expected_end_date = Option.some(
        ISODateTimeFormat.dateTimeParser().parseDateTime("2015-08-24T00:30:24Z"));
    final OptionType<URI> expected_revoke =
        Option.some(new URI("http://example.com/revoke"));
    final OPDSAvailabilityHeldReady expected =
        OPDSAvailabilityHeldReady.get(expected_end_date, expected_revoke);

    Assert.assertEquals(expected, availability);
    Assert.assertEquals(1, e.getAcquisitions().size());

    final OPDSAcquisition acquisition = e.getAcquisitions().get(0);
    Assert.assertTrue(
      "application/epub+zip is available",
      OPDSIndirectAcquisition.Companion.findTypeInOptional(
        mimeOf("application/epub+zip"),
        acquisition.getIndirectAcquisitions()).isSome());
  }

  @Test
  public void testEntryMultipleFormats0()
    throws Exception {
    final OPDSAcquisitionFeedEntryParserType parser = this.getParser();
    final OPDSAcquisitionFeedEntry e = parser.parseEntryStream(URI.create("urn:test"),
      OPDSFeedEntryParserContract.getResource("entry-with-formats-0.xml"));

    Assert.assertEquals(3, e.getAcquisitions().size());

    {
      final OPDSAcquisition acquisition = e.getAcquisitions().get(0);
      Assert.assertTrue(
        "application/epub+zip is available",
        OPDSIndirectAcquisition.Companion.findTypeInOptional(
          mimeOf("application/epub+zip"),
          acquisition.getIndirectAcquisitions()).isSome());
    }

    {
      final OPDSAcquisition acquisition = e.getAcquisitions().get(1);
      Assert.assertTrue(
        "application/pdf is available",
        OPDSIndirectAcquisition.Companion.findTypeInOptional(
          mimeOf("application/pdf"),
          acquisition.getIndirectAcquisitions()).isSome());
    }

    {
      final OPDSAcquisition acquisition = e.getAcquisitions().get(2);
      Assert.assertTrue(
        "text/html is available",
        OPDSIndirectAcquisition.Companion.findTypeInOptional(
          mimeOf("text/html"),
          acquisition.getIndirectAcquisitions()).isSome());
    }
  }

  @Test
  public void testEntryMultipleFormats1()
    throws Exception {
    final OPDSAcquisitionFeedEntryParserType parser = this.getParser();
    final OPDSAcquisitionFeedEntry e = parser.parseEntryStream(URI.create("urn:test"),
      OPDSFeedEntryParserContract.getResource("entry-with-formats-1.xml"));

    Assert.assertEquals(1, e.getAcquisitions().size());

    final OPDSAcquisition acquisition = e.getAcquisitions().get(0);
    Assert.assertTrue(
      "application/epub+zip is available",
      OPDSIndirectAcquisition.Companion.findTypeInOptional(
        mimeOf("application/epub+zip"),
        acquisition.getIndirectAcquisitions()).isSome());
  }

  @Test
  public void testEntryWithDRM()
    throws Exception {
    final OPDSAcquisitionFeedEntryParserType parser = this.getParser();
    final OPDSAcquisitionFeedEntry e = parser.parseEntryStream(URI.create("urn:test"),
      OPDSFeedEntryParserContract.getResource("entry-with-drm.xml"));

    OptionType<DRMLicensor> licensor_opt = e.getLicensor();
    Assert.assertTrue(licensor_opt.isSome());

    DRMLicensor licensor = ((Some<DRMLicensor>) licensor_opt).get();
    Assert.assertEquals(
      "NYPL",
      licensor.getVendor());
    Assert.assertEquals(
      "NYNYPL|0000000000|XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX|XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX",
      licensor.getClientToken());
    Assert.assertEquals(
      Option.some("http://qa.circulation.librarysimplified.org/NYNYPL/AdobeAuth/devices"),
      licensor.getDeviceManager());
  }

  private OPDSAcquisitionFeedEntryParserType getParser() {
    return OPDSAcquisitionFeedEntryParser.newParser();
  }

  private MIMEType mimeOf(String text) {
    try {
      return MIMEParser.Companion.parseRaisingException(text);
    } catch (final Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
