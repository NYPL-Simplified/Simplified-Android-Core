package org.nypl.simplified.ui.splash;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.MailTo;
import android.net.Uri;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.lang.ref.WeakReference;

/**
 * Used to handle mailto: links in the eula.html
 */
public class MailtoWebViewClient extends WebViewClient {
    private final WeakReference<Activity> mActivityRef;

    public MailtoWebViewClient(Activity activity) {
        mActivityRef = new WeakReference<>(activity);
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (url.startsWith(MailTo.MAILTO_SCHEME)) {
            final Activity activity = mActivityRef.get();
            if (activity != null) {
                MailTo mt = MailTo.parse(url);
                Intent i = newEmailIntent(activity, mt.getTo(), mt.getSubject());
                if (i.resolveActivity(activity.getPackageManager()) != null) {
                    activity.startActivity(i);
                }
                return true;
            }
        } else {
            return false;
        }
        return true;
    }

    private Intent newEmailIntent(Context context, String address, String subject) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:")); // only email apps should handle this
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{address});
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        return intent;
    }
}
