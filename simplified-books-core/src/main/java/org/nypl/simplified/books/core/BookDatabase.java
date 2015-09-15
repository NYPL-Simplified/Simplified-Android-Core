package org.nypl.simplified.books.core;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import org.nypl.simplified.files.DirectoryUtilities;
import org.nypl.simplified.files.FileUtilities;
import org.nypl.simplified.opds.core.OPDSJSONParserType;
import org.nypl.simplified.opds.core.OPDSJSONSerializerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A file-based book database.
 */

public final class BookDatabase implements BookDatabaseType
{
  private static final Logger LOG;

  static {
    LOG = NullCheck.notNull(LoggerFactory.getLogger(BookDatabase.class));
  }

  private final File                   directory;
  private final File                   file_credentials;
  private final File                   file_credentials_tmp;
  private final OPDSJSONParserType     parser;
  private final OPDSJSONSerializerType serializer;

  private BookDatabase(
    final OPDSJSONSerializerType in_json_serializer,
    final OPDSJSONParserType in_json_parser,
    final File in_directory)
  {
    this.directory = NullCheck.notNull(in_directory);
    this.parser = NullCheck.notNull(in_json_parser);
    this.serializer = NullCheck.notNull(in_json_serializer);

    this.file_credentials = new File(this.directory, "account.json");
    this.file_credentials_tmp = new File(this.directory, "account.json.tmp");

    BookDatabase.LOG.debug("opened database {}", this.directory);
  }

  /**
   * Open a database at the given directory.
   *
   * @param in_json_serializer A JSON serializer
   * @param in_json_parser     A JSON parser
   * @param in_directory       The directory
   *
   * @return A reference to the database
   */

  public static BookDatabaseType newDatabase(
    final OPDSJSONSerializerType in_json_serializer,
    final OPDSJSONParserType in_json_parser,
    final File in_directory)
  {
    return new BookDatabase(in_json_serializer, in_json_parser, in_directory);
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

  @Override public AccountCredentials credentialsGet()
    throws IOException
  {
    final String text = FileUtilities.fileReadUTF8(this.file_credentials);
    return AccountCredentialsJSON.deserializeFromText(text);
  }

  @Override public void credentialsSet(
    final AccountCredentials credentials)
    throws IOException
  {
    NullCheck.notNull(credentials);

    final String text = AccountCredentialsJSON.serializeToText(credentials);
    FileUtilities.fileWriteUTF8Atomically(
      this.file_credentials, this.file_credentials_tmp, text);
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
      new ArrayList<BookDatabaseEntryType>(32);

    if (this.directory.isDirectory()) {
      final File[] book_list = this.directory.listFiles(
        new FileFilter()
        {
          @Override public boolean accept(
            final @Nullable File path)
          {
            return NullCheck.notNull(path).isDirectory();
          }
        });

      for (final File f : book_list) {
        final BookID id = BookID.exactString(NullCheck.notNull(f.getName()));
        xs.add(
          new BookDatabaseEntry(
            this.serializer, this.parser, this.directory, id));
      }
    }

    return xs;
  }

  @Override public BookDatabaseEntryType getBookDatabaseEntry(
    final BookID book_id)
  {
    return new BookDatabaseEntry(
      this.serializer, this.parser, this.directory, NullCheck.notNull(book_id));
  }

  @Override public File getLocation()
  {
    return this.directory;
  }
}
