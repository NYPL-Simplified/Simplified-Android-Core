package org.nypl.simplified.tests.books;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.nypl.simplified.books.core.BookDatabase;
import org.nypl.simplified.books.core.BookDatabaseEntryFormat;
import org.nypl.simplified.books.core.BookDatabaseEntryFormatSnapshot;
import org.nypl.simplified.books.core.BookDatabaseEntryType;
import org.nypl.simplified.books.core.BookDatabaseType;
import org.nypl.simplified.books.core.BookID;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.Calendar;
import java.util.Collections;

import static org.nypl.simplified.books.core.BookDatabaseEntryFormat.*;
import static org.nypl.simplified.books.core.BookDatabaseEntryFormat.BookDatabaseEntryFormatEPUB;
import static org.nypl.simplified.books.core.BookDatabaseEntryFormatSnapshot.*;
import static org.nypl.simplified.books.core.BookDatabaseEntryFormatSnapshot.BookDatabaseEntryFormatSnapshotEPUB;

public abstract class BookDatabaseContract {

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
      BookDatabase.Companion.newDatabase(jsonSerializer, jsonParser, directory);

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
      BookDatabase.Companion.newDatabase(jsonSerializer, jsonParser, directory);

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
      BookDatabase.Companion.newDatabase(jsonSerializer, jsonParser, directory);
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
          Option.some("application/epub+zip"),
          Collections.<OPDSIndirectAcquisition>emptyList()));
      ee = eb.build();
    }

    final BookDatabaseEntryType databaseEntry =
      bookDatabase.databaseCreateEntry(BookID.exactString("abcd"), ee);

    final OptionType<BookDatabaseEntryFormatEPUB> formatOpt =
      databaseEntry.entryFindFormat(BookDatabaseEntryFormatEPUB.class);
    Assert.assertTrue("Format is present", formatOpt.isSome());
    Assert.assertTrue(
      "No audio book format",
      databaseEntry.entryFindFormat(BookDatabaseEntryFormatAudioBook.class).isNone());

    final BookDatabaseEntryFormatEPUB format =
      ((Some<BookDatabaseEntryFormatEPUB>) formatOpt).get();

    final BookDatabaseEntryFormatSnapshotEPUB snap = format.snapshot();
    Assert.assertTrue("No Adobe rights", snap.getAdobeRights().isNone());
    Assert.assertTrue("No book data", snap.getBook().isNone());
    Assert.assertFalse("Book is not downloaded", snap.isDownloaded());
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
      BookDatabase.Companion.newDatabase(jsonSerializer, jsonParser, directory);
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
          Option.some("application/audiobook+json"),
          Collections.<OPDSIndirectAcquisition>emptyList()));
      ee = eb.build();
    }

    final BookDatabaseEntryType databaseEntry =
      bookDatabase.databaseCreateEntry(BookID.exactString("abcd"), ee);

    final OptionType<BookDatabaseEntryFormatAudioBook> formatOpt =
      databaseEntry.entryFindFormat(BookDatabaseEntryFormatAudioBook.class);
    Assert.assertTrue("Format is present", formatOpt.isSome());
    Assert.assertTrue(
      "No EPUB format",
      databaseEntry.entryFindFormat(BookDatabaseEntryFormatEPUB.class).isNone());

    final BookDatabaseEntryFormatAudioBook format =
      ((Some<BookDatabaseEntryFormatAudioBook>) formatOpt).get();

    final BookDatabaseEntryFormatSnapshotAudioBook snap = format.snapshot();
    Assert.assertTrue(snap.isDownloaded());
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
      BookDatabase.Companion.newDatabase(jsonSerializer, jsonParser, directory);
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
          Option.some("application/epub+zip"),
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
      BookDatabase.Companion.newDatabase(jsonSerializer, jsonParser, directory);
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
          Option.some("application/epub+zip"),
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
      BookDatabase.Companion.newDatabase(jsonSerializer, jsonParser, directory);

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
      BookDatabase.Companion.newDatabase(jsonSerializer, jsonParser, directory);
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
          Option.some("application/epub+zip"),
          Collections.<OPDSIndirectAcquisition>emptyList()));
      ee = eb.build();
    }

    final BookDatabaseEntryType databaseEntry =
      bookDatabase.databaseCreateEntry(BookID.exactString("abcd"), ee);

    final OptionType<BookDatabaseEntryFormatEPUB> formatOpt =
      databaseEntry.entryFindFormat(BookDatabaseEntryFormatEPUB.class);
    Assert.assertTrue("Format is present", formatOpt.isSome());

    final BookDatabaseEntryFormatEPUB format =
      ((Some<BookDatabaseEntryFormatEPUB>) formatOpt).get();

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
