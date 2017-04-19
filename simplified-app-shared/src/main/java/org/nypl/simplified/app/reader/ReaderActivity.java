package org.nypl.simplified.app.reader;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.ColorMatrixColorFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.io7m.jfunctional.FunctionType;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.io7m.junreachable.UnreachableCodeException;

// Added by Bluefire
import com.sonydadc.urms.android.Urms;
import com.sonydadc.urms.android.UrmsError;
import com.sonydadc.urms.android.api.CreateProfileTask;
import com.sonydadc.urms.android.api.DeleteProfileTask;
import com.sonydadc.urms.android.api.GetOnlineBooksResult;
import com.sonydadc.urms.android.api.GetOnlineBooksTask;
import com.sonydadc.urms.android.api.RegisterBookTask;
import com.sonydadc.urms.android.task.EmptyResponse;
import com.sonydadc.urms.android.task.IFailedCallback;
import com.sonydadc.urms.android.task.IPostExecuteCallback;
import com.sonydadc.urms.android.task.ISucceededCallback;
import com.sonydadc.urms.android.task.IUrmsTask;
import com.sonydadc.urms.android.task.TaskFailedException;
import com.sonydadc.urms.android.task.UrmsTaskStatus;
import com.sonydadc.urms.android.type.UrmsConfig;
// Added by Bluefire


import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.joda.time.Instant;
import org.json.JSONException;
import org.json.JSONObject;
import org.nypl.simplified.app.R;
import org.nypl.simplified.app.Simplified;
import org.nypl.simplified.app.SimplifiedCatalogAppServicesType;
import org.nypl.simplified.app.SimplifiedReaderAppServicesType;
import org.nypl.simplified.app.catalog.annotation.Annotation;
import org.nypl.simplified.app.catalog.annotation.AnnotationResult;
import org.nypl.simplified.app.reader.ReaderPaginationChangedEvent.OpenPage;
import org.nypl.simplified.app.reader.ReaderReadiumViewerSettings.ScrollMode;
import org.nypl.simplified.app.reader.ReaderReadiumViewerSettings.SyntheticSpreadMode;
import org.nypl.simplified.app.reader.ReaderTOC.TOCElement;
import org.nypl.simplified.app.utilities.ErrorDialogUtilities;
import org.nypl.simplified.app.utilities.FadeUtilities;
import org.nypl.simplified.app.utilities.UIThread;
import org.nypl.simplified.books.core.AccountCredentials;
import org.nypl.simplified.books.core.AccountGetCachedCredentialsListenerType;
import org.nypl.simplified.books.core.BookID;
import org.nypl.simplified.books.core.BooksType;
import org.nypl.simplified.books.core.FeedEntryOPDS;
import org.nypl.simplified.books.core.LogUtilities;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.nypl.simplified.volley.NYPLStringRequest;
import org.readium.sdk.android.Container;
import org.readium.sdk.android.Package;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import bclurms.UrmsCreateProfileRequest;
import bclurms.UrmsEvaluateLicenseRequest;
import bclurms.UrmsInitializer;
import bclurms.UrmsRegisterBookRequest;

import static android.content.ContentValues.TAG;

/**
 * The main reader activity for reading an EPUB.
 */

public final class ReaderActivity extends Activity implements
  ReaderHTTPServerStartListenerType,
  ReaderSimplifiedFeedbackListenerType,
  ReaderReadiumFeedbackListenerType,
  ReaderReadiumEPUBLoadListenerType,
  ReaderCurrentPageListenerType,
  ReaderTOCSelectionListenerType,
  ReaderSettingsListenerType,
  ReaderMediaOverlayAvailabilityListenerType
{
  private static final String BOOK_ID;
  private static final String FILE_ID;
  private static final String ENTRY;
  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(ReaderActivity.class);
  }

  static {
    BOOK_ID = "org.nypl.simplified.app.ReaderActivity.book";
    FILE_ID = "org.nypl.simplified.app.ReaderActivity.file";
    ENTRY = "org.nypl.simplified.app.ReaderActivity.entry";
  }

  private @Nullable BookID                            book_id;
  private @Nullable FeedEntryOPDS                     entry;
  private @Nullable Container                         epub_container;
  private @Nullable ReaderReadiumJavaScriptAPIType    readium_js_api;
  private @Nullable ReaderSimplifiedJavaScriptAPIType simplified_js_api;
  private @Nullable ViewGroup                         view_hud;
  private @Nullable ProgressBar                       view_loading;
  private @Nullable ViewGroup                         view_media;
  private @Nullable ImageView                         view_media_next;
  private @Nullable ImageView                         view_media_play;
  private @Nullable ImageView                         view_media_prev;
  private @Nullable ProgressBar                       view_progress_bar;
  private @Nullable TextView                          view_progress_text;
  private @Nullable View                              view_root;
  private @Nullable ImageView                         view_settings;
  private @Nullable TextView                          view_title_text;
  private @Nullable ImageView                         view_toc;
  private @Nullable WebView                           view_web_view;
  private @Nullable ReaderReadiumViewerSettings       viewer_settings;
  private           boolean                           web_view_resized;
  private           ReaderBookLocation                current_location;
  private           ReaderBookLocation                sync_location;
  private           AccountCredentials                credentials;
  /**
   * Construct an activity.
   */


  // Added by Bluefire
  private static final String TAG = "ReaderActivity";
  // End Added by Bluefire


  public ReaderActivity()
  {

  }

  /**
   * Start a new reader for the given book.
   *
   * @param from The parent activity
   * @param book The unique ID of the book
   * @param file The actual EPUB file
   * @param entry The OPD feed entry
   */

  public static void startActivity(
    final Activity from,
    final BookID book,
    final File file,
    final FeedEntryOPDS entry)
  {
    NullCheck.notNull(file);
    final Bundle b = new Bundle();
    b.putSerializable(ReaderActivity.BOOK_ID, book);
    b.putSerializable(ReaderActivity.FILE_ID, file);
    b.putSerializable(ReaderActivity.ENTRY, entry);
    final Intent i = new Intent(from, ReaderActivity.class);
    i.putExtras(b);
    from.startActivity(i);
  }

  private void applyViewerColorFilters()
  {
    ReaderActivity.LOG.debug("applying color filters");

    final TextView in_progress_text =
      NullCheck.notNull(this.view_progress_text);
    final TextView in_title_text = NullCheck.notNull(this.view_title_text);
    final ImageView in_toc = NullCheck.notNull(this.view_toc);
    final ImageView in_settings = NullCheck.notNull(this.view_settings);
    final ImageView in_media_play = NullCheck.notNull(this.view_media_play);
    final ImageView in_media_next = NullCheck.notNull(this.view_media_next);
    final ImageView in_media_prev = NullCheck.notNull(this.view_media_prev);

    final int main_color = Color.parseColor(Simplified.getCurrentAccount().getMainColor());
    final ColorMatrixColorFilter filter =
      ReaderColorMatrix.getImageFilterMatrix(main_color);

    UIThread.runOnUIThread(
            new Runnable() {
              @Override
              public void run() {
                in_progress_text.setTextColor(main_color);
                in_title_text.setTextColor(main_color);
                in_toc.setColorFilter(filter);
                in_settings.setColorFilter(filter);
                in_media_play.setColorFilter(filter);
                in_media_next.setColorFilter(filter);
                in_media_prev.setColorFilter(filter);
              }
            });
  }

  /**
   * Apply the given color scheme to all views. Unfortunately, there does not
   * seem to be a more pleasant way, in the Android API, than manually applying
   * values to all of the views in the hierarchy.
   */

  private void applyViewerColorScheme(
    final ReaderColorScheme cs)
  {
    ReaderActivity.LOG.debug("applying color scheme");

    final View in_root = NullCheck.notNull(this.view_root);
    UIThread.runOnUIThread(
            new Runnable() {
              @Override
              public void run() {
                in_root.setBackgroundColor(cs.getBackgroundColor());
                ReaderActivity.this.applyViewerColorFilters();
              }
            });
  }

  private void makeInitialReadiumRequest(
    final ReaderHTTPServerType hs)
  {
    final URI reader_uri = URI.create(hs.getURIBase() + "reader.html");
    final WebView wv = NullCheck.notNull(this.view_web_view);
    UIThread.runOnUIThread(
            new Runnable() {
              @Override
              public void run() {
                ReaderActivity.LOG.debug(
                        "making initial reader request: {}", reader_uri);
                wv.loadUrl(reader_uri.toString());
              }
            });
  }

  @Override protected void onActivityResult(
    final int request_code,
    final int result_code,
    final @Nullable Intent data)
  {
    super.onActivityResult(request_code, result_code, data);

    ReaderActivity.LOG.debug(
            "onActivityResult: {} {} {}", request_code, result_code, data);

    if (request_code == ReaderTOCActivity.TOC_SELECTION_REQUEST_CODE) {
      if (result_code == Activity.RESULT_OK) {
        final Intent nnd = NullCheck.notNull(data);
        final Bundle b = NullCheck.notNull(nnd.getExtras());
        final TOCElement e = NullCheck.notNull(
          (TOCElement) b.getSerializable(
            ReaderTOCActivity.TOC_SELECTED_ID));
        this.onTOCSelectionReceived(e);
      }
    }
  }

  @Override public void onConfigurationChanged(
    final @Nullable Configuration c)
  {
    super.onConfigurationChanged(c);

    ReaderActivity.LOG.debug("configuration changed");

    final WebView in_web_view = NullCheck.notNull(this.view_web_view);
    final TextView in_progress_text =
      NullCheck.notNull(this.view_progress_text);
    final ProgressBar in_progress_bar =
      NullCheck.notNull(this.view_progress_bar);

    in_web_view.setVisibility(View.INVISIBLE);
    in_progress_text.setVisibility(View.INVISIBLE);
    in_progress_bar.setVisibility(View.INVISIBLE);

    this.web_view_resized = true;
    UIThread.runOnUIThreadDelayed(
            new Runnable() {
              @Override
              public void run() {
                final ReaderReadiumJavaScriptAPIType readium_js =
                        NullCheck.notNull(ReaderActivity.this.readium_js_api);
                readium_js.getCurrentPage(ReaderActivity.this);
                readium_js.mediaOverlayIsAvailable(ReaderActivity.this);
              }
            }, 300L);
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (Simplified.getSharedPrefs().getBoolean("setting_sync_last_read")) {
      this.syncLastRead();
    }
  }

  @Override protected void onCreate(
    final @Nullable Bundle state)
  {
    final int id = Simplified.getCurrentAccount().getId();
    if (id == 0) {
      setTheme(R.style.SimplifiedThemeNoActionBar_NYPL);
    }
    else if (id == 1) {
      setTheme(R.style.SimplifiedThemeNoActionBar_BPL);
    }
    else {
      setTheme(R.style.SimplifiedThemeNoActionBar);
    }

    super.onCreate(state);
    this.setContentView(R.layout.reader);

    ReaderActivity.LOG.debug("starting");

    final Intent i = NullCheck.notNull(this.getIntent());
    final Bundle a = NullCheck.notNull(i.getExtras());




    // Bluefire Added
    this.book_id =
            NullCheck.notNull((BookID) a.getSerializable(ReaderActivity.BOOK_ID));
    this.entry =
            NullCheck.notNull((FeedEntryOPDS) a.getSerializable(ReaderActivity.ENTRY));

    ReaderActivity.LOG.debug("ReaderActivity - before reading file from Assets and writing to internal storage");

    // Read file from Assets and write to internal storage
    File f = new File(getCacheDir() + "/sample.epub");
    if (!f.exists()) try {
      InputStream is = getAssets().open("sample.epub");
      int size = is.available();
      byte[] buffer = new byte[size];
      is.read(buffer);
      is.close();
      FileOutputStream fos = new FileOutputStream(f);
      fos.write(buffer);
      fos.close();
    } catch (Exception e) { throw new RuntimeException(e); }
    final File in_epub_file = new File(getCacheDir(), "sample.epub");


    ReaderActivity.LOG.debug("ReaderActivity before evaluateURMSLicense called");
    final String bookCCID = "NHG6M6VG63D4DQKJMC986FYFDG5MDQJE";
    final String bookUri = in_epub_file.getAbsolutePath();
    ReaderActivity.LOG.debug("ReaderActivity - bookUri:  {}", bookUri);


    ReaderActivity.LOG.debug("ReaderActivity - Requesting authToken…");


    Thread thread = new Thread(new Runnable() {

      @Override
      public void run() {
        try  {


          ReaderActivity.LOG.debug("ReaderActivity - Thread running");

          String userID = "google-110495186711904557779";
          String path = "/store/v2/users/" + userID + "/authtoken/generate";
          String sessionURL = "http://urms-967957035.eu-west-1.elb.amazonaws.com" + path;
          String timestamp = Long.toString(System.currentTimeMillis() / 1000);
          String hmacMessage = path + timestamp;
          String secretKey = "ucj0z3uthspfixtba5kmwewdgl7s1prm";

          ReaderActivity.LOG.debug("ReaderActivity - hmacMessage: {} ", hmacMessage);



          String base_string = hmacMessage;
          String key = secretKey;
          String authHash = "";
          try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret = new SecretKeySpec(key.getBytes("UTF-8"), mac.getAlgorithm());
            mac.init(secret);
            byte[] digest = mac.doFinal(base_string.getBytes());
            authHash = Base64.encodeToString(digest, Base64.DEFAULT); // Base 64 Encode the results
            Log.v(TAG, "String: " + base_string);
            Log.v(TAG, "key: " + key);
            Log.v(TAG, "authHash: " + authHash);
          } catch (Exception e) {
            System.out.println(e.getMessage());
          }

          authHash = authHash.replaceAll("(\\r|\\n)", "");
          authHash = authHash.replaceAll(Pattern.quote("+"), "%2B");
//          authHash = authHash.replaceAll(Pattern.quote("/"), "_");


//          byte[] hmac = hashMac(hmacMessage, secretKey);
//          ReaderActivity.LOG.debug("ReaderActivity - hmac: {} ", hmac);

//          String authHash = Base64.encodeToString(hmac, Base64.DEFAULT);
//          ReaderActivity.LOG.debug("ReaderActivity - authHash: {} ", authHash);

          String storeID = "129";
          String authString = storeID + "-" + timestamp + "-" + authHash;
          ReaderActivity.LOG.debug("ReaderActivity - authString: {} ", authString);


          // Create a new HttpClient and Post Header
          HttpClient httpclient = new DefaultHttpClient();

//          HttpHost httpproxy = new HttpHost("192.168.1.154", 8888, "http");
//          httpclient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY,httpproxy);




          HttpPost httppost = new HttpPost(sessionURL);


          try {
            // Set headers
            httppost.setHeader("Content-Type", "application/x-www-form-urlencoded");

            // Data to POST
//            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
//            nameValuePairs.add(new BasicNameValuePair("authString", authString));
//            nameValuePairs.add(new BasicNameValuePair("timestamp", timestamp));

            // Set content length header
//            UrlEncodedFormEntity urlEncodedFormEntity = new UrlEncodedFormEntity(nameValuePairs);

            // Set data body
            String dataBody = "authString=" + authString + "&timestamp=" + timestamp;
            StringEntity stringEntity = new StringEntity(dataBody, HTTP.UTF_8);
            stringEntity.setContentEncoding(HTTP.UTF_8);
            httppost.setEntity(stringEntity);

//            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
//            nameValuePairs.add(new BasicNameValuePair("authString", authString));
//            nameValuePairs.add(new BasicNameValuePair("timestamp", timestamp));
//            UrlEncodedFormEntity urlEncodedFormEntity = new UrlEncodedFormEntity(nameValuePairs, "UTF-8");
//            urlEncodedFormEntity.setContentEncoding("UTF-8");
//            long urlEncodedFormEntityLength = urlEncodedFormEntity.getContentLength();
//            httppost.setHeader("Content-Length", Long.toString(urlEncodedFormEntityLength));


//            ReaderActivity.LOG.debug("ReaderActivity - urlEncodedFormEntity: {} ", urlEncodedFormEntity.toString());
//            ReaderActivity.LOG.debug("ReaderActivity - urlEncodedFormEntity.getContent(): {} ", urlEncodedFormEntity.getContent());
//            ReaderActivity.LOG.debug("ReaderActivity - urlEncodedFormEntity.getContentType(): {} ", urlEncodedFormEntity.getContentType());
//            ReaderActivity.LOG.debug("ReaderActivity - urlEncodedFormEntity.getContentEncoding(): {} ", urlEncodedFormEntity.getContentEncoding());
//            ReaderActivity.LOG.debug("ReaderActivity - urlEncodedFormEntity.getContentLength(): {} ", urlEncodedFormEntity.getContentLength());
//            httppost.setEntity(urlEncodedFormEntity);


            // Execute HTTP Post Request

            ReaderActivity.LOG.debug("ReaderActivity - Executing HTTP POST request…");
            HttpResponse httpResponse = httpclient.execute(httppost);
            ReaderActivity.LOG.debug("ReaderActivity - Getting response entity…");
            HttpEntity responseEntity = httpResponse.getEntity();
            String response = "";
            if(responseEntity != null) {
              response = EntityUtils.toString(responseEntity);
              ReaderActivity.LOG.debug("ReaderActivity - Response is: {}", response);

            }


            JSONObject responseJson;
            try {
              responseJson = new JSONObject(response);
              final String authToken = responseJson.getString("authToken");
              ReaderActivity.LOG.debug("ReaderActivity - authToken: {}", authToken);

//             final String profileName = "default";
             final String profileName = "test_profile_april_13_2017_d";
//             Urms.reset();

              final UrmsConfig config = new UrmsConfig(
                      "https://urms-sdk.codefusion.technology/sdk/",			// cgp.api
                      "https://urms-marlin-us.codefusion.technology/bks/",	// marlin.api
                      "urn:marlin:organization:sne:service-provider:2",		// marlin.service_id
                      true 													// marlin.use_ssl
              );

              runOnUiThread(new Runnable() {
                public void run() {

//                  boolean deleteProfileSucceeded = Urms.createDeleteProfileTask(profileName).execute();
//
//                  if (deleteProfileSucceeded) {
//                    Log.d("ReaderActivity", "Profile deleted: " + profileName);
//                  } else {
//                    Log.d("ReaderActivity", "Error deleting profile: " + profileName);
//                  }

                  Log.d("ReaderActivity", "Token: " + authToken);

                  Urms.createCreateProfileTask(authToken, profileName, null, config).setSucceededCallback(new ISucceededCallback<EmptyResponse>() {
                    @Override
                    public void onSucceeded(IUrmsTask task, EmptyResponse result) {
                      Log.d("ReaderActivity", "Success creating profile.");


                      List<String> profiles = null;
                      try {
                        profiles = Urms.createGetProfilesTask().getResultWithExecute().getProfiles();
                        if (profiles.size() > 0) {
                          Log.d("ReaderActivity", "Profiles size: " + profiles.size());
                          Log.d("ReaderActivity", "Profiles: " + profiles.toString());
                          Urms.createSwitchProfileTask(profiles.get(0)).getResultWithExecute();
                          Log.d(TAG, "Switched to profile: " + profiles.get(0));
                          Log.d(TAG, "Evaluating URMS license…");
                          evaluateURMSLicense(bookCCID, bookUri, getApplicationContext(), a, in_epub_file);
                        }
                      } catch (TaskFailedException e) {
                        e.printStackTrace();
                      }
                    }
                  }).setFailedCallback(new IFailedCallback() {
                    @Override
                    public void onFailed(IUrmsTask task, UrmsTaskStatus status, UrmsError error) {
                      Log.d("ReaderActivity", "Failed creating profile.");

                      if (error.getErrorType() == UrmsError.RegisterUserDeviceCapacityReached) {
                        Log.e(TAG, "RegisterUserDeviceCapacityReached");
                      } else if (error.getErrorType() == UrmsError.NetworkError ||
                              error.getErrorType() == UrmsError.NetworkTimeout) {
                        Log.e(TAG, "Network error or network timeout.");
                      } else if (error.getErrorType() == UrmsError.UrmsNotInitialized) {
                        Log.e(TAG, "URMS not initialized.");
                      } else if (error.getErrorType() == UrmsError.LoseTime) {
                        Log.e(TAG, "Please ensure the time on your device is correct.");
                      } else if (error.getErrorType() == UrmsError.OutdatedVersion) {
                        Log.e(TAG, "Outdated version");
                      } else if (error.getErrorCode().endsWith("04")) {
                        Log.e(TAG, "Potential server/client configuration mismatch.");
                      } else {
                        Log.e(TAG, "Create Profile failed: Other error: " + error.getErrorCode());
                        Log.e(TAG, "Create Profile Error type: " + new Integer(error.getErrorType()).toString());
                      }

                      Log.d(TAG, "Create Profile failed, getting profiles…");

                      List<String> profiles = null;
                      try {
                        profiles = Urms.createGetProfilesTask().getResultWithExecute().getProfiles();
                        if (profiles.size() > 0) {
                          Log.d("ReaderActivity", "Profiles size: " + profiles.size());
                          Log.d("ReaderActivity", "Profiles: " + profiles.toString());
                          Urms.createSwitchProfileTask(profiles.get(0)).getResultWithExecute();
                          Log.d(TAG, "Switched to profile: " + profiles.get(0));
                          Log.d(TAG, "Evaluating URMS license…");
                          evaluateURMSLicense(bookCCID, bookUri, getApplicationContext(), a, in_epub_file);
                        }
                      } catch (TaskFailedException e) {
                        e.printStackTrace();
                      }


                    }
                  }).executeAsync();
                }
              });// End runnable


            } catch (JSONException e) {
              e.printStackTrace();
            }



          } catch (ClientProtocolException e) {
             e.printStackTrace();
          } catch (IOException e) {
             e.printStackTrace();
          }


        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });

    thread.start();
  }



  public void evaluateURMSLicense(final String bookCCID, final String bookUri, final Context mContext, final Bundle bundle, final File in_epub_file) {
    Log.e(TAG, "[evaluateURMSLicense] bookCCID = " + bookCCID);


    if(!Urms.createEvaluateLicenseTask(bookCCID).execute()) {
      Log.e(TAG, "[evaluateURMSLicense] Evaluate URMS license failed. Registering book...");
      Log.e(TAG, "[evaluateURMSLicense] First calling getOnlineBookTask…");

      GetOnlineBooksTask gobt = Urms.createGetOnlineBooksTask();
      gobt.setSucceededCallback(new ISucceededCallback<GetOnlineBooksResult>() {
        @Override
        public void onSucceeded(IUrmsTask task, GetOnlineBooksResult result) {
          Log.e(TAG, "[evaluateURMSLicense] getOnlineBookTask succeeded, registering book:");

          RegisterBookTask rbt = Urms.createRegisterBookTask(bookCCID);
          rbt.setPostExecuteCallback(new IPostExecuteCallback() {
            @Override
            public void onPostExecute(IUrmsTask task) {

              Log.e(TAG, "Register book task - on post execute.");
              evaluateURMSLicense(bookCCID, bookUri, mContext, bundle, in_epub_file); // Call self after registerBookTask succeeds, to evaluate again


            }
          });
          rbt.setDestination(in_epub_file);

          rbt.executeAsync();
//      Urms.executeBackground(rbt);
          Log.e(TAG, "Register book task executed.");


        }
      });
      gobt.executeAsync();

    } else {
      Log.e(TAG, "No error from evaluateURMSLicense. Opening book.");
      // End Bluefire Added


      ReaderActivity.LOG.debug("epub file: {}", in_epub_file);
      ReaderActivity.LOG.debug("book id:   {}", this.book_id);
      ReaderActivity.LOG.debug("entry id:   {}", this.entry.getFeedEntry().getID());

      final SimplifiedReaderAppServicesType rs =
              Simplified.getReaderAppServices();

      final ReaderSettingsType settings = rs.getSettings();
      settings.addListener(this);

      this.viewer_settings = new ReaderReadiumViewerSettings(
              SyntheticSpreadMode.SINGLE, ScrollMode.AUTO, (int) settings.getFontScale(), 20);

      final ReaderReadiumFeedbackDispatcherType rd =
              ReaderReadiumFeedbackDispatcher.newDispatcher();
      final ReaderSimplifiedFeedbackDispatcherType sd =
              ReaderSimplifiedFeedbackDispatcher.newDispatcher();

      final ViewGroup in_hud = NullCheck.notNull(
              (ViewGroup) this.findViewById(
                      R.id.reader_hud_container));
      final ImageView in_toc =
              NullCheck.notNull((ImageView) in_hud.findViewById(R.id.reader_toc));
      final ImageView in_settings =
              NullCheck.notNull((ImageView) in_hud.findViewById(R.id.reader_settings));
      final TextView in_title_text =
              NullCheck.notNull((TextView) in_hud.findViewById(R.id.reader_title_text));
      final TextView in_progress_text = NullCheck.notNull(
              (TextView) in_hud.findViewById(
                      R.id.reader_position_text));
      final ProgressBar in_progress_bar = NullCheck.notNull(
              (ProgressBar) in_hud.findViewById(
                      R.id.reader_position_progress));

      final ViewGroup in_media_overlay =
              NullCheck.notNull((ViewGroup) this.findViewById(R.id.reader_hud_media));
      final ImageView in_media_previous = NullCheck.notNull(
              (ImageView) this.findViewById(
                      R.id.reader_hud_media_previous));
      final ImageView in_media_next = NullCheck.notNull(
              (ImageView) this.findViewById(
                      R.id.reader_hud_media_next));
      final ImageView in_media_play = NullCheck.notNull(
              (ImageView) this.findViewById(
                      R.id.reader_hud_media_play));

      final ProgressBar in_loading =
              NullCheck.notNull((ProgressBar) this.findViewById(R.id.reader_loading));
      final WebView in_webview =
              NullCheck.notNull((WebView) this.findViewById(R.id.reader_webview));

      this.view_root = NullCheck.notNull(in_hud.getRootView());

      in_loading.setVisibility(View.VISIBLE);
      in_progress_bar.setVisibility(View.INVISIBLE);
      in_progress_text.setVisibility(View.INVISIBLE);
      in_webview.setVisibility(View.INVISIBLE);
      in_hud.setVisibility(View.VISIBLE);
      in_media_overlay.setVisibility(View.INVISIBLE);

      in_settings.setOnClickListener(
              new OnClickListener() {
                @Override
                public void onClick(
                        final @Nullable View v) {
                  final FragmentManager fm = ReaderActivity.this.getFragmentManager();
                  final ReaderSettingsDialog d = new ReaderSettingsDialog();
                  d.show(fm, "settings-dialog");
                }
              });

      this.view_loading = in_loading;
      this.view_progress_text = in_progress_text;
      this.view_progress_bar = in_progress_bar;
      this.view_title_text = in_title_text;
      this.view_web_view = in_webview;
      this.view_hud = in_hud;
      this.view_toc = in_toc;
      this.view_settings = in_settings;
      this.web_view_resized = true;
      this.view_media = in_media_overlay;
      this.view_media_next = in_media_next;
      this.view_media_prev = in_media_previous;
      this.view_media_play = in_media_play;

      final WebChromeClient wc_client = new WebChromeClient() {
        @Override
        public void onShowCustomView(
                final @Nullable View view,
                final @Nullable CustomViewCallback callback) {
          super.onShowCustomView(view, callback);
          ReaderActivity.LOG.debug("web-chrome: {}", view);
        }
      };

      final WebViewClient wv_client =
              new ReaderWebViewClient(this, sd, this, rd, this);
      in_webview.setBackgroundColor(0x00000000);
      in_webview.setWebChromeClient(wc_client);
      in_webview.setWebViewClient(wv_client);
      in_webview.setOnLongClickListener(
              new OnLongClickListener() {
                @Override
                public boolean onLongClick(
                        final @Nullable View v) {
                  ReaderActivity.LOG.debug("ignoring long click on web view");
                  return true;
                }
              });

      // Allow the webview to be debuggable only if this is a dev build
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        if ((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
          WebView.setWebContentsDebuggingEnabled(true);
        }
      }

      final WebSettings s = NullCheck.notNull(in_webview.getSettings());
      s.setAppCacheEnabled(false);
      s.setAllowFileAccess(false);
      s.setAllowFileAccessFromFileURLs(false);
      s.setAllowContentAccess(false);
      s.setAllowUniversalAccessFromFileURLs(false);
      s.setSupportMultipleWindows(false);
      s.setCacheMode(WebSettings.LOAD_NO_CACHE);
      s.setGeolocationEnabled(false);
      s.setJavaScriptEnabled(true);

      this.readium_js_api = ReaderReadiumJavaScriptAPI.newAPI(in_webview);
      this.simplified_js_api = ReaderSimplifiedJavaScriptAPI.newAPI(in_webview);

      in_title_text.setText("");

      final ReaderReadiumEPUBLoaderType pl = rs.getEPUBLoader();
      pl.loadEPUB(in_epub_file, this);

      this.applyViewerColorFilters();

      final SimplifiedCatalogAppServicesType app =
              Simplified.getCatalogAppServices();

      final BooksType books = app.getBooks();

      books.accountGetCachedLoginDetails(
              new AccountGetCachedCredentialsListenerType() {
                @Override
                public void onAccountIsNotLoggedIn() {
                  throw new UnreachableCodeException();
                }

                @Override
                public void onAccountIsLoggedIn(
                        final AccountCredentials creds) {

                  ReaderActivity.this.credentials = creds;

                }
              }
      );
    }
  // Bluefire added
  }
  // Bluefire added

  @Override public void onCurrentPageError(
    final Throwable x)
  {
    ReaderActivity.LOG.error("{}", x.getMessage(), x);
  }

  @Override public void onCurrentPageReceived(
    final ReaderBookLocation l)
  {
    ReaderActivity.LOG.debug("received book location: {}", l);

    if (Simplified.getSharedPrefs().getBoolean("setting_sync_last_read")) {

      LOG.debug("CurrentPage prefs {}", Simplified.getSharedPrefs().getBoolean("post_last_read"));

      if (Simplified.getSharedPrefs().getBoolean("post_last_read")) {

        this.postLastRead(l);

      }
    }
    else
    {
      final SimplifiedReaderAppServicesType rs =
        Simplified.getReaderAppServices();
      final ReaderBookmarksType bm = rs.getBookmarks();
      final BookID in_book_id = NullCheck.notNull(this.book_id);

      bm.setBookmark(in_book_id, l);

    }
  }

  @Override protected void onDestroy()
  {
    super.onDestroy();

    final ReaderReadiumJavaScriptAPIType readium_js =
      NullCheck.notNull(ReaderActivity.this.readium_js_api);
    readium_js.getCurrentPage(ReaderActivity.this);
    readium_js.mediaOverlayIsAvailable(ReaderActivity.this);

    final SimplifiedReaderAppServicesType rs =
      Simplified.getReaderAppServices();

    final ReaderSettingsType settings = rs.getSettings();
    settings.removeListener(this);
//    System.exit(0);
  }

  @Override public void onEPUBLoadFailed(
    final Throwable x)
  {
    ErrorDialogUtilities.showErrorWithRunnable(
            this, ReaderActivity.LOG, "Could not load EPUB file", x, new Runnable() {
              @Override
              public void run() {
                ReaderActivity.this.finish();
              }
            });
  }

  @Override public void onEPUBLoadSucceeded(
    final Container c)
  {
    this.epub_container = c;
    final Package p = NullCheck.notNull(c.getDefaultPackage());

    final TextView in_title_text = NullCheck.notNull(this.view_title_text);
    UIThread.runOnUIThread(
      new Runnable()
      {
        @Override public void run()
        {
          in_title_text.setText(NullCheck.notNull(p.getTitle()));
        }
      });

    /**
     * Configure the TOC button.
     */

    final SimplifiedReaderAppServicesType rs =
      Simplified.getReaderAppServices();

    final View in_toc = NullCheck.notNull(this.view_toc);

    in_toc.setOnClickListener(
            new OnClickListener() {
              @Override
              public void onClick(
                      final @Nullable View v) {
                final ReaderTOC sent_toc = ReaderTOC.fromPackage(p);
                ReaderTOCActivity.startActivityForResult(
                        ReaderActivity.this, sent_toc);
                ReaderActivity.this.overridePendingTransition(0, 0);
              }
            });

    /**
     * Get a reference to the web server. Start it if necessary (the callbacks
     * will still be executed if the server is already running).
     */

    final ReaderHTTPServerType hs = rs.getHTTPServer();
    hs.startIfNecessaryForPackage(p, this);
  }

  @Override public void onMediaOverlayIsAvailable(
    final boolean available)
  {
    ReaderActivity.LOG.debug(
            "media overlay status changed: available: {}", available);

    final ViewGroup in_media_hud = NullCheck.notNull(this.view_media);
    final TextView in_title = NullCheck.notNull(this.view_title_text);
    UIThread.runOnUIThread(
            new Runnable() {
              @Override
              public void run() {
                in_media_hud.setVisibility(available ? View.VISIBLE : View.GONE);
                in_title.setVisibility(available ? View.GONE : View.VISIBLE);
              }
            });
  }

  @Override public void onMediaOverlayIsAvailableError(
    final Throwable x)
  {
    ReaderActivity.LOG.error("{}", x.getMessage(), x);
  }

  @Override public void onReaderSettingsChanged(
    final ReaderSettingsType s)
  {
    ReaderActivity.LOG.debug("reader settings changed");

    final ReaderReadiumJavaScriptAPIType js =
      NullCheck.notNull(this.readium_js_api);
    js.setPageStyleSettings(s);

    final ReaderColorScheme cs = s.getColorScheme();
    this.applyViewerColorScheme(cs);

    UIThread.runOnUIThreadDelayed(
            new Runnable() {
              @Override
              public void run() {
                final ReaderReadiumJavaScriptAPIType readium_js =
                        NullCheck.notNull(ReaderActivity.this.readium_js_api);
                readium_js.getCurrentPage(ReaderActivity.this);
                readium_js.mediaOverlayIsAvailable(ReaderActivity.this);
              }
            }, 300L);
  }

  @Override public void onReadiumFunctionDispatchError(
    final Throwable x)
  {
    ReaderActivity.LOG.error("{}", x.getMessage(), x);
  }

  private void showBookLocationDialog(final String response) {

    final AlertDialog.Builder builder = new AlertDialog.Builder(ReaderActivity.this);
    builder.setTitle("Sync Reading Position");

    if (ReaderActivity.this.epub_container != null) {
      final Container container = NullCheck.notNull(ReaderActivity.this.epub_container);

      final Package default_package = NullCheck.notNull(container.getDefaultPackage());
      final AnnotationResult result = new Gson().fromJson(response, AnnotationResult.class);
      OptionType<ReaderOpenPageRequestType> page_request = null;

      for (final Annotation annotation : result.getFirst().getItems()) {
        if ("http://librarysimplified.org/terms/annotation/idling".equals(annotation.getMotivation())) {

          final String text = NullCheck.notNull(annotation.getTarget().getSelector().getValue());
          LOG.debug("CurrentPage text {}", text);

          final String key = NullCheck.notNull(this.book_id.toString());
          LOG.debug("CurrentPage key {}", key);

          try {
            final JSONObject o = new JSONObject(text);

            final OptionType<ReaderBookLocation> mark = Option.some(ReaderBookLocation.fromJSON(o));

            page_request = mark.map(
              new FunctionType<ReaderBookLocation, ReaderOpenPageRequestType>() {
                @Override
                public ReaderOpenPageRequestType call(
                  final ReaderBookLocation l) {
                  LOG.debug("CurrentPage location {}", l);

                  final String chapter = default_package.getSpineItem(l.getIDRef()).getTitle();
                  builder.setMessage("Would you like to go to the latest page read? \n\nChapter:\n\" " + chapter + "\"");

                  ReaderActivity.this.sync_location = l;
                  return ReaderOpenPageRequest.fromBookLocation(l);
                }
              });


            LOG.debug("CurrentPage sync {}", text);

          } catch (JSONException e) {
            e.printStackTrace();
          }

        }
      }

      final OptionType<ReaderOpenPageRequestType> page = page_request;

      builder.setPositiveButton("YES",
        new DialogInterface.OnClickListener() {
          @Override
          public void onClick(final DialogInterface dialog, final int which) {
            // positive button logic

            final ReaderReadiumJavaScriptAPIType js =
              NullCheck.notNull(ReaderActivity.this.readium_js_api);
            final ReaderReadiumViewerSettings vs =
              NullCheck.notNull(ReaderActivity.this.viewer_settings);
            final Container c = NullCheck.notNull(ReaderActivity.this.epub_container);
            final Package p = NullCheck.notNull(c.getDefaultPackage());

            js.openBook(p, vs, page);
            Simplified.getSharedPrefs().putBoolean("post_last_read", true);
            LOG.debug("CurrentPage set prefs {}", Simplified.getSharedPrefs().getBoolean("post_last_read"));
          }
        });

      builder.setNegativeButton("NO",
        new DialogInterface.OnClickListener() {
          @Override
          public void onClick(final DialogInterface dialog, final int which) {
            // negative button logic
            Simplified.getSharedPrefs().putBoolean("post_last_read", true);
            LOG.debug("CurrentPage set prefs {}", Simplified.getSharedPrefs().getBoolean("post_last_read"));

          }
        });

      LOG.debug("CurrentPage current_location {}", this.current_location);
      LOG.debug("CurrentPage sync_location {}", this.sync_location);
    }

    if ((this.current_location == null && this.sync_location == null) || this.current_location != null && this.sync_location == null)
    {
      Simplified.getSharedPrefs().putBoolean("post_last_read", true);
      LOG.debug("CurrentPage set prefs {}", Simplified.getSharedPrefs().getBoolean("post_last_read"));
    }
    else if (this.current_location == null && this.sync_location != null)
    {
      final AlertDialog dialog = builder.create();
      dialog.show();
    }
    else if (!(this.current_location.getIDRef().equals(this.sync_location.getIDRef()) && this.current_location.getContentCFI().equals(this.sync_location.getContentCFI())))
    {
      final AlertDialog dialog = builder.create();
      dialog.show();
    }
    else
    {
      Simplified.getSharedPrefs().putBoolean("post_last_read", true);
      LOG.debug("CurrentPage set prefs {}", Simplified.getSharedPrefs().getBoolean("post_last_read"));
    }

  }

  @Override public void onReadiumFunctionInitialize()
  {
    ReaderActivity.LOG.debug("readium initialize requested");

    final SimplifiedReaderAppServicesType rs =
      Simplified.getReaderAppServices();

    final ReaderHTTPServerType hs = rs.getHTTPServer();
    final Container c = NullCheck.notNull(this.epub_container);
    final Package p = NullCheck.notNull(c.getDefaultPackage());
    p.setRootUrls(hs.getURIBase().toString(), null);

    final ReaderReadiumViewerSettings vs =
      NullCheck.notNull(this.viewer_settings);
    final ReaderReadiumJavaScriptAPIType js =
      NullCheck.notNull(this.readium_js_api);

    /**
     * If there's a bookmark for the current book, send a request to open the
     * book to that specific page. Otherwise, start at the beginning.
     */

    final BookID in_book_id = NullCheck.notNull(this.book_id);

    final OPDSAcquisitionFeedEntry in_entry = NullCheck.notNull(this.entry.getFeedEntry());

    final ReaderBookmarksType bookmarks = rs.getBookmarks();
    final OptionType<ReaderBookLocation> mark =
      bookmarks.getBookmark(in_book_id, in_entry);
    
    final OptionType<ReaderOpenPageRequestType> page_request = mark.map(
      new FunctionType<ReaderBookLocation, ReaderOpenPageRequestType>()
      {
        @Override public ReaderOpenPageRequestType call(
          final ReaderBookLocation l)
        {
          ReaderActivity.this.current_location = l;
          return ReaderOpenPageRequest.fromBookLocation(l);
        }
      });
    // is this correct? inject fonts before book opens or after
    js.injectFonts();

    // open book with page request, vs = view settings, p = package , what is package actually ? page_request = idref + contentcfi
    js.openBook(p, vs, page_request);

    /**
     * Configure the visibility of UI elements.
     */

    final WebView in_web_view = NullCheck.notNull(this.view_web_view);
    final ProgressBar in_loading = NullCheck.notNull(this.view_loading);
    final ProgressBar in_progress_bar =
      NullCheck.notNull(this.view_progress_bar);
    final TextView in_progress_text =
      NullCheck.notNull(this.view_progress_text);
    final ImageView in_media_play = NullCheck.notNull(this.view_media_play);
    final ImageView in_media_next = NullCheck.notNull(this.view_media_next);
    final ImageView in_media_prev = NullCheck.notNull(this.view_media_prev);

    in_loading.setVisibility(View.GONE);
    in_web_view.setVisibility(View.VISIBLE);
    in_progress_bar.setVisibility(View.VISIBLE);
    in_progress_text.setVisibility(View.INVISIBLE);

    final ReaderSettingsType settings = rs.getSettings();
    this.onReaderSettingsChanged(settings);

    UIThread.runOnUIThread(
      new Runnable()
      {
        @Override public void run()
        {
          in_media_play.setOnClickListener(
            new OnClickListener()
            {
              @Override public void onClick(
                final @Nullable View v)
              {
                ReaderActivity.LOG.debug("toggling media overlay");
                js.mediaOverlayToggle();
              }
            });

          in_media_next.setOnClickListener(
            new OnClickListener()
            {
              @Override public void onClick(
                final @Nullable View v)
              {
                ReaderActivity.LOG.debug("next media overlay");
                js.mediaOverlayNext();
              }
            });

          in_media_prev.setOnClickListener(
            new OnClickListener()
            {
              @Override public void onClick(
                final @Nullable View v)
              {
                ReaderActivity.LOG.debug("previous media overlay");
                js.mediaOverlayPrevious();
              }
            });
        }
      });
  }

  private void postLastRead(final ReaderBookLocation l) {

    final SimplifiedReaderAppServicesType rs =
      Simplified.getReaderAppServices();
    final ReaderBookmarksType bm = rs.getBookmarks();
    final BookID in_book_id = NullCheck.notNull(this.book_id);
    final OPDSAcquisitionFeedEntry in_entry = NullCheck.notNull(this.entry.getFeedEntry());

    if (in_entry.getAnnotations().isSome()) {

      final String url = ((Some<URI>) in_entry.getAnnotations()).get().toString();
      final RequestQueue queue = Volley.newRequestQueue(this);

      // Request a string response from the provided URL.
      final NYPLStringRequest request = new NYPLStringRequest(Request.Method.GET, url, this.credentials,
        new Response.Listener<String>() {


          @Override
          public void onResponse(final String response) {

            LOG.debug("CurrentPage onResponse {}", response);

            final AnnotationResult result = new Gson().fromJson(response, AnnotationResult.class);

            if (result.getTotal() == 0)
            {
              bm.setBookmark(in_book_id, l, in_entry, ReaderActivity.this.credentials, queue);

            }
            else {


              for (final Annotation annotation : result.getFirst().getItems()) {

                if (annotation.getBody().isJsonObject()) {

                  final JsonObject body = (JsonObject) annotation.getBody();

                  final JsonPrimitive time = body.getAsJsonPrimitive("http://librarysimplified.org/terms/time");

                  LOG.debug("CurrentPage time {}", time.getAsString());

                  final Instant server_instant = new Instant(time.getAsString());
                  final Instant current_local_instant = new Instant();

                  final long local_new = current_local_instant.getMillis() - server_instant.getMillis();
                  LOG.debug("CurrentPage local_new {}", local_new);
                  final long server_new = server_instant.getMillis() - current_local_instant.getMillis();
                  LOG.debug("CurrentPage server_new {}", server_new);

                  if (local_new > 0) {
                    bm.setBookmark(in_book_id, l, in_entry, ReaderActivity.this.credentials, queue);
                  }
                }
                else
                {
                  bm.setBookmark(in_book_id, l, in_entry, ReaderActivity.this.credentials, queue);
                }
              }
            }

          }
        }, new Response.ErrorListener() {

        @Override
        public void onErrorResponse(final VolleyError error) {

          LOG.debug("CurrentPage onErrorResponse {}", error);

        }
      });

      // Add the request to the RequestQueue.
      queue.add(request);
    }
  }

  private void syncLastRead() {

    final OPDSAcquisitionFeedEntry in_entry = NullCheck.notNull(this.entry.getFeedEntry());

      if (in_entry.getAnnotations().isSome()) {

        final RequestQueue queue = Volley.newRequestQueue(this);
        final String url = ((Some<URI>) in_entry.getAnnotations()).get().toString();

        // Request a string response from the provided URL.
        final NYPLStringRequest request = new NYPLStringRequest(Request.Method.GET, url, this.credentials,
          new Response.Listener<String>() {


            @Override
            public void onResponse(final String response) {

              LOG.debug("CurrentPage onResponse {}", response);
              ReaderActivity.this.showBookLocationDialog(response);

            }
          }, new Response.ErrorListener() {

          @Override
          public void onErrorResponse(final VolleyError error) {

            LOG.debug("CurrentPage onErrorResponse {}", error);

          }
        });

        // Add the request to the RequestQueue.
        queue.add(request);
      }


  }

  @Override public void onReadiumFunctionInitializeError(
    final Throwable e)
  {
    ErrorDialogUtilities.showErrorWithRunnable(
      this,
      ReaderActivity.LOG,
      "Unable to initialize Readium",
      e,
      new Runnable()
      {
        @Override public void run()
        {
          ReaderActivity.this.finish();
        }
      });
  }

  /**
   * {@inheritDoc}
   *
   * When the device orientation changes, the configuration change handler
   * {@link #onConfigurationChanged(Configuration)} makes the web view invisible
   * so that the user does not see the now incorrectly-paginated content. When
   * Readium tells the app that the content pagination has changed, it makes the
   * web view visible again.
   */

  @Override public void onReadiumFunctionPaginationChanged(
    final ReaderPaginationChangedEvent e)
  {
    ReaderActivity.LOG.debug("pagination changed: {}", e);
    final WebView in_web_view = NullCheck.notNull(this.view_web_view);



    /**
     * Configure the progress bar and text.
     */

    final TextView in_progress_text =
      NullCheck.notNull(this.view_progress_text);
    final ProgressBar in_progress_bar =
      NullCheck.notNull(this.view_progress_bar);

    final Container container = NullCheck.notNull(this.epub_container);
    final Package default_package = NullCheck.notNull(container.getDefaultPackage());

    UIThread.runOnUIThread(
      new Runnable()
      {
        @Override public void run()
        {
          final double p = e.getProgressFractional();
          in_progress_bar.setMax(100);
          in_progress_bar.setProgress((int) (100.0 * p));

          final List<OpenPage> pages = e.getOpenPages();
          if (pages.isEmpty()) {
            in_progress_text.setText("");
          } else {
            final OpenPage page = NullCheck.notNull(pages.get(0));
            in_progress_text.setText(
              NullCheck.notNull(
                String.format(
                  "Page %d of %d (%s)",
                  page.getSpineItemPageIndex() + 1,
                  page.getSpineItemPageCount(),
                  default_package.getSpineItem(page.getIDRef()).getTitle())));
          }

          /**
           * Ask for Readium to deliver the unique identifier of the current page,
           * and tell Simplified that the page has changed and so any Javascript
           * state should be reconfigured.
           */
          UIThread.runOnUIThreadDelayed(
            new Runnable() {
              @Override
              public void run() {
                final ReaderReadiumJavaScriptAPIType readium_js =
                  NullCheck.notNull(ReaderActivity.this.readium_js_api);
                readium_js.getCurrentPage(ReaderActivity.this);
                readium_js.mediaOverlayIsAvailable(ReaderActivity.this);
              }
            }, 300L);
        }
      });

    final ReaderSimplifiedJavaScriptAPIType simplified_js =
      NullCheck.notNull(this.simplified_js_api);

    /**
     * Make the web view visible with a slight delay (as sometimes a
     * pagination-change event will be sent even though the content has not
     * yet been laid out in the web view). Only do this if the screen
     * orientation has just changed.
     */

    if (this.web_view_resized) {
      this.web_view_resized = false;
      UIThread.runOnUIThreadDelayed(
        new Runnable()
        {
          @Override public void run()
          {
            in_web_view.setVisibility(View.VISIBLE);
            in_progress_bar.setVisibility(View.VISIBLE);
            in_progress_text.setVisibility(View.VISIBLE);
            simplified_js.pageHasChanged();
          }
        }, 200L);
    } else {
      UIThread.runOnUIThread(
              new Runnable() {
                @Override
                public void run() {
                  simplified_js.pageHasChanged();
                }
              });
    }
  }

  @Override public void onReadiumFunctionPaginationChangedError(
    final Throwable x)
  {
    ReaderActivity.LOG.error("{}", x.getMessage(), x);
  }

  @Override public void onReadiumFunctionSettingsApplied()
  {
    ReaderActivity.LOG.debug("received settings applied");
  }

  @Override public void onReadiumFunctionSettingsAppliedError(
    final Throwable e)
  {
    ReaderActivity.LOG.error("{}", e.getMessage(), e);
  }

  @Override public void onReadiumFunctionUnknown(
    final String text)
  {
    ReaderActivity.LOG.error("unknown readium function: {}", text);
  }

  @Override public void onReadiumMediaOverlayStatusChangedIsPlaying(
    final boolean playing)
  {
    ReaderActivity.LOG.debug(
      "media overlay status changed: playing: {}", playing);

    final Resources rr = NullCheck.notNull(this.getResources());
    final ImageView play = NullCheck.notNull(this.view_media_play);

    UIThread.runOnUIThread(
            new Runnable() {
              @Override
              public void run() {
                if (playing) {
                  play.setImageDrawable(rr.getDrawable(R.drawable.circle_pause_8x));
                } else {
                  play.setImageDrawable(rr.getDrawable(R.drawable.circle_play_8x));
                }
              }
            });
  }

  @Override public void onReadiumMediaOverlayStatusError(
    final Throwable e)
  {
    ReaderActivity.LOG.error("{}", e.getMessage(), e);
  }

  @Override public void onServerStartFailed(
    final ReaderHTTPServerType hs,
    final Throwable x)
  {
    ErrorDialogUtilities.showErrorWithRunnable(
            this,
            ReaderActivity.LOG,
            "Could not start http server.",
            x,
            new Runnable() {
              @Override
              public void run() {
                ReaderActivity.this.finish();
              }
            });
  }

  @Override public void onServerStartSucceeded(
    final ReaderHTTPServerType hs,
    final boolean first)
  {
    if (first) {
      ReaderActivity.LOG.debug("http server started");
    } else {
      ReaderActivity.LOG.debug("http server already running");
    }

    this.makeInitialReadiumRequest(hs);
  }

  @Override public void onSimplifiedFunctionDispatchError(
    final Throwable x)
  {
    ReaderActivity.LOG.error("{}", x.getMessage(), x);
  }

  @Override public void onSimplifiedFunctionUnknown(
    final String text)
  {
    ReaderActivity.LOG.error("unknown function: {}", text);
  }

  @Override public void onSimplifiedGestureCenter()
  {
    final ViewGroup in_hud = NullCheck.notNull(this.view_hud);
    UIThread.runOnUIThread(
            new Runnable() {
              @Override
              public void run() {
                switch (in_hud.getVisibility()) {
                  case View.VISIBLE: {
                    FadeUtilities.fadeOut(
                            in_hud, FadeUtilities.DEFAULT_FADE_DURATION);
                    break;
                  }
                  case View.INVISIBLE:
                  case View.GONE: {
                    FadeUtilities.fadeIn(in_hud, FadeUtilities.DEFAULT_FADE_DURATION);
                    break;
                  }
                }
              }
            });
  }

  @Override public void onSimplifiedGestureCenterError(
    final Throwable x)
  {
    ReaderActivity.LOG.error("{}", x.getMessage(), x);
  }

  @Override public void onSimplifiedGestureLeft()
  {
    final ReaderReadiumJavaScriptAPIType js =
      NullCheck.notNull(this.readium_js_api);
    js.pagePrevious();
  }

  @Override public void onSimplifiedGestureLeftError(
    final Throwable x)
  {
    ReaderActivity.LOG.error("{}", x.getMessage(), x);
  }

  @Override public void onSimplifiedGestureRight()
  {
    final ReaderReadiumJavaScriptAPIType js =
      NullCheck.notNull(this.readium_js_api);
    js.pageNext();
  }

  @Override public void onSimplifiedGestureRightError(
    final Throwable x)
  {
    ReaderActivity.LOG.error("{}", x.getMessage(), x);
  }

  @Override public void onTOCSelectionReceived(
    final TOCElement e)
  {
    ReaderActivity.LOG.debug("received TOC selection: {}", e);

    final ReaderReadiumJavaScriptAPIType js =
      NullCheck.notNull(this.readium_js_api);

    js.openContentURL(e.getContentRef(), e.getSourceHref());
  }


// Bluefire Added

  /**
   * Encryption of a given text using the provided secretKey
   *
   * @param text
   * @param secretKey
   * @return the encoded string
   * @throws SignatureException
   */
  public static byte[] hashMac(String text, String secretKey)
          throws SignatureException, UnsupportedEncodingException {

    try {
      Key sk = new SecretKeySpec(secretKey.getBytes("UTF-8"), HASH_ALGORITHM);
      Mac mac = Mac.getInstance(sk.getAlgorithm());
      mac.init(sk);
      final byte[] hmac = mac.doFinal(text.getBytes());
      return hmac;
    } catch (NoSuchAlgorithmException e1) {
      // throw an exception or pick a different encryption method
      throw new SignatureException(
              "error building signature, no such algorithm in device "
                      + HASH_ALGORITHM);
    } catch (InvalidKeyException e) {
      throw new SignatureException(
              "error building signature, invalid key " + HASH_ALGORITHM);
    }
  }

  private static final String HASH_ALGORITHM = "HmacSHA256";

  public static String toHexString(byte[] bytes) {
    StringBuilder sb = new StringBuilder(bytes.length * 2);

    Formatter formatter = new Formatter(sb);
    for (byte b : bytes) {
      formatter.format("%02x", b);
    }

    return sb.toString();
  }
// Bluefire Added

}
