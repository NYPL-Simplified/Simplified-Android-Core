package org.nypl.simplified.app.reader;

import org.nypl.simplified.books.core.BookID;

import com.io7m.jfunctional.OptionType;

public interface ReaderBookmarksType
{
  OptionType<ReaderBookLocation> getBookmark(
    BookID id);

  void setBookmark(
    BookID id,
    ReaderBookLocation bookmark);
}
