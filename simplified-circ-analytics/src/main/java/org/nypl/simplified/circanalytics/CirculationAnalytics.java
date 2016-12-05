package org.nypl.simplified.circanalytics;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;

import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.io7m.jfunctional.Some;

import org.nypl.simplified.books.core.AccountCredentials;
import org.nypl.simplified.books.core.FeedEntryOPDS;
import org.nypl.simplified.books.core.LogUtilities;
import org.nypl.simplified.books.core.NetworkStateReceiver;
import org.nypl.simplified.books.core.NetworkStateReceiver.NetworkStateReceiverListener;
import org.nypl.simplified.volley.NYPLStringRequest;
import org.slf4j.Logger;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by aferditamuriqi on 10/24/16.
 */

public final class CirculationAnalytics implements NetworkStateReceiverListener, Application.ActivityLifecycleCallbacks {

    private static final Logger LOG;
    private final RequestQueue queue;
    private final NetworkStateReceiver networkStateReceiver;
    private final HashMap<String, Integer> activities;
    private final Application application;

    static {
        LOG = LogUtilities.getLog(CirculationAnalytics.class);
    }

    public CirculationAnalytics(Activity activity) {

        application = (Application) activity.getApplicationContext();
        activities = new HashMap<String, Integer>();
        queue = Volley.newRequestQueue(application);
        application.registerActivityLifecycleCallbacks(this);
        networkStateReceiver = new NetworkStateReceiver(application);
        networkStateReceiver.addListener(this);
        application.registerReceiver(networkStateReceiver, new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));

    }

    /**
     *
     */
    public void postEvent(AccountCredentials creds, FeedEntryOPDS entry, String event) {

        final URI url = ((Some<URI>) entry.getFeedEntry().getAnalytics()).get().resolve(event);

        final String stringUrl = ((Some<URI>) entry.getFeedEntry().getAnalytics()).get().toString() + "/" + event;

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

    @Override
    public void onNetworkAvailable() {
        queue.start();
    }

    @Override
    public void onNetworkUnavailable() {
        queue.stop();
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

    }

    @Override
    public void onActivityStarted(Activity activity) {
        //map Activity unique class name with 1 on foreground
        activities.put(activity.getLocalClassName(), 1);
        applicationStatus();
    }

    @Override
    public void onActivityResumed(Activity activity) {
        if(activity.getApplicationContext() == application) {
            application.registerReceiver(networkStateReceiver, new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        if(activity.getApplicationContext() == application) {
            application.unregisterReceiver(networkStateReceiver);
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {
        //map Activity unique class name with 0 on foreground
        activities.put(activity.getLocalClassName(), 0);
        applicationStatus();
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }

    /**
     * Check if any activity is in the foreground
     */
    private boolean isBackGround() {
        for (String s : activities.keySet()) {
            if (activities.get(s) == 1) {
                return false;
            }
        }
        return true;
    }

    /**
     * Log application status.
     */
    private void applicationStatus() {
        boolean backgrounded = isBackGround();
        LOG.debug("ApplicationStatus", "Is application background" + backgrounded);
    }

}
