package org.nypl.simplified.circanalytics;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.io7m.jfunctional.Some;

import org.nypl.simplified.books.core.AccountCredentials;
import org.nypl.simplified.books.core.FeedEntryOPDS;
import org.nypl.simplified.books.core.LogUtilities;
import org.nypl.simplified.volley.NYPLStringRequest;
import org.slf4j.Logger;

import java.net.URI;

/**
 * Created by aferditamuriqi on 10/24/16.
 *
 */

public final class CirculationAnalytics {


  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(CirculationAnalytics.class);
  }

  /**
   *
   */
  public static void postEvent(AccountCredentials creds, Context context, FeedEntryOPDS entry, String event)
  {

    final RequestQueue queue = Volley.newRequestQueue(context);

    final URI url = ((Some<URI>) entry.getFeedEntry().getAnalytics()).get().resolve(event);

    final String stringUrl =  ((Some<URI>) entry.getFeedEntry().getAnalytics()).get().toString() + "/" + event;

    final NYPLStringRequest request = new NYPLStringRequest(Request.Method.GET, stringUrl, creds, new Response.Listener<String>() {
      @Override
      public void onResponse(final String string) {
        // do nothing
        LOG.debug(string);
      }
    }, new Response.ErrorListener() {
      @Override
      public void onErrorResponse(final VolleyError error) {
        // do nothing
        LOG.debug(error.toString());
      }
    });
    queue.add(request);

  }

}
