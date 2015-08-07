package org.nypl.simplified.app.reader;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;
import org.json.JSONException;
import org.json.JSONObject;
import org.nypl.simplified.app.utilities.LogUtilities;
import org.nypl.simplified.books.core.BookID;
import org.slf4j.Logger;

/**
 * <p>The default implementation of the {@link ReaderBookmarksType}
 * interface.</p>
 *
 * <p>This implementation uses the Android `SharedPreferences` class to
 * serialize bookmarks.</p>
 */

public final class ReaderBookmarks implements ReaderBookmarksType
{
  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(ReaderBookmarks.class);
  }

  private final SharedPreferences bookmarks;

  private ReaderBookmarks(
    final Context cc)
  {
    NullCheck.notNull(cc);
    this.bookmarks =
      NullCheck.notNull(cc.getSharedPreferences("reader-bookmarks", 0));
  }

  /**
   * Open the bookmarks database.
   *
   * @param cc The application context
   *
   * @return A bookmarks database
   */

  public static ReaderBookmarksType openBookmarks(
    final Context cc)
  {
    return new ReaderBookmarks(cc);
  }

  @Override public OptionType<ReaderBookLocation> getBookmark(
    final BookID id)
  {
    NullCheck.notNull(id);
    final String key = NullCheck.notNull(id.toString());

    try {
      if (this.bookmarks.contains(key)) {
        final String text = this.bookmarks.getString(key, null);
        if (text != null) {
          final JSONObject o = new JSONObject(text);
          return Option.some(ReaderBookLocation.fromJSON(o));
        }
      }
      return Option.none();
    } catch (final JSONException e) {
      ReaderBookmarks.LOG.error(
        "unable to deserialize bookmark: {}", e.getMessage(), e);
      return Option.none();
    }
  }

  @Override public void setBookmark(
    final BookID id,
    final ReaderBookLocation bookmark)
  {
    NullCheck.notNull(id);
    NullCheck.notNull(bookmark);

    try {
      ReaderBookmarks.LOG.debug(
        "saving bookmark for book {}: {}", id, bookmark);

      final JSONObject o = NullCheck.notNull(bookmark.toJSON());
      final String text = NullCheck.notNull(o.toString());
      final String key = NullCheck.notNull(id.toString());
      final Editor e = this.bookmarks.edit();
      e.putString(key, text);
      e.apply();
    } catch (final JSONException e) {
      ReaderBookmarks.LOG.error(
        "unable to serialize bookmark: {}", e.getMessage(), e);
    }
  }
}
