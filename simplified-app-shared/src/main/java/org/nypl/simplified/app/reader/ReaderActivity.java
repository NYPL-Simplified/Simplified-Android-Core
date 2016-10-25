package org.nypl.simplified.app.reader;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.ColorMatrixColorFilter;
import android.os.Build;
import android.os.Bundle;
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
import com.io7m.jfunctional.FunctionType;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.io7m.junreachable.UnreachableCodeException;

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
import org.nypl.simplified.prefs.Prefs;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.nypl.simplified.volley.NYPLStringRequest;
import org.readium.sdk.android.Container;
import org.readium.sdk.android.Package;
import org.slf4j.Logger;

import java.io.File;
import java.net.URI;
import java.util.List;

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
  private           Prefs                             prefs;
  /**
   * Construct an activity.
   */

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

    final Resources rr = NullCheck.notNull(this.getResources());
    final int main_color = rr.getColor(R.color.feature_main_color);
    final ColorMatrixColorFilter filter =
      ReaderColorMatrix.getImageFilterMatrix(main_color);

    UIThread.runOnUIThread(
      new Runnable()
      {
        @Override public void run()
        {
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
      new Runnable()
      {
        @Override public void run()
        {
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
      new Runnable()
      {
        @Override public void run()
        {
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
    this.syncLastRead();
  }

  @Override protected void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);
    this.setContentView(R.layout.reader);

    ReaderActivity.LOG.debug("starting");

    final Intent i = NullCheck.notNull(this.getIntent());
    final Bundle a = NullCheck.notNull(i.getExtras());

    final File in_epub_file =
      NullCheck.notNull((File) a.getSerializable(ReaderActivity.FILE_ID));
    this.book_id =
      NullCheck.notNull((BookID) a.getSerializable(ReaderActivity.BOOK_ID));
    this.entry =
      NullCheck.notNull((FeedEntryOPDS) a.getSerializable(ReaderActivity.ENTRY));

    this.prefs =  new Prefs(ReaderActivity.this);

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
      new OnClickListener()
      {
        @Override public void onClick(
          final @Nullable View v)
        {
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

    final WebChromeClient wc_client = new WebChromeClient()
    {
      @Override public void onShowCustomView(
        final @Nullable View view,
        final @Nullable CustomViewCallback callback)
      {
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
      new OnLongClickListener()
      {
        @Override public boolean onLongClick(
          final @Nullable View v)
        {
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
      new AccountGetCachedCredentialsListenerType()
      {
        @Override public void onAccountIsNotLoggedIn()
        {
          throw new UnreachableCodeException();
        }

        @Override public void onAccountIsLoggedIn(
          final AccountCredentials creds) {

          ReaderActivity.this.credentials = creds;

        }
      }
    );
  }

  @Override public void onCurrentPageError(
    final Throwable x)
  {
    ReaderActivity.LOG.error("{}", x.getMessage(), x);
  }

  @Override public void onCurrentPageReceived(
    final ReaderBookLocation l)
  {
    ReaderActivity.LOG.debug("received book location: {}", l);

    final BookID in_book_id = NullCheck.notNull(this.book_id);
    final OPDSAcquisitionFeedEntry in_entry = NullCheck.notNull(this.entry.getFeedEntry());

    final SimplifiedReaderAppServicesType rs =
      Simplified.getReaderAppServices();

    final ReaderBookmarksType bm = rs.getBookmarks();

    LOG.debug("CurrentPage prefs {}", this.prefs.getBoolean("post_last_read"));

    final RequestQueue queue = Volley.newRequestQueue(this);

    if (this.prefs.getBoolean("post_last_read")) {
      bm.setBookmark(in_book_id, l, in_entry, this.credentials, queue);
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
  }

  @Override public void onEPUBLoadFailed(
    final Throwable x)
  {
    ErrorDialogUtilities.showErrorWithRunnable(
      this, ReaderActivity.LOG, "Could not load EPUB file", x, new Runnable()
      {
        @Override public void run()
        {
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
      new OnClickListener()
      {
        @Override public void onClick(
          final @Nullable View v)
        {
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
      new Runnable()
      {
        @Override public void run()
        {
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

    final Container container = NullCheck.notNull(ReaderActivity.this.epub_container);

    final Package default_package = NullCheck.notNull(container.getDefaultPackage());
    final AnnotationResult result = new Gson().fromJson(response, AnnotationResult.class);
    OptionType<ReaderOpenPageRequestType> page_request = null;

    for (final Annotation annotation : result.getFirst().getItems())
    {
      if ("http://librarysimplified.org/terms/annotation/idling".equals(annotation.getMotivation())) {

        final String text = NullCheck.notNull(annotation.getTarget().getSelector().getValue());
        LOG.debug("CurrentPage text {}", text);

        final String key = NullCheck.notNull(this.book_id.toString());
        LOG.debug("CurrentPage key {}", key);

        try {
          final JSONObject o = new JSONObject(text);

          final OptionType<ReaderBookLocation> mark = Option.some(ReaderBookLocation.fromJSON(o));

          page_request = mark.map(
            new FunctionType<ReaderBookLocation, ReaderOpenPageRequestType>()
            {
              @Override public ReaderOpenPageRequestType call(
                final ReaderBookLocation l)
              {
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

          ReaderActivity.this.prefs.putBoolean("post_last_read", true);
          LOG.debug("CurrentPage set prefs {}", ReaderActivity.this.prefs.getBoolean("post_last_read"));

        }
      });

    builder.setNegativeButton("NO",
      new DialogInterface.OnClickListener() {
        @Override
        public void onClick(final DialogInterface dialog, final int which) {
          // negative button logic
          ReaderActivity.this.prefs.putBoolean("post_last_read", true);
          LOG.debug("CurrentPage set prefs {}", ReaderActivity.this.prefs.getBoolean("post_last_read"));

        }
      });

    LOG.debug("CurrentPage current_location {}", this.current_location);
    LOG.debug("CurrentPage sync_location {}", this.sync_location);

    if ((this.current_location == null && this.sync_location == null) || this.current_location != null && this.sync_location == null)
    {
      this.prefs.putBoolean("post_last_read", true);
      LOG.debug("CurrentPage set prefs {}", this.prefs.getBoolean("post_last_read"));
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
      this.prefs.putBoolean("post_last_read", true);
      LOG.debug("CurrentPage set prefs {}", this.prefs.getBoolean("post_last_read"));
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

  private void syncLastRead() {

    final SimplifiedReaderAppServicesType rs =
      Simplified.getReaderAppServices();
    final ReaderBookmarksType bm = rs.getBookmarks();
    final BookID in_book_id = NullCheck.notNull(this.book_id);
    final OPDSAcquisitionFeedEntry in_entry = NullCheck.notNull(this.entry.getFeedEntry());
    final ReaderBookmarksType bookmarks = rs.getBookmarks();

    final OptionType<ReaderBookLocation> mark =
      bookmarks.getBookmark(in_book_id, in_entry);

    final OptionType<ReaderOpenPageRequestType> page_request = mark.map(
      new FunctionType<ReaderBookLocation, ReaderOpenPageRequestType>() {
        @Override
        public ReaderOpenPageRequestType call(
          final ReaderBookLocation l) {
          LOG.debug("CurrentPage location {}", l);
          ReaderActivity.this.current_location = l;
          return ReaderOpenPageRequest.fromBookLocation(l);
        }
      });

    // Instantiate the RequestQueue.
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
        new Runnable()
        {
          @Override public void run()
          {
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
      new Runnable()
      {
        @Override public void run()
        {
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
      new Runnable()
      {
        @Override public void run()
        {
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
      new Runnable()
      {
        @Override public void run()
        {
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

}
