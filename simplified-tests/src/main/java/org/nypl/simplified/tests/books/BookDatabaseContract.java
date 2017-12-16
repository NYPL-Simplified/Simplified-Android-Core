package org.nypl.simplified.tests.books;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.nypl.simplified.books.core.BookDatabase;
import org.nypl.simplified.books.core.BookDatabaseEntryType;
import org.nypl.simplified.books.core.BookDatabaseType;
import org.nypl.simplified.books.core.BookID;
import org.nypl.simplified.opds.core.OPDSAcquisition;
import org.nypl.simplified.opds.core.OPDSAcquisition.Type;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryBuilderType;
import org.nypl.simplified.opds.core.OPDSAvailabilityOpenAccess;
import org.nypl.simplified.opds.core.OPDSJSONParser;
import org.nypl.simplified.opds.core.OPDSJSONParserType;
import org.nypl.simplified.opds.core.OPDSJSONSerializer;
import org.nypl.simplified.opds.core.OPDSJSONSerializerType;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Calendar;

public abstract class BookDatabaseContract {

  @Rule public ExpectedException expected = ExpectedException.none();

  @Test
  public void testBooksDatabaseInit()
      throws Exception {
    final OPDSJSONSerializerType in_json_serializer =
        OPDSJSONSerializer.newSerializer();
    final OPDSJSONParserType in_json_parser = OPDSJSONParser.newParser();
    final File in_directory = File.createTempFile("pre", "");
    in_directory.delete();

    final BookDatabaseType bd = BookDatabase.newDatabase(
        in_json_serializer, in_json_parser, in_directory);

    bd.databaseCreate();
    Assert.assertTrue(in_directory.isDirectory());
    bd.databaseDestroy();
    Assert.assertTrue(bd.databaseGetBooks().isEmpty());
  }

  @Test
  public void testBooksDatabaseEntry()
      throws Exception {
    final OPDSJSONSerializerType in_json_serializer =
        OPDSJSONSerializer.newSerializer();
    final OPDSJSONParserType in_json_parser = OPDSJSONParser.newParser();
    final File in_directory = File.createTempFile("pre", "");
    in_directory.delete();

    final BookDatabaseType bd = BookDatabase.newDatabase(
        in_json_serializer, in_json_parser, in_directory);
    bd.databaseCreate();

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
              Type.ACQUISITION_BORROW, URI.create("http://example.com")));
      ee = eb.build();
    }

    final BookDatabaseEntryType e =
        bd.databaseOpenEntryForWriting(BookID.exactString("abcd"));
    e.entryCreate(ee);

    final OPDSAcquisitionFeedEntry ef = e.entryGetFeedData();
    Assert.assertEquals(ee.getID(), ef.getID());
  }

  @Test
  public void testBooksDatabaseInitFailed()
      throws Exception {
    final OPDSJSONSerializerType in_json_serializer =
        OPDSJSONSerializer.newSerializer();
    final OPDSJSONParserType in_json_parser = OPDSJSONParser.newParser();
    final File in_directory = File.createTempFile("pre", "");

    final BookDatabaseType bd = BookDatabase.newDatabase(
        in_json_serializer, in_json_parser, in_directory);

    expected.expect(IOException.class);
    bd.databaseCreate();
  }
}
