package org.nypl.simplified.tests.books.book_database;

import com.io7m.jfunctional.Option;

import org.junit.Assert;
import org.junit.Test;
import org.nypl.simplified.books.accounts.AccountID;
import org.nypl.simplified.books.book_database.BookDatabase;
import org.nypl.simplified.books.book_database.BookDatabaseEntryType;
import org.nypl.simplified.books.book_database.BookDatabaseType;
import org.nypl.simplified.books.book_database.BookID;
import org.nypl.simplified.files.DirectoryUtilities;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.nypl.simplified.opds.core.OPDSAvailabilityOpenAccess;
import org.nypl.simplified.opds.core.OPDSJSONParser;
import org.nypl.simplified.opds.core.OPDSJSONParserType;
import org.nypl.simplified.opds.core.OPDSJSONSerializer;
import org.nypl.simplified.opds.core.OPDSJSONSerializerType;

import java.io.File;
import java.util.Calendar;

public abstract class BookDatabaseContract {

  @Test
  public final void openEmpty()
      throws Exception {

    final OPDSJSONParserType parser = OPDSJSONParser.newParser();
    final OPDSJSONSerializerType serializer = OPDSJSONSerializer.newSerializer();

    final File directory =
        DirectoryUtilities.directoryCreateTemporary();
    final BookDatabaseType db =
        BookDatabase.open(parser, serializer, AccountID.create(1), directory);

    Assert.assertEquals(0L, db.books().size());
  }

  @Test
  public final void openCreateReopen()
      throws Exception {

    final OPDSJSONParserType parser = OPDSJSONParser.newParser();
    final OPDSJSONSerializerType serializer = OPDSJSONSerializer.newSerializer();

    final File directory =
        DirectoryUtilities.directoryCreateTemporary();
    final BookDatabaseType db0 =
        BookDatabase.open(parser, serializer, AccountID.create(1), directory);

    final OPDSAcquisitionFeedEntry entry0 =
        OPDSAcquisitionFeedEntry.newBuilder(
            "a",
            "Title",
            Calendar.getInstance(),
            OPDSAvailabilityOpenAccess.get(Option.none()))
            .build();

    final OPDSAcquisitionFeedEntry entry1 =
        OPDSAcquisitionFeedEntry.newBuilder(
            "b",
            "Title",
            Calendar.getInstance(),
            OPDSAvailabilityOpenAccess.get(Option.none()))
            .build();

    final OPDSAcquisitionFeedEntry entry2 =
        OPDSAcquisitionFeedEntry.newBuilder(
            "c",
            "Title",
            Calendar.getInstance(),
            OPDSAvailabilityOpenAccess.get(Option.none()))
            .build();

    final BookID id0 = BookID.create("a");
    db0.createOrUpdate(id0, entry0);
    final BookID id1 = BookID.create("b");
    db0.createOrUpdate(id1, entry1);
    final BookID id2 = BookID.create("c");
    db0.createOrUpdate(id2, entry2);

    final BookDatabaseType db1 =
        BookDatabase.open(parser, serializer, AccountID.create(1), directory);

    Assert.assertEquals(3, db1.books().size());
    Assert.assertTrue(db1.books().contains(id0));
    Assert.assertTrue(db1.books().contains(id1));
    Assert.assertTrue(db1.books().contains(id2));
    Assert.assertEquals(db1.entry(id0).book().id().value(), entry0.getID());
    Assert.assertEquals(db1.entry(id1).book().id().value(), entry1.getID());
    Assert.assertEquals(db1.entry(id2).book().id().value(), entry2.getID());
  }

  @Test
  public final void openCreateDelete()
      throws Exception {

    final OPDSJSONParserType parser = OPDSJSONParser.newParser();
    final OPDSJSONSerializerType serializer = OPDSJSONSerializer.newSerializer();

    final File directory =
        DirectoryUtilities.directoryCreateTemporary();
    final BookDatabaseType db0 =
        BookDatabase.open(parser, serializer, AccountID.create(1), directory);

    final OPDSAcquisitionFeedEntry entry0 =
        OPDSAcquisitionFeedEntry.newBuilder(
            "a",
            "Title",
            Calendar.getInstance(),
            OPDSAvailabilityOpenAccess.get(Option.none()))
            .build();

    final BookID id0 = BookID.create("a");
    final BookDatabaseEntryType db_entry = db0.createOrUpdate(id0, entry0);
    Assert.assertEquals(1, db0.books().size());
    db_entry.delete();
    Assert.assertEquals(0, db0.books().size());
  }
}
