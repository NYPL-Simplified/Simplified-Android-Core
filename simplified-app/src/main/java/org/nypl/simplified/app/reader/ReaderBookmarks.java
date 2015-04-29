package org.nypl.simplified.app.reader;

import org.nypl.simplified.app.utilities.LogUtilities;
import org.nypl.simplified.books.core.BookID;
import org.slf4j.Logger;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;

public final class ReaderBookmarks implements ReaderBookmarksType
{
  private static final Logger     LOG;

  static {
    LOG = LogUtilities.getLog(ReaderBookmarks.class);
  }

  public static ReaderBookmarksType openBookmarks(
    final Context cc)
  {
    return new ReaderBookmarks(cc);
  }

  private final SharedPreferences bookmarks;

  private ReaderBookmarks(
    final Context cc)
  {
    NullCheck.notNull(cc);
    this.bookmarks =
      NullCheck.notNull(cc.getSharedPreferences("reader-bookmarks", 0));
  }

  @Override public OptionType<String> getBookmark(
    final BookID id)
  {
    final String key = id.toString();
    if (this.bookmarks.contains(key)) {
      return Option.of(this.bookmarks.getString(key, null));
    }
    return Option.none();
  }

  @Override public void setBookmark(
    final BookID id,
    final String bookmark)
  {
    final Editor e = this.bookmarks.edit();
    e.putString(id.toString(), bookmark);
    e.apply();
  }
}
