package org.nypl.simplified.circanalytics;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.io7m.jfunctional.Some;

import org.nypl.simplified.books.accounts.AccountAuthenticationCredentials;
import org.nypl.simplified.books.feeds.FeedEntryOPDS;
import org.nypl.simplified.volley.NYPLStringRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

public final class CirculationAnalytics {

  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(CirculationAnalytics.class);
  }

  /**
   *
   */
  public static void postEvent(
    AccountAuthenticationCredentials creds,
    Context context,
    FeedEntryOPDS entry,
    String event) {

    final RequestQueue queue = Volley.newRequestQueue(context);

    final URI url = ((Some<URI>) entry.getFeedEntry().getAnalytics()).get().resolve(event);

    final String stringUrl = ((Some<URI>) entry.getFeedEntry().getAnalytics()).get().toString() + "/" + event;

    final NYPLStringRequest request =
      new NYPLStringRequest(Request.Method.GET, stringUrl, creds, string -> {
      // do nothing
      LOG.debug(string);
    }, error -> {
      // do nothing
      LOG.debug(error.toString());
    });

    queue.add(request);
  }

}
