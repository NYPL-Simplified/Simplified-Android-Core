package org.nypl.simplified.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

import org.json.JSONException;
import org.json.JSONObject;
import org.nypl.drm.core.AdobeVendorID;
import org.nypl.simplified.books.core.AccountAdobeToken;
import org.nypl.simplified.books.core.AccountAuthProvider;
import org.nypl.simplified.books.core.AccountAuthToken;
import org.nypl.simplified.books.core.AccountBarcode;
import org.nypl.simplified.books.core.AccountCredentials;
import org.nypl.simplified.books.core.AccountLoginListenerType;
import org.nypl.simplified.books.core.AccountPIN;
import org.nypl.simplified.books.core.AccountPatron;
import org.nypl.simplified.books.core.BookID;
import org.nypl.simplified.books.core.BooksType;
import org.nypl.simplified.books.core.LogUtilities;
import org.slf4j.Logger;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.StringTokenizer;

/**
 * A mindlessly simple activity that displays a given URI in a full-screen web
 * view.
 */

public final class CleverLoginActivity extends Activity implements AccountLoginListenerType {

  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(CleverLoginActivity.class);
  }

  private WebView web_view;
  private String redirect_uri = "open-ebooks-clever://oauth";
  private String provider = "Clever";
  private String feed_initial_uri;

  private
  @Nullable
  LoginListenerType listener;

  /**
   * Construct an activity.
   */

  public CleverLoginActivity() {

  }


  @Override
  public boolean onOptionsItemSelected(
    final @Nullable MenuItem item_mn) {
    final MenuItem item = NullCheck.notNull(item_mn);
    switch (item.getItemId()) {

      case android.R.id.home: {
        onBackPressed();
        return true;
      }

      default: {
        return super.onOptionsItemSelected(item);
      }
    }
  }


  @SuppressLint("JavascriptInterface")
  @Override
  protected void onCreate(final Bundle state) {
    super.onCreate(state);

    this.setContentView(R.layout.login_clever);

    final String title = "Login with Clever";
    final Resources rr = NullCheck.notNull(this.getResources());
    this.feed_initial_uri = NullCheck.notNull(
      rr.getString(
        R.string.feature_catalog_start_uri));

    final String uri = "" + feed_initial_uri + "oauth_authenticate?provider=" + provider + "&redirect_uri=" + redirect_uri + "";


    setTitle(title);
    CleverLoginActivity.LOG.debug("title: {}", title);
    CleverLoginActivity.LOG.debug("uri: {}", uri);


    this.web_view =
      NullCheck.notNull((WebView) this.findViewById(R.id.web_view));


    this.web_view.getSettings().setJavaScriptEnabled(true); // enable javascript

//    final Activity activity = this;

    this.web_view.setWebViewClient(new WebViewClient() {


      @Override
      public boolean shouldOverrideUrlLoading(final WebView view, final String url) {
        if (url.startsWith("open-ebooks-clever")) {
          // Parse the token fom the URL and close the webview

          return true;
        } else {
          return super.shouldOverrideUrlLoading(view, url);
        }
      }


      @Override
      public void onPageFinished(final WebView view, final String url) {


//        Intent intent = getIntent();
//
//        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
//          Uri data = intent.getData();
//          // may be some test here with your custom uri
//          String var = data.getQueryParameter("var"); // "str" is set
//          String varr = data.getQueryParameter("varr"); // "string" is set
//        }

        if (url.startsWith("open-ebooks-clever")) {

          final Uri uri = Uri.parse(url);
          final String fragment = uri.getFragment();

          final StringTokenizer stok = new StringTokenizer(fragment, "&");
          String access_token = null;
          String error = null;
          String patron_info = null;
          JSONObject patron_json = null;
          JSONObject error_json = null;

          while (stok.hasMoreTokens()) {
            final String token = stok.nextToken();
            final StringTokenizer kvpair = new StringTokenizer(token, "=");

            if (kvpair.hasMoreTokens()) {
              final String key = kvpair.nextToken();
              final String value = kvpair.nextToken();
              if (key.equalsIgnoreCase("access_token")) {
                access_token = value;
              } else if (key.equalsIgnoreCase("patron_info")) {
                // json object
                try {
                  patron_info = URLDecoder.decode(value, "UTF-8");

                 patron_json = new JSONObject(patron_info);

                } catch (UnsupportedEncodingException e) {
                  e.printStackTrace();
                } catch (JSONException e) {
                  e.printStackTrace();
                }
              } else if (key.equalsIgnoreCase("error")) {
//                error = value;
                try {
                  error = URLDecoder.decode(value, "UTF-8");
                  error_json = new JSONObject(error);
                } catch (UnsupportedEncodingException e) {
                  e.printStackTrace();
                } catch (JSONException e) {
                  e.printStackTrace();
                }
              }
            }
          }

          if (error == null) {
            final SimplifiedCatalogAppServicesType app =
              Simplified.getCatalogAppServices();


            final Resources rr = NullCheck.notNull(CleverLoginActivity.this.getResources());
            final OptionType<AdobeVendorID> adobe_vendor = Option.some(
              new AdobeVendorID(rr.getString(R.string.feature_adobe_vendor_id)));

            final BooksType books = app.getBooks();

            final AccountBarcode barcode = new AccountBarcode("");
            final AccountPIN pin = new AccountPIN("");
            final AccountAuthToken auth_token = new AccountAuthToken(NullCheck.notNull(access_token));
            final AccountPatron patron = new AccountPatron(patron_info);
            final AccountAdobeToken adobe_token = new AccountAdobeToken("");
            final AccountAuthProvider auth_provider = new AccountAuthProvider("Clever");

            final AccountCredentials creds = new AccountCredentials(adobe_vendor, barcode, pin, auth_provider, Option.some(auth_token), Option.some(adobe_token), Option.some(patron));
            books.accountLogin(creds, CleverLoginActivity.this);


          } else {
            //error display problem.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

              CookieManager.getInstance().removeAllCookies(null);
              CookieManager.getInstance().flush();
            } else {
              CookieSyncManager cookieSyncMngr = CookieSyncManager.createInstance(CleverLoginActivity.this);
              cookieSyncMngr.startSync();
              CookieManager cookieManager = CookieManager.getInstance();
              cookieManager.removeAllCookie();
              cookieManager.removeSessionCookie();
              cookieSyncMngr.stopSync();
              cookieSyncMngr.sync();
            }
            CleverLoginActivity.this.web_view.reload();
          }
        }
      }
    });

    this.web_view.loadUrl(uri);

  }


  /**
   * Set the listener that will be used to receive the results of the login
   * attempt.
   *
   * @param in_listener The listener
   */

  public void setLoginListener(
    final LoginListenerType in_listener) {
    this.listener = NullCheck.notNull(in_listener);
  }

  @Override
  public void onAccountSyncAuthenticationFailure(final String message) {
    // Nothing
  }

  @Override
  public void onAccountSyncBook(final BookID book) {
    // Nothing
  }

  @Override
  public void onAccountSyncFailure(
    final OptionType<Throwable> error,
    final String message) {
    LogUtilities.errorWithOptionalException(CleverLoginActivity.LOG, message, error);
  }

  @Override
  public void onAccountSyncSuccess() {
    // Nothing
  }

  @Override
  public void onAccountSyncBookDeleted(final BookID book) {
    // Nothing
  }

  @Override
  public void onAccountLoginFailureCredentialsIncorrect() {
    CleverLoginActivity.LOG.error("onAccountLoginFailureCredentialsIncorrect");

    final Resources rr = NullCheck.notNull(this.getResources());
    final OptionType<Throwable> none = Option.none();
    this.onAccountLoginFailure(
      none, rr.getString(R.string.settings_login_failed_credentials));
  }

  @Override
  public void onAccountLoginFailureServerError(final int code) {
    CleverLoginActivity.LOG.error(
      "onAccountLoginFailureServerError: {}", code);

    final Resources rr = NullCheck.notNull(this.getResources());
    final OptionType<Throwable> none = Option.none();
    this.onAccountLoginFailure(
      none, rr.getString(R.string.settings_login_failed_server));
  }

  @Override
  public void onAccountLoginFailureLocalError(
    final OptionType<Throwable> error,
    final String message) {
    CleverLoginActivity.LOG.error("onAccountLoginFailureLocalError: {}", message);

    final Resources rr = NullCheck.notNull(this.getResources());
    this.onAccountLoginFailure(
      error, rr.getString(R.string.settings_login_failed_server));
  }

  @Override
  public void onAccountLoginSuccess(
    final AccountCredentials creds) {
    CleverLoginActivity.LOG.debug("login succeeded");


    final Intent result = getIntent();
//    result.putExtra("result", url);
    setResult(1, result);
    finish();


//    final LoginListenerType ls = this.listener;
//    if (ls != null) {
//      try {
//        ls.onLoginSuccess(creds);
//      } catch (final Throwable e) {
//        CleverLoginActivity.LOG.debug("{}", e.getMessage(), e);
//      }
//    }
  }

  @Override
  public void onAccountLoginFailureDeviceActivationError(final String message) {
    CleverLoginActivity.LOG.error(
      "onAccountLoginFailureDeviceActivationError: {}", message);

    final OptionType<Throwable> none = Option.none();
    this.onAccountLoginFailure(
      none, CleverLoginActivity.getDeviceActivationErrorMessage(
        this.getResources(), message));
  }

  private void onAccountLoginFailure(
    final OptionType<Throwable> error,
    final String message) {
    final String s = NullCheck.notNull(
      String.format(
        "login failed: %s", message));

    LogUtilities.errorWithOptionalException(CleverLoginActivity.LOG, s, error);


    final LoginListenerType ls = this.listener;
    if (ls != null) {
      try {
        ls.onLoginFailure(error, message);
      } catch (final Throwable e) {
        CleverLoginActivity.LOG.debug("{}", e.getMessage(), e);
      }
    }
  }

  public static String getDeviceActivationErrorMessage(
    final Resources rr,
    final String message) {
    /**
     * This is absolutely not the way to do this. The nypl-drm-adobe
     * interfaces should be expanded to return values of an enum type. For now,
     * however, these are the only error codes that can be assigned useful
     * messages.
     */

    if (message.startsWith("E_ACT_TOO_MANY_ACTIVATIONS")) {
      return rr.getString(R.string.settings_login_failed_adobe_device_limit);
    } else if (message.startsWith("E_ADEPT_REQUEST_EXPIRED")) {
      return rr.getString(
        R.string.settings_login_failed_adobe_device_bad_clock);
    } else {
      return rr.getString(R.string.settings_login_failed_device);
    }
  }
}
