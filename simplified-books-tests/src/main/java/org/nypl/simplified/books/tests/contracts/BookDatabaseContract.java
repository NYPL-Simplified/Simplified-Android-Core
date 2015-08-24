package org.nypl.simplified.books.tests.contracts;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.PartialProcedureType;
import com.io7m.jfunctional.Unit;
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
import org.nypl.simplified.test.utilities.TestUtilities;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Calendar;

/**
 * Default implementation of {@link BookDatabaseContractType}.
 */

public final class BookDatabaseContract implements BookDatabaseContractType
{
  /**
   * Construct a contract.
   */

  public BookDatabaseContract()
  {

  }

  @Override public void testBooksDatabaseInit()
    throws Exception
  {
    final OPDSJSONSerializerType in_json_serializer =
      OPDSJSONSerializer.newSerializer();
    final OPDSJSONParserType in_json_parser = OPDSJSONParser.newParser();
    final File in_directory = File.createTempFile("pre", "");
    in_directory.delete();

    final BookDatabaseType bd = BookDatabase.newDatabase(
      in_json_serializer, in_json_parser, in_directory);

    bd.create();
    TestUtilities.assertTrue(in_directory.isDirectory());
    bd.destroy();
    TestUtilities.assertTrue(bd.getBookDatabaseEntries().isEmpty());
  }

  @Override public void testBooksDatabaseEntry()
    throws Exception
  {
    final OPDSJSONSerializerType in_json_serializer =
      OPDSJSONSerializer.newSerializer();
    final OPDSJSONParserType in_json_parser = OPDSJSONParser.newParser();
    final File in_directory = File.createTempFile("pre", "");
    in_directory.delete();

    final BookDatabaseType bd = BookDatabase.newDatabase(
      in_json_serializer, in_json_parser, in_directory);
    bd.create();

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
      bd.getBookDatabaseEntry(BookID.exactString("abcd"));
    e.create();
    e.setData(ee);

    final OPDSAcquisitionFeedEntry ef = e.getData();
    TestUtilities.assertEquals(ee.getID(), ef.getID());
  }

  @Override public void testBooksDatabaseInitFailed()
    throws Exception
  {
    final OPDSJSONSerializerType in_json_serializer =
      OPDSJSONSerializer.newSerializer();
    final OPDSJSONParserType in_json_parser = OPDSJSONParser.newParser();
    final File in_directory = File.createTempFile("pre", "");

    final BookDatabaseType bd = BookDatabase.newDatabase(
      in_json_serializer, in_json_parser, in_directory);

    TestUtilities.expectException(
      IOException.class, new PartialProcedureType<Unit, Exception>()
      {
        @Override public void call(
          final Unit x)
          throws Exception
        {
          bd.create();
        }
      });
  }
}
