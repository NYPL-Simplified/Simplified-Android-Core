package org.nypl.simplified.app.reader;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;
import org.nypl.simplified.books.core.BookID;
import org.nypl.simplified.books.core.LogUtilities;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.slf4j.Logger;

import java.util.Timer;
import java.util.TimerTask;

/**
 * <p>The default implementation of the {@link ReaderBookmarksSharedPrefsType}
 * interface.</p>
 *
 * <p>This implementation uses the Android `SharedPreferences` class to
 * serialize bookmarksSharedPrefs.</p>
 */

public final class ReaderBookmarksSharedPrefs implements ReaderBookmarksSharedPrefsType
{
  private static final Logger LOG;
  private Timer write_timer = new Timer();

  static {
    LOG = LogUtilities.getLog(ReaderBookmarksSharedPrefs.class);
  }

  private final SharedPreferences bookmarksSharedPrefs;

  private ReaderBookmarksSharedPrefs(
    final Context cc)
  {
    NullCheck.notNull(cc);
    this.bookmarksSharedPrefs =
      NullCheck.notNull(cc.getSharedPreferences("reader-bookmarks", 0));
  }

  /**
   * Open the bookmarksSharedPrefs database.
   *
   * @param cc The application context
   *
   * @return A bookmarksSharedPrefs database
   */

  public static ReaderBookmarksSharedPrefsType openBookmarksSharedPrefs(
    final Context cc)
  {
    return new ReaderBookmarksSharedPrefs(cc);
  }

  /**
   * ReaderBookmarksSharedPrefsType Methods
   */

  @Nullable
  @Override
  public ReaderBookLocation getReadingPosition(
      @NotNull BookID id,
      @NotNull OPDSAcquisitionFeedEntry entry) {

    final String key = NullCheck.notNull(id.toString());

    try {
      if (this.bookmarksSharedPrefs.contains(key)) {
        final String text = this.bookmarksSharedPrefs.getString(key, null);
        if (text != null) {
          final JSONObject o = new JSONObject(text);
          return ReaderBookLocation.fromJSON(o);
        }
      }
      return null;
    } catch (final JSONException e) {
      ReaderBookmarksSharedPrefs.LOG.error(
          "unable to deserialize bookmark: {}", e.getMessage(), e);
      return null;
    }
  }

  @Override
  public void saveReadingPosition(
      @NotNull BookID id,
      @NotNull ReaderBookLocation bookmark) {

    // Rate-limit writes to shared prefs
    this.write_timer.cancel();
    this.write_timer = new Timer();
    this.write_timer.schedule(
        new TimerTask() {
          @Override
          public void run() {
            try {
              ReaderBookmarksSharedPrefs.LOG.debug(
                  "saving bookmark for book {}: {}", id, bookmark);

              // save to bookmarksSharedPrefs database
              if (!"null".equals(((Some<String>) bookmark.getContentCFI()).get())) {

                final JSONObject o = NullCheck.notNull(bookmark.toJSON());
                final String text = NullCheck.notNull(o.toString());
                final String key = NullCheck.notNull(id.toString());
                final Editor e = ReaderBookmarksSharedPrefs.this.bookmarksSharedPrefs.edit();
                e.putString(key, text);
                e.apply();

              }
            } catch (final JSONException e) {
              ReaderBookmarksSharedPrefs.LOG.error(
                  "unable to serialize bookmark: {}", e.getMessage(), e);
            }
            ReaderBookmarksSharedPrefs.LOG.debug("CurrentPage timer run ");
          }
        }, 3000L);
  }
}
