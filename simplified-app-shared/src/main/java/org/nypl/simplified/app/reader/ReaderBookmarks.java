package org.nypl.simplified.app.reader;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;

import org.joda.time.Instant;
import org.json.JSONException;
import org.json.JSONObject;
import org.nypl.drm.core.AdobeDeviceID;
import org.nypl.simplified.app.Simplified;
import org.nypl.simplified.app.SimplifiedCatalogAppServicesType;
import org.nypl.simplified.app.catalog.annotation.Annotation;
import org.nypl.simplified.app.catalog.annotation.Selector;
import org.nypl.simplified.app.catalog.annotation.Target;
import org.nypl.simplified.books.core.AccountCredentials;
import org.nypl.simplified.books.core.BookID;
import org.nypl.simplified.books.core.BooksControllerConfigurationType;
import org.nypl.simplified.books.core.BooksType;
import org.nypl.simplified.books.core.LogUtilities;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.nypl.simplified.volley.NYPLJsonObjectRequest;
import org.slf4j.Logger;

import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;

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
  private Timer write_timer = new Timer();

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

  @Override
  public OptionType<ReaderBookLocation> getBookmark(
    final BookID id,
    final OPDSAcquisitionFeedEntry entry) {

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


  @Override
  public void setBookmark(
    final BookID id,
    final ReaderBookLocation bookmark,
    final OPDSAcquisitionFeedEntry entry,
    final AccountCredentials credentials,
    final RequestQueue queue) {
    NullCheck.notNull(id);
    NullCheck.notNull(bookmark);
    NullCheck.notNull(entry);

    this.write_timer.cancel();
    this.write_timer = new Timer();
    this.write_timer.schedule(
      new TimerTask() {
        @Override
        public void run() {
          try {
            ReaderBookmarks.LOG.debug(
              "saving bookmark for book {}: {}", id, bookmark);

            // save to server
            if (!"null".equals(((Some<String>) bookmark.getContentCFI()).get())) {
              final Annotation annotation = new Annotation();
              annotation.setContext("http://www.w3.org/ns/anno.jsonld");
              annotation.setType("Annotation");
              annotation.setMotivation("http://librarysimplified.org/terms/annotation/idling");
              annotation.setTarget(new Target(entry.getID(), new Selector("oa:FragmentSelector", bookmark.toJSON().toString())));
              final JsonObject body = new JsonObject();
              body.addProperty("http://librarysimplified.org/terms/time", new Instant().toString());
              body.addProperty("http://librarysimplified.org/terms/device", ((Some<AdobeDeviceID>) credentials.getAdobeDeviceID()).get().getValue());
              annotation.setBody(body);

              Gson gson = new GsonBuilder()
                .disableHtmlEscaping()
                .create();

              ReaderBookmarks.LOG.debug(
                "annotation to post: {}", gson.toJson(annotation).toString());
              ReaderBookmarks.LOG.debug(
                "annotation to post: {}", annotation.toString());

              final SimplifiedCatalogAppServicesType app =
                Simplified.getCatalogAppServices();
              final BooksType books = app.getBooks();
              final BooksControllerConfigurationType books_config =
                books.booksGetConfiguration();

              final URI uri = books_config.getCurrentRootFeedURI().resolve("annotations/");

              final NYPLJsonObjectRequest post_request = new NYPLJsonObjectRequest(
                Request.Method.POST,
                uri.toString(),
                gson.toJson(annotation),
                credentials,
                new Response.Listener<JSONObject>() {
                  @Override
                  public void onResponse(final JSONObject json_request) {
                    //do nothing
                  }
                },
                new Response.ErrorListener() {
                  @Override
                  public void onErrorResponse(final VolleyError volley_error) {
                    //do nothing
                  }
                });
              queue.add(post_request);

              final JSONObject o = NullCheck.notNull(bookmark.toJSON());
              final String text = NullCheck.notNull(o.toString());
              final String key = NullCheck.notNull(id.toString());
              final Editor e = ReaderBookmarks.this.bookmarks.edit();
              e.putString(key, text);
              e.apply();

            }
          } catch (final JSONException e) {
            ReaderBookmarks.LOG.error(
              "unable to serialize bookmark: {}", e.getMessage(), e);
          }
          ReaderBookmarks.LOG.debug("CurrentPage timer run ");
        }
      }, 3000L);

  }
}
