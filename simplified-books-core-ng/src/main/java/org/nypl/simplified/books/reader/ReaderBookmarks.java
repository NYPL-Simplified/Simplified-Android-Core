package org.nypl.simplified.books.reader;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import com.io7m.jnull.NullCheck;

import org.nypl.simplified.books.book_database.BookID;

import java.util.HashMap;

/**
 * The reader bookmarks.
 */

@AutoValue
public abstract class ReaderBookmarks {

  /**
   * @return The bookmarks
   */

  public abstract ImmutableMap<BookID, ReaderBookLocation> bookmarks();

  /**
   * @return A set of bookmarks
   */

  public static ReaderBookmarks create(final ImmutableMap<BookID, ReaderBookLocation> bookmarks) {
    return new AutoValue_ReaderBookmarks(bookmarks);
  }

  /**
   * Add a bookmark.
   *
   * @param book_id      The book id
   * @param new_location The book location
   * @return A new set of bookmarks with the extra bookmark added
   */

  public final ReaderBookmarks withBookmark(
      final BookID book_id,
      final ReaderBookLocation new_location) {
    final HashMap<BookID, ReaderBookLocation> m = new HashMap<>(this.bookmarks());
    m.put(NullCheck.notNull(book_id, "Book"),
        NullCheck.notNull(new_location, "Location"));
    return create(ImmutableMap.copyOf(m));
  }
}
