package org.nypl.simplified.books.core;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * The type of book databases.
 */

public interface BookDatabaseType
{
  /**
   * Create the initial empty database. Has no effect if the database already
   * exists.
   *
   * @throws IOException On I/O errors
   */

  void create()
    throws IOException;

  /**
   * @return {@code true} if user credentials exist in the database.
   */

  boolean credentialsExist();

  /**
   * @return The user credentials
   *
   * @throws IOException On I/O errors
   */

  AccountCredentials credentialsGet()
    throws IOException;

  /**
   * Set the user credentials.
   *
   * @param credentials The credentials
   *
   * @throws IOException On I/O errors
   */

  void credentialsSet(
    final AccountCredentials credentials)
    throws IOException;

  /**
   * Destroy the database.
   *
   * @throws IOException On I/O errors
   */

  void destroy()
    throws IOException;

  /**
   * @return The list of database entries
   */

  List<BookDatabaseEntryType> getBookDatabaseEntries();

  /**
   * @param book_id The book ID
   *
   * @return The database entry for {@code book_id}
   */

  BookDatabaseEntryType getBookDatabaseEntry(
    BookID book_id);

  /**
   * @return The location of the database
   */

  File getLocation();
}
