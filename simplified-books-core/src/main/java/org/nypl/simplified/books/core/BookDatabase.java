package org.nypl.simplified.books.core;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.nypl.simplified.files.DirectoryUtilities;
import org.nypl.simplified.files.FileUtilities;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParserType;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntrySerializerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.io7m.jfunctional.Pair;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

/**
 * A file-based book database.
 */

public final class BookDatabase implements BookDatabaseType
{
  private static final Logger LOG;

  static {
    LOG = NullCheck.notNull(LoggerFactory.getLogger(BookDatabase.class));
  }

  /**
   * Open a database at the given directory.
   *
   * @param in_directory
   *          The directory
   * @return A reference to the database
   */

  public static BookDatabaseType newDatabase(
    final OPDSAcquisitionFeedEntryParserType in_parser,
    final OPDSAcquisitionFeedEntrySerializerType in_serializer,
    final File in_directory)
  {
    return new BookDatabase(in_parser, in_serializer, in_directory);
  }

  private final File                                   directory;
  private final File                                   file_credentials;
  private final File                                   file_credentials_tmp;
  private final OPDSAcquisitionFeedEntryParserType     parser;
  private final OPDSAcquisitionFeedEntrySerializerType serializer;

  private BookDatabase(
    final OPDSAcquisitionFeedEntryParserType in_parser,
    final OPDSAcquisitionFeedEntrySerializerType in_serializer,
    final File in_directory)
  {
    this.directory = NullCheck.notNull(in_directory);
    this.parser = NullCheck.notNull(in_parser);
    this.serializer = NullCheck.notNull(in_serializer);

    this.file_credentials = new File(this.directory, "credentials.txt");
    this.file_credentials_tmp =
      new File(this.directory, "credentials.txt.tmp");

    BookDatabase.LOG.debug("opened database {}", this.directory);
  }

  @Override public void create()
    throws IOException
  {
    DirectoryUtilities.directoryCreate(this.directory);
  }

  @Override public boolean credentialsExist()
  {
    return this.file_credentials.isFile();
  }

  @Override public Pair<AccountBarcode, AccountPIN> credentialsGet()
    throws IOException
  {
    final String text = FileUtilities.fileReadUTF8(this.file_credentials);
    final String[] segments = text.split(":");
    final AccountBarcode b =
      new AccountBarcode(NullCheck.notNull(segments[0]));
    final AccountPIN p = new AccountPIN(NullCheck.notNull(segments[1]));
    return Pair.pair(b, p);
  }

  @Override public void credentialsSet(
    final AccountBarcode barcode,
    final AccountPIN pin)
    throws IOException
  {
    NullCheck.notNull(barcode);
    NullCheck.notNull(pin);

    final String text =
      NullCheck.notNull(String.format("%s:%s", barcode, pin));
    FileUtilities.fileWriteUTF8Atomically(
      this.file_credentials,
      this.file_credentials_tmp,
      text);
  }

  @Override public void destroy()
    throws IOException
  {
    if (this.directory.isDirectory()) {
      final List<BookDatabaseEntryType> es = this.getBookDatabaseEntries();
      for (final BookDatabaseEntryType e : es) {
        e.destroy();
      }
    } else {
      throw new IllegalStateException("Not logged in");
    }
  }

  @Override public List<BookDatabaseEntryType> getBookDatabaseEntries()
  {
    final List<BookDatabaseEntryType> xs =
      new ArrayList<BookDatabaseEntryType>();

    if (this.directory.isDirectory()) {
      final File[] book_list = this.directory.listFiles(new FileFilter() {
        @Override public boolean accept(
          final @Nullable File path)
        {
          return NullCheck.notNull(path).isDirectory();
        }
      });

      for (final File f : book_list) {
        final BookID id = BookID.exactString(NullCheck.notNull(f.getName()));
        xs.add(new BookDatabaseEntry(
          this.parser,
          this.serializer,
          this.directory,
          id));
      }
    }

    return xs;
  }

  @Override public BookDatabaseEntryType getBookDatabaseEntry(
    final BookID book_id)
  {
    return new BookDatabaseEntry(
      this.parser,
      this.serializer,
      this.directory,
      NullCheck.notNull(book_id));
  }

  @Override public File getLocation()
  {
    return this.directory;
  }
}
