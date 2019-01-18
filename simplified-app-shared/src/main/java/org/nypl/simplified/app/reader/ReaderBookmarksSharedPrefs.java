package org.nypl.simplified.app.reader;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nypl.simplified.books.core.BookID;
import org.nypl.simplified.books.reader.ReaderBookLocation;
import org.nypl.simplified.books.reader.ReaderBookLocationJSON;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * <p>The default implementation of the {@link ReaderBookmarksSharedPrefsType}
 * interface.</p>
 *
 * <p>This implementation uses the Android `SharedPreferences` class to
 * serialize bookmarksSharedPrefs.</p>
 */

public final class ReaderBookmarksSharedPrefs implements ReaderBookmarksSharedPrefsType {

  private static final Logger LOG = LoggerFactory.getLogger(ReaderBookmarksSharedPrefs.class);
  private Timer write_timer = new Timer();
  private final SharedPreferences bookmarksSharedPrefs;
  private final ObjectMapper jsonMapper;

  private ReaderBookmarksSharedPrefs(
    final Context cc) {
    NullCheck.notNull(cc);
    this.bookmarksSharedPrefs =
      NullCheck.notNull(cc.getSharedPreferences("reader-bookmarks", 0));
    this.jsonMapper = new ObjectMapper();
  }

  /**
   * Open the bookmarksSharedPrefs database.
   *
   * @param cc The application context
   * @return A bookmarksSharedPrefs database
   */

  public static ReaderBookmarksSharedPrefsType openBookmarksSharedPrefs(
    final Context cc) {
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
          return ReaderBookLocationJSON.deserializeFromString(this.jsonMapper, text);
        }
      }
      return null;
    } catch (final IOException e) {
      LOG.error("unable to deserialize bookmark: {}", e.getMessage(), e);
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
            LOG.debug("saving bookmark for book {}: {}", id, bookmark);

            // save to bookmarksSharedPrefs database
            if (!"null".equals(((Some<String>) bookmark.contentCFI()).get())) {
              final String text =
                NullCheck.notNull(ReaderBookLocationJSON.serializeToString(jsonMapper, bookmark));
              final String key =
                NullCheck.notNull(id.toString());
              final Editor e =
                ReaderBookmarksSharedPrefs.this.bookmarksSharedPrefs.edit();
              e.putString(key, text);
              e.apply();

            }
          } catch (final IOException e) {
            LOG.error("unable to serialize bookmark: {}", e.getMessage(), e);
          }
          LOG.debug("CurrentPage timer run ");
        }
      }, 3000L);
  }
}
