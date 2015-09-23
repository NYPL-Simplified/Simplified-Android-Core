package org.nypl.simplified.app.reader;

import com.io7m.jfunctional.OptionType;
import org.nypl.simplified.books.core.BookID;

/**
 * The interface exposed by bookmarks databases.
 */

public interface ReaderBookmarksType
{
  /**
   * Retrieve the current bookmark for the given book, if any.
   *
   * @param id The ID of the book
   *
   * @return A bookmark, if any
   */

  OptionType<ReaderBookLocation> getBookmark(
    BookID id);

  /**
   * Set the bookmark for the given book.
   *
   * @param id       The ID of the book
   * @param bookmark The bookmark
   */

  void setBookmark(
    BookID id,
    ReaderBookLocation bookmark);
}
