package org.nypl.simplified.books.core;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.io7m.jfunctional.Pair;

/**
 * The type of book databases.
 */

public interface BookDatabaseType
{
  /**
   * Create the initial empty database. Has no effect if the database already
   * exists.
   *
   * @throws IOException
   *           On I/O errors
   */

  void create()
    throws IOException;

  /**
   * @return <tt>true</tt> if user credentials exist in the database.
   */

  boolean credentialsExist();

  /**
   * @return The user credentials
   * @throws IOException
   *           On I/O errors
   */

  Pair<AccountBarcode, AccountPIN> credentialsGet()
    throws IOException;

  /**
   * Set the user credentials.
   *
   * @param barcode
   *          The barcode
   * @param pin
   *          The PIN
   * @throws IOException
   *           On I/O errors
   */

  void credentialsSet(
    AccountBarcode barcode,
    AccountPIN pin)
    throws IOException;

  /**
   * Destroy the database.
   *
   * @throws IOException
   *           On I/O errors
   */

  void destroy()
    throws IOException;

  /**
   * @return The list of database entries
   */

  List<BookDatabaseEntryType> getBookDatabaseEntries();

  /**
   * @param book_id
   *          The book ID
   * @return The database entry for <tt>book_id</tt>
   */

  BookDatabaseEntryType getBookDatabaseEntry(
    BookID book_id);

  /**
   * @return The location of the database
   */

  File getLocation();
}
