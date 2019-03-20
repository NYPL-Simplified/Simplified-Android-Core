package org.nypl.simplified.tests.books;

import android.content.Context;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.nypl.simplified.books.core.BookDatabase;
import org.nypl.simplified.books.core.BookDatabaseEntryType;
import org.nypl.simplified.books.core.BookDatabaseType;
import org.nypl.simplified.books.core.BookID;
import org.nypl.simplified.mime.MIMEParser;
import org.nypl.simplified.opds.core.OPDSAcquisition;
import org.nypl.simplified.opds.core.OPDSAcquisition.Relation;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryBuilderType;
import org.nypl.simplified.opds.core.OPDSAvailabilityOpenAccess;
import org.nypl.simplified.opds.core.OPDSIndirectAcquisition;
import org.nypl.simplified.opds.core.OPDSJSONParser;
import org.nypl.simplified.opds.core.OPDSJSONParserType;
import org.nypl.simplified.opds.core.OPDSJSONSerializer;
import org.nypl.simplified.opds.core.OPDSJSONSerializerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Calendar;
import java.util.Collections;

import static org.nypl.simplified.books.core.BookDatabaseEntryFormatHandle.*;
import static org.nypl.simplified.books.core.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleEPUB;
import static org.nypl.simplified.books.core.BookDatabaseEntryFormatSnapshot.*;
import static org.nypl.simplified.books.core.BookDatabaseEntryFormatSnapshot.BookDatabaseEntryFormatSnapshotEPUB;

public abstract class BookDatabaseContract {

  private static final Logger LOG = LoggerFactory.getLogger(BookDatabaseContract.class);

  protected abstract Context context();

  @Rule
  public ExpectedException expected = ExpectedException.none();

  /**
   * A database starts out empty.
   *
   * @throws Exception On errors
   */

  @Test
  public void testBooksDatabaseInit() throws Exception {

    final OPDSJSONSerializerType jsonSerializer = OPDSJSONSerializer.newSerializer();
    final OPDSJSONParserType jsonParser = OPDSJSONParser.newParser();
    final File directory = File.createTempFile("pre", "");
    directory.delete();

    final BookDatabaseType bookDatabase =
      BookDatabase.Companion.newDatabase(this.context(), jsonSerializer, jsonParser, directory);

    bookDatabase.databaseCreate();
    Assert.assertTrue(directory.isDirectory());
    bookDatabase.databaseDestroy();
    Assert.assertTrue(bookDatabase.databaseGetBooks().isEmpty());
  }

  /**
   * Trying to open an entry that hasn't been created fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testBooksDatabaseOpenNonexistent() throws Exception {

    final OPDSJSONSerializerType jsonSerializer = OPDSJSONSerializer.newSerializer();
    final OPDSJSONParserType jsonParser = OPDSJSONParser.newParser();
    final File directory = File.createTempFile("pre", "");
    directory.delete();

    final BookDatabaseType bookDatabase =
      BookDatabase.Companion.newDatabase(this.context(), jsonSerializer, jsonParser, directory);

    bookDatabase.databaseCreate();
    Assert.assertTrue(directory.isDirectory());
    bookDatabase.databaseDestroy();

    this.expected.expect(FileNotFoundException.class);
    bookDatabase.databaseOpenExistingEntry(BookID.exactString("abcd"));
  }

  /**
   * Creating a book database entry for a feed that contains an EPUB acquisition results in an
   * EPUB format.
   *
   * @throws Exception On errors
   */

  @Test
  public void testBooksDatabaseEntryHasEPUBFormat() throws Exception {

    final OPDSJSONSerializerType jsonSerializer = OPDSJSONSerializer.newSerializer();
    final OPDSJSONParserType jsonParser = OPDSJSONParser.newParser();
    final File directory = File.createTempFile("pre", "");
    directory.delete();

    final BookDatabaseType bookDatabase =
      BookDatabase.Companion.newDatabase(this.context(), jsonSerializer, jsonParser, directory);
    bookDatabase.databaseCreate();

    final OPDSAcquisitionFeedEntry ee;
    {
      final OptionType<URI> revoke = Option.none();
      final OPDSAcquisitionFeedEntryBuilderType eb =
        OPDSAcquisitionFeedEntry.newBuilder(
          "abcd",
          "Title",
          Calendar.getInstance(),
          OPDSAvailabilityOpenAccess.get(revoke));
      eb.addAcquisition(
        new OPDSAcquisition(
          Relation.ACQUISITION_BORROW,
          URI.create("http://example.com"),
          Option.some(MIMEParser.Companion.parseRaisingException("application/epub+zip")),
          Collections.<OPDSIndirectAcquisition>emptyList()));
      ee = eb.build();
    }

    final BookDatabaseEntryType databaseEntry =
      bookDatabase.databaseCreateEntry(BookID.exactString("abcd"), ee);

    final OptionType<BookDatabaseEntryFormatHandleEPUB> formatOpt =
      databaseEntry.entryFindFormatHandle(BookDatabaseEntryFormatHandleEPUB.class);
    Assert.assertTrue("Format is present", formatOpt.isSome());
    Assert.assertTrue(
      "No audio book format",
      databaseEntry.entryFindFormatHandle(BookDatabaseEntryFormatHandleAudioBook.class).isNone());

    final BookDatabaseEntryFormatHandleEPUB format =
      ((Some<BookDatabaseEntryFormatHandleEPUB>) formatOpt).get();

    final BookDatabaseEntryFormatSnapshotEPUB snap = format.snapshot();
    Assert.assertTrue("No Adobe rights", snap.getAdobeRights().isNone());
    Assert.assertTrue("No book data", snap.getBook().isNone());
    Assert.assertFalse("Book is not downloaded", snap.isDownloaded());

    Assert.assertEquals(
      formatOpt,
      databaseEntry.entryFindFormatHandleForContentType("application/epub+zip"));
    Assert.assertEquals(
      Option.none(),
      databaseEntry.entryFindFormatHandleForContentType("application/not-a-supported-format"));
  }

  /**
   * Creating a book database entry for a feed that contains an EPUB acquisition results in an
   * EPUB format.
   *
   * @throws Exception On errors
   */

  @Test
  public void testBooksDatabaseEntryHasEPUBFormatOpenNext() throws Exception {

    final OPDSJSONSerializerType jsonSerializer = OPDSJSONSerializer.newSerializer();
    final OPDSJSONParserType jsonParser = OPDSJSONParser.newParser();
    final File directory = File.createTempFile("pre", "");
    directory.delete();

    final BookDatabaseType bookDatabase =
      BookDatabase.Companion.newDatabase(this.context(), jsonSerializer, jsonParser, directory);
    bookDatabase.databaseCreate();

    final OPDSAcquisitionFeedEntry ee;
    {
      final OptionType<URI> revoke = Option.none();
      final OPDSAcquisitionFeedEntryBuilderType eb =
        OPDSAcquisitionFeedEntry.newBuilder(
          "abcd",
          "Title",
          Calendar.getInstance(),
          OPDSAvailabilityOpenAccess.get(revoke));
      eb.addAcquisition(
        new OPDSAcquisition(
          Relation.ACQUISITION_BORROW,
          URI.create("http://example.com"),
          Option.some(MIMEParser.Companion.parseRaisingException("application/epub+zip")),
          Collections.<OPDSIndirectAcquisition>emptyList()));
      ee = eb.build();
    }

    final BookDatabaseEntryType databaseEntry0 =
      bookDatabase.databaseCreateEntry(BookID.exactString("abcd"), ee);

    final OptionType<BookDatabaseEntryFormatHandleEPUB> formatOpt0 =
      databaseEntry0.entryFindFormatHandle(BookDatabaseEntryFormatHandleEPUB.class);
    final BookDatabaseEntryFormatHandleEPUB format0 =
      ((Some<BookDatabaseEntryFormatHandleEPUB>) formatOpt0).get();
    final BookDatabaseEntryFormatSnapshotEPUB snap0 = format0.snapshot();

    final BookDatabaseEntryType databaseEntry1 =
      bookDatabase.databaseOpenExistingEntry(BookID.exactString("abcd"));

    final OptionType<BookDatabaseEntryFormatHandleEPUB> formatOpt1 =
      databaseEntry1.entryFindFormatHandle(BookDatabaseEntryFormatHandleEPUB.class);
    final BookDatabaseEntryFormatHandleEPUB format1 =
      ((Some<BookDatabaseEntryFormatHandleEPUB>) formatOpt1).get();
    final BookDatabaseEntryFormatSnapshotEPUB snap1 = format1.snapshot();

    Assert.assertEquals(snap0, snap1);
  }

  /**
   * Creating a book database entry for a feed that contains an audio book acquisition results in an
   * audio book format.
   *
   * @throws Exception On errors
   */

  @Test
  public void testBooksDatabaseEntryHasAudioBookFormat() throws Exception {

    final OPDSJSONSerializerType jsonSerializer = OPDSJSONSerializer.newSerializer();
    final OPDSJSONParserType jsonParser = OPDSJSONParser.newParser();
    final File directory = File.createTempFile("pre", "");
    directory.delete();

    final BookDatabaseType bookDatabase =
      BookDatabase.Companion.newDatabase(this.context(), jsonSerializer, jsonParser, directory);
    bookDatabase.databaseCreate();

    final OPDSAcquisitionFeedEntry ee;
    {
      final OptionType<URI> revoke = Option.none();
      final OPDSAcquisitionFeedEntryBuilderType eb =
        OPDSAcquisitionFeedEntry.newBuilder(
          "abcd",
          "Title",
          Calendar.getInstance(),
          OPDSAvailabilityOpenAccess.get(revoke));
      eb.addAcquisition(
        new OPDSAcquisition(
          Relation.ACQUISITION_BORROW,
          URI.create("http://example.com"),
          Option.some(MIMEParser.Companion.parseRaisingException("application/audiobook+json")),
          Collections.<OPDSIndirectAcquisition>emptyList()));
      ee = eb.build();
    }

    final BookDatabaseEntryType databaseEntry =
      bookDatabase.databaseCreateEntry(BookID.exactString("abcd"), ee);

    final OptionType<BookDatabaseEntryFormatHandleAudioBook> formatOpt =
      databaseEntry.entryFindFormatHandle(BookDatabaseEntryFormatHandleAudioBook.class);
    Assert.assertTrue("Format is present", formatOpt.isSome());
    Assert.assertTrue(
      "No EPUB format",
      databaseEntry.entryFindFormatHandle(BookDatabaseEntryFormatHandleEPUB.class).isNone());

    final BookDatabaseEntryFormatHandleAudioBook format =
      ((Some<BookDatabaseEntryFormatHandleAudioBook>) formatOpt).get();

    Assert.assertEquals(
      formatOpt,
      databaseEntry.entryFindFormatHandleForContentType("application/audiobook+json"));
    Assert.assertEquals(
      Option.none(),
      databaseEntry.entryFindFormatHandleForContentType("application/not-a-supported-format"));
  }

  /**
   * Creating a book database entry with an audio book format, and copying in a book and then deleting the local
   * book data repeatedly, works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testBooksDatabaseEntryAudioBookCopyDeleteRepeatedly() throws Exception {

    final OPDSJSONSerializerType jsonSerializer = OPDSJSONSerializer.newSerializer();
    final OPDSJSONParserType jsonParser = OPDSJSONParser.newParser();
    final File directory = File.createTempFile("pre", "");
    directory.delete();

    final BookDatabaseType bookDatabase =
      BookDatabase.Companion.newDatabase(this.context(), jsonSerializer, jsonParser, directory);
    bookDatabase.databaseCreate();

    final OPDSAcquisitionFeedEntry ee;
    {
      final OptionType<URI> revoke = Option.none();
      final OPDSAcquisitionFeedEntryBuilderType eb =
        OPDSAcquisitionFeedEntry.newBuilder(
          "abcd",
          "Title",
          Calendar.getInstance(),
          OPDSAvailabilityOpenAccess.get(revoke));
      eb.addAcquisition(
        new OPDSAcquisition(
          Relation.ACQUISITION_BORROW,
          URI.create("http://example.com"),
          Option.some(MIMEParser.Companion.parseRaisingException("application/audiobook+json")),
          Collections.<OPDSIndirectAcquisition>emptyList()));
      ee = eb.build();
    }

    final BookDatabaseEntryType databaseEntry =
      bookDatabase.databaseCreateEntry(BookID.exactString("abcd"), ee);

    for (int index = 0; index < 3; ++index) {
      final OptionType<BookDatabaseEntryFormatHandleAudioBook> formatOpt =
        databaseEntry.entryFindFormatHandle(BookDatabaseEntryFormatHandleAudioBook.class);
      Assert.assertTrue("Format is present", formatOpt.isSome());

      final BookDatabaseEntryFormatHandleAudioBook format =
        ((Some<BookDatabaseEntryFormatHandleAudioBook>) formatOpt).get();

      final File file = copyToTempFile("/org/nypl/simplified/tests/books/basic-manifest.json");
      format.copyInManifestAndURI(file, URI.create("urn:invalid"));

      databaseEntry.entryDeleteBookData();
    }
  }

  /**
   * Creating a book database entry with an audio book format, and copying in a book and then destroying the entry,
   * works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testBooksDatabaseEntryAudioBookCopyDestroyEntry() throws Exception {

    final OPDSJSONSerializerType jsonSerializer = OPDSJSONSerializer.newSerializer();
    final OPDSJSONParserType jsonParser = OPDSJSONParser.newParser();
    final File directory = File.createTempFile("pre", "");
    directory.delete();

    final BookDatabaseType bookDatabase =
      BookDatabase.Companion.newDatabase(this.context(), jsonSerializer, jsonParser, directory);
    bookDatabase.databaseCreate();

    final OPDSAcquisitionFeedEntry ee;
    {
      final OptionType<URI> revoke = Option.none();
      final OPDSAcquisitionFeedEntryBuilderType eb =
        OPDSAcquisitionFeedEntry.newBuilder(
          "abcd",
          "Title",
          Calendar.getInstance(),
          OPDSAvailabilityOpenAccess.get(revoke));
      eb.addAcquisition(
        new OPDSAcquisition(
          Relation.ACQUISITION_BORROW,
          URI.create("http://example.com"),
          Option.some(MIMEParser.Companion.parseRaisingException("application/audiobook+json")),
          Collections.<OPDSIndirectAcquisition>emptyList()));
      ee = eb.build();
    }

    final BookDatabaseEntryType databaseEntry =
      bookDatabase.databaseCreateEntry(BookID.exactString("abcd"), ee);

    final OptionType<BookDatabaseEntryFormatHandleAudioBook> formatOpt =
      databaseEntry.entryFindFormatHandle(BookDatabaseEntryFormatHandleAudioBook.class);
    Assert.assertTrue("Format is present", formatOpt.isSome());

    final BookDatabaseEntryFormatHandleAudioBook format =
      ((Some<BookDatabaseEntryFormatHandleAudioBook>) formatOpt).get();

    final File file = copyToTempFile("/org/nypl/simplified/tests/books/basic-manifest.json");
    format.copyInManifestAndURI(file, URI.create("urn:invalid"));

    databaseEntry.entryDestroy();
  }

  /**
   * Creating a book database entry with an EPUB format, and copying in a book and then deleting the local book data
   * repeatedly, works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testBooksDatabaseEntryEPUBCopyDeleteRepeatedly() throws Exception {

    final OPDSJSONSerializerType jsonSerializer = OPDSJSONSerializer.newSerializer();
    final OPDSJSONParserType jsonParser = OPDSJSONParser.newParser();
    final File directory = File.createTempFile("pre", "");
    directory.delete();

    final BookDatabaseType bookDatabase =
      BookDatabase.Companion.newDatabase(this.context(), jsonSerializer, jsonParser, directory);
    bookDatabase.databaseCreate();

    final OPDSAcquisitionFeedEntry ee;
    {
      final OptionType<URI> revoke = Option.none();
      final OPDSAcquisitionFeedEntryBuilderType eb =
        OPDSAcquisitionFeedEntry.newBuilder(
          "abcd",
          "Title",
          Calendar.getInstance(),
          OPDSAvailabilityOpenAccess.get(revoke));
      eb.addAcquisition(
        new OPDSAcquisition(
          Relation.ACQUISITION_BORROW,
          URI.create("http://example.com"),
          Option.some(MIMEParser.Companion.parseRaisingException("application/epub+zip")),
          Collections.<OPDSIndirectAcquisition>emptyList()));
      ee = eb.build();
    }

    final BookDatabaseEntryType databaseEntry =
      bookDatabase.databaseCreateEntry(BookID.exactString("abcd"), ee);

    for (int index = 0; index < 3; ++index) {
      final OptionType<BookDatabaseEntryFormatHandleEPUB> formatOpt =
        databaseEntry.entryFindFormatHandle(BookDatabaseEntryFormatHandleEPUB.class);
      Assert.assertTrue("Format is present", formatOpt.isSome());

      final BookDatabaseEntryFormatHandleEPUB format =
        ((Some<BookDatabaseEntryFormatHandleEPUB>) formatOpt).get();

      {
        final BookDatabaseEntryFormatSnapshotEPUB snap = format.snapshot();
        Assert.assertTrue("No Adobe rights", snap.getAdobeRights().isNone());
        Assert.assertTrue("No book data", snap.getBook().isNone());
        Assert.assertFalse("Book is not downloaded", snap.isDownloaded());
      }

      format.copyInBook(copyToTempFile("/org/nypl/simplified/tests/books/empty.epub"));

      {
        final BookDatabaseEntryFormatSnapshotEPUB snap = format.snapshot();
        Assert.assertTrue("No Adobe rights", snap.getAdobeRights().isNone());
        Assert.assertTrue("Book data", snap.getBook().isSome());
        Assert.assertTrue("Book is downloaded", snap.isDownloaded());
      }

      databaseEntry.entryDeleteBookData();
    }
  }

  /**
   * Creating a book database entry with an EPUB format, and copying in a book and then destroying the entry, works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testBooksDatabaseEntryEPUBCopyDestroy() throws Exception {

    final OPDSJSONSerializerType jsonSerializer = OPDSJSONSerializer.newSerializer();
    final OPDSJSONParserType jsonParser = OPDSJSONParser.newParser();
    final File directory = File.createTempFile("pre", "");
    directory.delete();

    final BookDatabaseType bookDatabase =
      BookDatabase.Companion.newDatabase(this.context(), jsonSerializer, jsonParser, directory);
    bookDatabase.databaseCreate();

    final OPDSAcquisitionFeedEntry ee;
    {
      final OptionType<URI> revoke = Option.none();
      final OPDSAcquisitionFeedEntryBuilderType eb =
        OPDSAcquisitionFeedEntry.newBuilder(
          "abcd",
          "Title",
          Calendar.getInstance(),
          OPDSAvailabilityOpenAccess.get(revoke));
      eb.addAcquisition(
        new OPDSAcquisition(
          Relation.ACQUISITION_BORROW,
          URI.create("http://example.com"),
          Option.some(MIMEParser.Companion.parseRaisingException("application/epub+zip")),
          Collections.<OPDSIndirectAcquisition>emptyList()));
      ee = eb.build();
    }

    final BookDatabaseEntryType databaseEntry =
      bookDatabase.databaseCreateEntry(BookID.exactString("abcd"), ee);

    final OptionType<BookDatabaseEntryFormatHandleEPUB> formatOpt =
      databaseEntry.entryFindFormatHandle(BookDatabaseEntryFormatHandleEPUB.class);
    Assert.assertTrue("Format is present", formatOpt.isSome());

    final BookDatabaseEntryFormatHandleEPUB format =
      ((Some<BookDatabaseEntryFormatHandleEPUB>) formatOpt).get();

    {
      final BookDatabaseEntryFormatSnapshotEPUB snap = format.snapshot();
      Assert.assertTrue("No Adobe rights", snap.getAdobeRights().isNone());
      Assert.assertTrue("No book data", snap.getBook().isNone());
      Assert.assertFalse("Book is not downloaded", snap.isDownloaded());
    }

    format.copyInBook(copyToTempFile("/org/nypl/simplified/tests/books/empty.epub"));

    {
      final BookDatabaseEntryFormatSnapshotEPUB snap = format.snapshot();
      Assert.assertTrue("No Adobe rights", snap.getAdobeRights().isNone());
      Assert.assertTrue("Book data", snap.getBook().isSome());
      Assert.assertTrue("Book is downloaded", snap.isDownloaded());
    }

    databaseEntry.entryDestroy();
  }

  private static File copyToTempFile(
    final String name)
    throws IOException {
    final File file = File.createTempFile("simplified-book-database-", ".bin");
    LOG.debug("copyToTempFile: {} -> {}", name, file);
    try (final FileOutputStream output = new FileOutputStream(file)) {
      try (final InputStream input = BookDatabaseContract.class.getResourceAsStream(name)) {
        final byte[] buffer = new byte[4096];
        while (true) {
          final int r = input.read(buffer);
          if (r == -1) {
            break;
          }
          output.write(buffer, 0, r);
        }
        return file;
      }
    }
  }

  /**
   * Creating a book database entry for a feed that contains an audio book acquisition results in an
   * audio book format.
   *
   * @throws Exception On errors
   */

  @Test
  public void testBooksDatabaseEntryHasAudioBookFormatOpenNext() throws Exception {

    final OPDSJSONSerializerType jsonSerializer = OPDSJSONSerializer.newSerializer();
    final OPDSJSONParserType jsonParser = OPDSJSONParser.newParser();
    final File directory = File.createTempFile("pre", "");
    directory.delete();

    final BookDatabaseType bookDatabase =
      BookDatabase.Companion.newDatabase(this.context(), jsonSerializer, jsonParser, directory);
    bookDatabase.databaseCreate();

    final OPDSAcquisitionFeedEntry ee;
    {
      final OptionType<URI> revoke = Option.none();
      final OPDSAcquisitionFeedEntryBuilderType eb =
        OPDSAcquisitionFeedEntry.newBuilder(
          "abcd",
          "Title",
          Calendar.getInstance(),
          OPDSAvailabilityOpenAccess.get(revoke));
      eb.addAcquisition(
        new OPDSAcquisition(
          Relation.ACQUISITION_BORROW,
          URI.create("http://example.com"),
          Option.some(MIMEParser.Companion.parseRaisingException("application/audiobook+json")),
          Collections.<OPDSIndirectAcquisition>emptyList()));
      ee = eb.build();
    }

    final BookDatabaseEntryType databaseEntry0 =
      bookDatabase.databaseCreateEntry(BookID.exactString("abcd"), ee);

    final OptionType<BookDatabaseEntryFormatHandleAudioBook> formatOpt0 =
      databaseEntry0.entryFindFormatHandle(BookDatabaseEntryFormatHandleAudioBook.class);
    final BookDatabaseEntryFormatHandleAudioBook format0 =
      ((Some<BookDatabaseEntryFormatHandleAudioBook>) formatOpt0).get();
    final BookDatabaseEntryFormatSnapshotAudioBook snap0 = format0.snapshot();

    final BookDatabaseEntryType databaseEntry1 =
      bookDatabase.databaseOpenExistingEntry(BookID.exactString("abcd"));

    final OptionType<BookDatabaseEntryFormatHandleAudioBook> formatOpt1 =
      databaseEntry1.entryFindFormatHandle(BookDatabaseEntryFormatHandleAudioBook.class);
    final BookDatabaseEntryFormatHandleAudioBook format1 =
      ((Some<BookDatabaseEntryFormatHandleAudioBook>) formatOpt1).get();
    final BookDatabaseEntryFormatSnapshotAudioBook snap1 = format1.snapshot();

    Assert.assertEquals(snap0, snap1);
  }

  /**
   * Creating a database entry yields the correct serialized OPDS entry.
   *
   * @throws Exception On errors
   */

  @Test
  public void testBooksDatabaseEntry() throws Exception {

    final OPDSJSONSerializerType jsonSerializer = OPDSJSONSerializer.newSerializer();
    final OPDSJSONParserType jsonParser = OPDSJSONParser.newParser();
    final File directory = File.createTempFile("pre", "");
    directory.delete();

    final BookDatabaseType bookDatabase =
      BookDatabase.Companion.newDatabase(this.context(), jsonSerializer, jsonParser, directory);
    bookDatabase.databaseCreate();

    final OPDSAcquisitionFeedEntry ee;
    {
      final OptionType<URI> revoke = Option.none();
      final OPDSAcquisitionFeedEntryBuilderType eb =
        OPDSAcquisitionFeedEntry.newBuilder(
          "abcd",
          "Title",
          Calendar.getInstance(),
          OPDSAvailabilityOpenAccess.get(revoke));
      eb.addAcquisition(
        new OPDSAcquisition(
          Relation.ACQUISITION_BORROW,
          URI.create("http://example.com"),
          Option.some(MIMEParser.Companion.parseRaisingException("application/epub+zip")),
          Collections.<OPDSIndirectAcquisition>emptyList()));
      ee = eb.build();
    }

    final BookDatabaseEntryType e =
      bookDatabase.databaseCreateEntry(BookID.exactString("abcd"), ee);
    Assert.assertTrue("Entry exists", e.entryExists());

    final OPDSAcquisitionFeedEntry ef = e.entryGetFeedData();
    Assert.assertEquals(ee.getID(), ef.getID());
  }

  /**
   * Creating a database entry yields the correct serialized OPDS entry.
   *
   * @throws Exception On errors
   */

  @Test
  public void testBooksDatabaseEntryCover() throws Exception {

    final OPDSJSONSerializerType jsonSerializer = OPDSJSONSerializer.newSerializer();
    final OPDSJSONParserType jsonParser = OPDSJSONParser.newParser();
    final File directory = File.createTempFile("pre", "");
    directory.delete();

    final BookDatabaseType bookDatabase =
      BookDatabase.Companion.newDatabase(this.context(), jsonSerializer, jsonParser, directory);
    bookDatabase.databaseCreate();

    final OPDSAcquisitionFeedEntry ee;
    {
      final OptionType<URI> revoke = Option.none();
      final OPDSAcquisitionFeedEntryBuilderType eb =
        OPDSAcquisitionFeedEntry.newBuilder(
          "abcd",
          "Title",
          Calendar.getInstance(),
          OPDSAvailabilityOpenAccess.get(revoke));
      eb.addAcquisition(
        new OPDSAcquisition(
          Relation.ACQUISITION_BORROW,
          URI.create("http://example.com"),
          Option.some(MIMEParser.Companion.parseRaisingException("application/epub+zip")),
          Collections.<OPDSIndirectAcquisition>emptyList()));
      ee = eb.build();
    }

    final File cover = File.createTempFile("pre", "");

    final BookDatabaseEntryType e =
      bookDatabase.databaseCreateEntry(BookID.exactString("abcd"), ee);

    final File expected = new File(new File(directory, "abcd"), "cover.jpg");
    Assert.assertFalse("Cover does not exist", expected.isFile());

    e.entrySetCover(Option.of(cover));

    Assert.assertTrue("Cover exists", expected.isFile());
  }

  /**
   * Trying to initialize a database to an invalid path fails.
   *
   * @throws Exception On errors
   */

  @Test
  public void testBooksDatabaseInitFailed() throws Exception {

    final OPDSJSONSerializerType jsonSerializer = OPDSJSONSerializer.newSerializer();
    final OPDSJSONParserType jsonParser = OPDSJSONParser.newParser();
    final File directory = File.createTempFile("pre", "");

    final BookDatabaseType bookDatabase =
      BookDatabase.Companion.newDatabase(this.context(), jsonSerializer, jsonParser, directory);

    this.expected.expect(IOException.class);
    bookDatabase.databaseCreate();
  }

  /**
   * Copying in an EPUB works. Deleting it again works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testBooksDatabaseEntryEPUBCopyInDelete() throws Exception {

    final OPDSJSONSerializerType jsonSerializer = OPDSJSONSerializer.newSerializer();
    final OPDSJSONParserType jsonParser = OPDSJSONParser.newParser();
    final File directory = File.createTempFile("pre", "");
    directory.delete();

    final BookDatabaseType bookDatabase =
      BookDatabase.Companion.newDatabase(this.context(), jsonSerializer, jsonParser, directory);
    bookDatabase.databaseCreate();

    final OPDSAcquisitionFeedEntry ee;
    {
      final OptionType<URI> revoke = Option.none();
      final OPDSAcquisitionFeedEntryBuilderType eb =
        OPDSAcquisitionFeedEntry.newBuilder(
          "abcd",
          "Title",
          Calendar.getInstance(),
          OPDSAvailabilityOpenAccess.get(revoke));
      eb.addAcquisition(
        new OPDSAcquisition(
          Relation.ACQUISITION_BORROW,
          URI.create("http://example.com"),
          Option.some(MIMEParser.Companion.parseRaisingException("application/epub+zip")),
          Collections.<OPDSIndirectAcquisition>emptyList()));
      ee = eb.build();
    }

    final BookDatabaseEntryType databaseEntry =
      bookDatabase.databaseCreateEntry(BookID.exactString("abcd"), ee);

    final OptionType<BookDatabaseEntryFormatHandleEPUB> formatOpt =
      databaseEntry.entryFindFormatHandle(BookDatabaseEntryFormatHandleEPUB.class);
    Assert.assertTrue("Format is present", formatOpt.isSome());

    final BookDatabaseEntryFormatHandleEPUB format =
      ((Some<BookDatabaseEntryFormatHandleEPUB>) formatOpt).get();

    final File epub = File.createTempFile("pre", "").getAbsoluteFile();
    format.copyInBook(epub);

    final File expected = new File(new File(directory, "abcd"), "book.epub");
    Assert.assertTrue("EPUB exists", expected.isFile());

    {
      final BookDatabaseEntryFormatSnapshotEPUB snap = format.snapshot();
      Assert.assertTrue("No Adobe rights", snap.getAdobeRights().isNone());
      Assert.assertEquals(Option.some(expected), snap.getBook());
      Assert.assertTrue("Book is downloaded", snap.isDownloaded());
    }

    format.deleteBookData();

    Assert.assertFalse("EPUB does not exist", expected.isFile());

    {
      final BookDatabaseEntryFormatSnapshotEPUB snap = format.snapshot();
      Assert.assertTrue("No Adobe rights", snap.getAdobeRights().isNone());
      Assert.assertEquals(Option.none(), snap.getBook());
      Assert.assertFalse("Book not downloaded", snap.isDownloaded());
    }
  }
}
