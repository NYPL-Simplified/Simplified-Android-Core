/*
 * Copyright © 2015 <code@io7m.com> http://io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package org.nypl.simplified.books.core;

import com.io7m.jfunctional.OptionType;

import java.io.IOException;
import java.util.Set;

/**
 * A read-only interface to the book database.
 */

public interface BookDatabaseReadableType
{
  /**
   * @return {@code true} if user credentials exist in the database.
   */

  boolean databaseAccountCredentialsExist();

  /**
   * @return The user credentials
   *
   * @throws IOException On I/O errors
   */

  AccountCredentials databaseAccountCredentialsGet()
    throws IOException;

  /**
   * @param book The book ID
   *
   * @return A snapshot of the most recently written data for the given book, if
   * the book exists
   */

  OptionType<BookDatabaseEntrySnapshot> databaseGetEntrySnapshot(
    BookID book);

  /**
   * @return The set of books currently in the database
   */

  Set<BookID> databaseGetBooks();
}
