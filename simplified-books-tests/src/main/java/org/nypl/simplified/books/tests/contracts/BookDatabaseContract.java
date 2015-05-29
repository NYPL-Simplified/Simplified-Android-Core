package org.nypl.simplified.books.tests.contracts;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Calendar;

import org.nypl.simplified.books.core.BookDatabase;
import org.nypl.simplified.books.core.BookDatabaseEntryType;
import org.nypl.simplified.books.core.BookDatabaseType;
import org.nypl.simplified.books.core.BookID;
import org.nypl.simplified.opds.core.OPDSAcquisition;
import org.nypl.simplified.opds.core.OPDSAcquisition.Type;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryBuilderType;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParser;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParserType;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntrySerializer;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntrySerializerType;
import org.nypl.simplified.test.utilities.TestUtilities;

import com.io7m.jfunctional.PartialProcedureType;
import com.io7m.jfunctional.Unit;

@SuppressWarnings({
  "boxing",
  "synthetic-access",
  "null",
  "resource",
  "static-method" }) public final class BookDatabaseContract implements
  BookDatabaseContractType
{
  @Override public void testBooksDatabaseInit()
    throws Exception
  {
    final OPDSAcquisitionFeedEntryParserType in_parser =
      OPDSAcquisitionFeedEntryParser.newParser();
    final OPDSAcquisitionFeedEntrySerializerType in_serializer =
      OPDSAcquisitionFeedEntrySerializer.newSerializer();
    final File in_directory = File.createTempFile("pre", "");
    in_directory.delete();

    final BookDatabaseType bd =
      BookDatabase.newDatabase(in_parser, in_serializer, in_directory);

    bd.create();
    TestUtilities.assertTrue(in_directory.isDirectory());
    bd.destroy();
    TestUtilities.assertTrue(bd.getBookDatabaseEntries().isEmpty());
  }

  @Override public void testBooksDatabaseEntry()
    throws Exception
  {
    final OPDSAcquisitionFeedEntryParserType in_parser =
      OPDSAcquisitionFeedEntryParser.newParser();
    final OPDSAcquisitionFeedEntrySerializerType in_serializer =
      OPDSAcquisitionFeedEntrySerializer.newSerializer();
    final File in_directory = File.createTempFile("pre", "");
    in_directory.delete();

    final BookDatabaseType bd =
      BookDatabase.newDatabase(in_parser, in_serializer, in_directory);
    bd.create();

    final OPDSAcquisitionFeedEntry ee;
    {
      final OPDSAcquisitionFeedEntryBuilderType eb =
        OPDSAcquisitionFeedEntry.newBuilder(
          "abcd",
          "Title",
          Calendar.getInstance());
      eb.addAcquisition(new OPDSAcquisition(Type.ACQUISITION_BORROW, URI
        .create("http://example.com")));
      ee = eb.build();
    }

    final BookDatabaseEntryType e =
      bd.getBookDatabaseEntry(BookID.exactString("abcd"));
    e.create();
    e.setData(ee);

    final OPDSAcquisitionFeedEntry ef = e.getData();
    TestUtilities.assertEquals(ee.getID(), ef.getID());
  }

  @Override public void testBooksDatabaseInitFailed()
    throws Exception
  {
    final OPDSAcquisitionFeedEntryParserType in_parser =
      OPDSAcquisitionFeedEntryParser.newParser();
    final OPDSAcquisitionFeedEntrySerializerType in_serializer =
      OPDSAcquisitionFeedEntrySerializer.newSerializer();
    final File in_directory = File.createTempFile("pre", "");

    final BookDatabaseType bd =
      BookDatabase.newDatabase(in_parser, in_serializer, in_directory);

    TestUtilities.expectException(
      IOException.class,
      new PartialProcedureType<Unit, Exception>() {
        @Override public void call(
          final Unit x)
          throws Exception
        {
          bd.create();
        }
      });
  }
}
