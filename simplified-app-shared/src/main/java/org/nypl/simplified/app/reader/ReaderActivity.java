package org.nypl.simplified.app.reader;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.ColorMatrixColorFilter;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.junreachable.UnreachableCodeException;

import org.joda.time.Instant;
import org.json.JSONObject;
import org.nypl.drm.core.AdobeDeviceID;
import org.nypl.simplified.app.R;
import org.nypl.simplified.app.ReaderSyncManager;
import org.nypl.simplified.app.Simplified;
import org.nypl.simplified.app.SimplifiedCatalogAppServicesType;
import org.nypl.simplified.app.SimplifiedReaderAppServicesType;
import org.nypl.simplified.app.reader.ReaderPaginationChangedEvent.OpenPage;
import org.nypl.simplified.app.reader.ReaderReadiumViewerSettings.ScrollMode;
import org.nypl.simplified.app.reader.ReaderReadiumViewerSettings.SyntheticSpreadMode;
import org.nypl.simplified.app.reader.ReaderTOC.TOCElement;
import org.nypl.simplified.app.utilities.ErrorDialogUtilities;
import org.nypl.simplified.app.utilities.FadeUtilities;
import org.nypl.simplified.app.utilities.UIThread;
import org.nypl.simplified.books.core.AccountCredentials;
import org.nypl.simplified.books.core.AccountGetCachedCredentialsListenerType;
import org.nypl.simplified.books.core.AccountsControllerType;
import org.nypl.simplified.books.core.BookDatabaseEntryReadableType;
import org.nypl.simplified.books.core.BookDatabaseEntryWritableType;
import org.nypl.simplified.books.core.BookDatabaseType;
import org.nypl.simplified.books.core.BookmarkAnnotation;
import org.nypl.simplified.books.core.SelectorNode;
import org.nypl.simplified.books.core.TargetNode;
import org.nypl.simplified.books.core.BodyNode;
import org.nypl.simplified.books.core.BookID;
import org.nypl.simplified.books.core.BooksType;
import org.nypl.simplified.books.core.FeedEntryOPDS;
import org.nypl.simplified.books.core.LogUtilities;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.readium.sdk.android.Container;
import org.readium.sdk.android.Package;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import kotlin.Unit;

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
  private           OPDSAcquisitionFeedEntry          feed_entry;
  private @Nullable Container                         epub_container;
  private @Nullable ReaderReadiumJavaScriptAPIType    readium_js_api;
  private @Nullable ReaderSimplifiedJavaScriptAPIType simplified_js_api;
  private           ImageView                         view_bookmark;
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
  private @Nullable ReaderBookLocation                current_location;
  private @Nullable BookmarkAnnotation                current_bookmark;
  private @Nullable AccountCredentials                credentials;
  private @Nullable ReaderSyncManager                 sync_manager;
  private           int                               current_page_index;
  private           int                               current_page_count;
  private @Nullable String                            current_chapter_title;
  private @Nullable List<BookmarkAnnotation>          bookmarks;


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
    Objects.requireNonNull(file);
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

    final TextView in_progress_text = Objects.requireNonNull(this.view_progress_text);
    final TextView in_title_text = Objects.requireNonNull(this.view_title_text);
    final ImageView in_toc = Objects.requireNonNull(this.view_toc);
    final ImageView in_bookmark = Objects.requireNonNull(this.view_bookmark);
    final ImageView in_settings = Objects.requireNonNull(this.view_settings);
    final ImageView in_media_play = Objects.requireNonNull(this.view_media_play);
    final ImageView in_media_next = Objects.requireNonNull(this.view_media_next);
    final ImageView in_media_prev = Objects.requireNonNull(this.view_media_prev);

    final int main_color = Color.parseColor(Simplified.getCurrentAccount().getMainColor());
    final ColorMatrixColorFilter filter =
      ReaderColorMatrix.getImageFilterMatrix(main_color);

    UIThread.runOnUIThread(() -> {
      in_progress_text.setTextColor(main_color);
      in_title_text.setTextColor(main_color);
      in_toc.setColorFilter(filter);
      in_bookmark.setColorFilter(filter);
      in_settings.setColorFilter(filter);
      in_media_play.setColorFilter(filter);
      in_media_next.setColorFilter(filter);
      in_media_prev.setColorFilter(filter);
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

    final View in_root = Objects.requireNonNull(this.view_root);
    UIThread.runOnUIThread(() -> {
      in_root.setBackgroundColor(cs.getBackgroundColor());
      ReaderActivity.this.applyViewerColorFilters();
    });
  }

  private void makeInitialReadiumRequest(
    final ReaderHTTPServerType hs)
  {
    final URI reader_uri = URI.create(hs.getURIBase() + "reader.html");
    final WebView wv = Objects.requireNonNull(this.view_web_view);
    UIThread.runOnUIThread(() -> {
      ReaderActivity.LOG.debug("making initial reader request: {}", reader_uri);
      wv.loadUrl(reader_uri.toString());
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
        final Intent nnd = Objects.requireNonNull(data);
        final Bundle b = Objects.requireNonNull(nnd.getExtras());
        final TOCElement element = (TOCElement) b.getSerializable(
          ReaderTOCActivity.TOC_SELECTED_ID);
        final BookmarkAnnotation annotation = (BookmarkAnnotation) b.getSerializable(
          ReaderTOCActivity.BOOKMARK_SELECTED_ID);
        if (element != null) {
          this.onTOCSelectionReceived(element);
        } else if (annotation != null) {
          this.onBookmarkSelectionReceived(annotation);
        } else {
          LOG.error("There was an error receiving input from user via TOC Selection");
        }
      } else {
        LOG.error("Error from TOC Activity Result");
      }
    }
  }

  @Override public void onConfigurationChanged(
    final @Nullable Configuration c)
  {
    super.onConfigurationChanged(c);

    ReaderActivity.LOG.debug("configuration changed");

    final WebView in_web_view = Objects.requireNonNull(this.view_web_view);
    final TextView in_progress_text =
      Objects.requireNonNull(this.view_progress_text);
    final ProgressBar in_progress_bar =
      Objects.requireNonNull(this.view_progress_bar);

    in_web_view.setVisibility(View.INVISIBLE);
    in_progress_text.setVisibility(View.INVISIBLE);
    in_progress_bar.setVisibility(View.INVISIBLE);

    this.web_view_resized = true;
    UIThread.runOnUIThreadDelayed(() -> {
      final ReaderReadiumJavaScriptAPIType readium_js =
        Objects.requireNonNull(ReaderActivity.this.readium_js_api);
      readium_js.getCurrentPage(ReaderActivity.this);
      readium_js.mediaOverlayIsAvailable(ReaderActivity.this);
    }, 300L);
  }

  @SuppressLint("SetJavaScriptEnabled")
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

    final Intent i = Objects.requireNonNull(this.getIntent());
    final Bundle a = Objects.requireNonNull(i.getExtras());

    final File in_epub_file =
      Objects.requireNonNull((File) a.getSerializable(ReaderActivity.FILE_ID));
    this.book_id =
      Objects.requireNonNull((BookID) a.getSerializable(ReaderActivity.BOOK_ID));
    final FeedEntryOPDS entry =
      Objects.requireNonNull((FeedEntryOPDS) a.getSerializable(ReaderActivity.ENTRY));
    this.feed_entry = entry.getFeedEntry();

    ReaderActivity.LOG.debug("epub file:  {}", in_epub_file);
    ReaderActivity.LOG.debug("book id:    {}", this.book_id);
    ReaderActivity.LOG.debug("entry id:   {}", entry.getFeedEntry().getID());

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

    final ViewGroup in_hud = Objects.requireNonNull(
      this.findViewById(R.id.reader_hud_container));
    final ImageView in_toc = Objects.requireNonNull(
      in_hud.findViewById(R.id.reader_toc));
    final ImageView in_bookmark = Objects.requireNonNull(
      in_hud.findViewById(R.id.reader_bookmark));
    final ImageView in_settings = Objects.requireNonNull(
      in_hud.findViewById(R.id.reader_settings));
    final TextView in_title_text = Objects.requireNonNull(
      in_hud.findViewById(R.id.reader_title_text));
    final TextView in_progress_text = Objects.requireNonNull(
      in_hud.findViewById(R.id.reader_position_text));
    final ProgressBar in_progress_bar = Objects.requireNonNull(
      in_hud.findViewById(R.id.reader_position_progress));

    final ViewGroup in_media_overlay = Objects.requireNonNull(
      this.findViewById(R.id.reader_hud_media));
    final ImageView in_media_previous = Objects.requireNonNull(
      this.findViewById(R.id.reader_hud_media_previous));
    final ImageView in_media_next = Objects.requireNonNull(
      this.findViewById(R.id.reader_hud_media_next));
    final ImageView in_media_play = Objects.requireNonNull(
      this.findViewById(R.id.reader_hud_media_play));

    final ProgressBar in_loading = Objects.requireNonNull(
      this.findViewById(R.id.reader_loading));
    final WebView in_webview = Objects.requireNonNull(
      this.findViewById(R.id.reader_webview));

    this.view_root = Objects.requireNonNull(in_hud.getRootView());

    in_loading.setVisibility(View.VISIBLE);
    in_progress_bar.setVisibility(View.INVISIBLE);
    in_progress_text.setVisibility(View.INVISIBLE);
    in_webview.setVisibility(View.INVISIBLE);
    in_hud.setVisibility(View.VISIBLE);
    in_media_overlay.setVisibility(View.INVISIBLE);

    in_settings.setOnClickListener(view -> {
      final FragmentManager fm = ReaderActivity.this.getFragmentManager();
      final ReaderSettingsDialog d = new ReaderSettingsDialog();
      d.show(fm, "settings-dialog");
    });

    // set reader brightness.
    final int brightness = getPreferences(Context.MODE_PRIVATE).getInt("reader_brightness", 50);
    final float back_light_value = (float) brightness / 100;
    final WindowManager.LayoutParams layout_params = getWindow().getAttributes();
    layout_params.screenBrightness = back_light_value;
    getWindow().setAttributes(layout_params);

    this.view_loading = in_loading;
    this.view_progress_text = in_progress_text;
    this.view_progress_bar = in_progress_bar;
    this.view_title_text = in_title_text;
    this.view_web_view = in_webview;
    this.view_hud = in_hud;
    this.view_toc = in_toc;
    this.view_bookmark = in_bookmark;
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
    in_webview.setOnLongClickListener(view -> {
      ReaderActivity.LOG.debug("ignoring long click on web view");
      return true;
    });

    // Allow the webview to be debuggable only if this is a dev build
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      if ((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
        WebView.setWebContentsDebuggingEnabled(true);
      }
    }

    final WebSettings s = Objects.requireNonNull(in_webview.getSettings());
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

    final SimplifiedCatalogAppServicesType app = Simplified.getCatalogAppServices();

    final BooksType books = app.getBooks();
    books.accountGetCachedLoginDetails(
      new AccountGetCachedCredentialsListenerType()
      {
        @Override public void onAccountIsNotLoggedIn() {
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

    this.current_location = l;
    Objects.requireNonNull(this.current_location);

    initiateSyncManagement();

    final SimplifiedReaderAppServicesType rs = Simplified.getReaderAppServices();
    final ReaderBookmarksSharedPrefsType bm = rs.getBookmarks();
    final BookID in_book_id = Objects.requireNonNull(this.book_id);

    bm.saveReadingPosition(in_book_id, this.current_location);
    uploadReadingPosition(this.current_location);

    checkForExistingBookmarkAtLocation(this.current_location);
  }

  @Override protected void onPause()
  {
    super.onPause();

    final SimplifiedReaderAppServicesType rs = Simplified.getReaderAppServices();

    if (this.book_id != null && this.current_location != null) {
      rs.getBookmarks().saveReadingPosition(this.book_id, this.current_location);
    }

    final ReaderSyncManager mgr = Objects.requireNonNull(this.sync_manager);
    mgr.sendOffAnyQueuedRequest();
  }

  @Override protected void onDestroy()
  {
    super.onDestroy();

    final ReaderReadiumJavaScriptAPIType readium_js =
      Objects.requireNonNull(ReaderActivity.this.readium_js_api);
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
      this, ReaderActivity.LOG,
      "Could not load EPUB file",
      x,
      ReaderActivity.this::finish);
  }



  @Override public void onEPUBLoadSucceeded(
    final Container c) {
    this.epub_container = c;
    final Package p = Objects.requireNonNull(c.getDefaultPackage());

    final TextView in_title_text = Objects.requireNonNull(this.view_title_text);
    UIThread.runOnUIThread(() -> in_title_text.setText(Objects.requireNonNull(p.getTitle())));

    /*
      Get any bookmarks from the local database.
     */

    synchronized(this) {
      if (this.bookmarks == null) {
        try {
          final BookDatabaseType db = Simplified.getCatalogAppServices().getBooks().bookGetDatabase();
          final BookDatabaseEntryReadableType entry = db.databaseOpenEntryForReading(this.book_id);
          this.bookmarks = entry.entryGetBookmarks();
          LOG.debug("Bookmarks ivar reconstituted after book launch: \n{}", this.bookmarks);
        } catch (IOException e) {
          LOG.error("Error getting list of bookmarks from the book entry database");
          this.bookmarks = new ArrayList<>();
        }
      }
    }

    /*
      Configure the TOC button.
     */

    final SimplifiedReaderAppServicesType rs = Simplified.getReaderAppServices();
    final View in_toc = Objects.requireNonNull(this.view_toc);

    in_toc.setOnClickListener((View v) -> {
      final ReaderTOC sent_toc = ReaderTOC.fromPackage(p);
      ReaderTOCActivity.startActivityForResult(ReaderActivity.this, sent_toc, this.bookmarks);
      ReaderActivity.this.overridePendingTransition(0, 0);
    });

    /*
      Configure the Bookmark button.
     */

    final View in_bookmark = Objects.requireNonNull(this.view_bookmark);
    Objects.requireNonNull(this.bookmarks);

    in_bookmark.setOnClickListener(view -> {
      if (this.current_bookmark != null) {
        delete(this.current_bookmark);
        this.bookmarks.remove(this.current_bookmark);
        this.current_bookmark = null;
      } else {
        final ReaderBookLocation current_loc = Objects.requireNonNull(this.current_location);
        final BookmarkAnnotation annotation = createAnnotation(current_loc, null);
        this.current_bookmark = annotation;
        saveAndUpload(annotation, current_loc);
        this.bookmarks.add(Objects.requireNonNull(this.current_bookmark));
      }
      updateBookmarkIconUI();
    });

    /*
      Get a reference to the web server. Start it if necessary (the callbacks
      will still be executed if the server is already running).
     */

    final ReaderHTTPServerType hs = rs.getHTTPServer();
    hs.startIfNecessaryForPackage(p, this);
  }

  private void updateBookmarkIconUI() {
    if (this.current_bookmark != null) {
      this.view_bookmark.setImageResource(R.drawable.bookmark_on);
    } else {
      this.view_bookmark.setImageResource(R.drawable.bookmark_off);
    }
  }

  private void saveAndUpload(final @NonNull BookmarkAnnotation annotation,
                             final @NonNull ReaderBookLocation location) {

    final ReaderSyncManager mgr = Objects.requireNonNull(this.sync_manager);
    final List<BookmarkAnnotation> bm = Objects.requireNonNull(this.bookmarks);

    //Save bookmark to local disk
    saveToDisk(annotation);

    //Save bookmark on the server
    mgr.postBookmarkToServer(annotation, (ID) -> {
      if (ID != null) {
        LOG.debug("Bookmark successfully uploaded. ID: {}", ID);
        final BookmarkAnnotation newAnnotation = createAnnotation(location, ID);
        this.delete(annotation);
        bm.remove(annotation);
        this.saveToDisk(newAnnotation);
      } else {
        LOG.error("Skipping annotation upload.");
      }
      return Unit.INSTANCE;
    });
  }

  private void saveToDisk(@NonNull BookmarkAnnotation mark) {
    final BookDatabaseType db = Simplified.getCatalogAppServices().getBooks().bookGetWritableDatabase();
    final BookDatabaseEntryWritableType entry = db.databaseOpenEntryForWriting(this.book_id);
    try {
      entry.entryAddBookmark(mark);
    } catch (IOException e) {
      LOG.error("Error writing annotation to app database: {}", mark);
      ErrorDialogUtilities.showError(
        this,
        ReaderActivity.LOG,
        getString(R.string.bookmark_save_error), null);
    }
  }

  private void delete(final BookmarkAnnotation annotation) {
    final ReaderSyncManager mgr = Objects.requireNonNull(this.sync_manager);

    /*
    Delete on the server if we have an ID/URI
     */
    if (annotation.getId() != null) {
      mgr.deleteBookmarkOnServer(annotation.getId(), (Boolean success) -> {
        if (success) {
          LOG.debug("Bookmark successfully deleted from server.");
        } else {
          LOG.debug("Error deleting bookmark on server. Continuing to delete bookmark locally.");
        }
        return Unit.INSTANCE;
      });
    } else {
      LOG.error("No annotation ID present on bookmark. Skipping network request to delete.");
    }

    /*
    Delete on the disk
     */
    final BookDatabaseType db = Simplified.getCatalogAppServices().getBooks().bookGetWritableDatabase();
    final BookDatabaseEntryWritableType entry = db.databaseOpenEntryForWriting(this.book_id);
    try {
      entry.entryDeleteBookmark(annotation);
    } catch (IOException e) {
      LOG.error("Error deleting annotation from the app database: {}", annotation);
    }
  }

  private synchronized @NonNull BookmarkAnnotation createAnnotation(
    @NonNull ReaderBookLocation bookmark,
    @Nullable String id)
  {
    Objects.requireNonNull(this.current_page_count);
    Objects.requireNonNull(this.credentials);
    Objects.requireNonNull(this.view_progress_bar);

    final String annotContext = "http://www.w3.org/ns/anno.jsonld";
    final String type = "Annotation";
    final String motivation = "http://www.w3.org/ns/oa#bookmarking";
    final String bookID = this.feed_entry.getID();
    final String selectorType = "oa:FragmentSelector";
    final String value = Objects.requireNonNull(bookmark.toJsonString());
    final String timestamp = new Instant().toString();
    final String deviceID = getDeviceIDString();

    float chapterProgress = 0.0f;
    if (this.current_page_count > 0) {
      chapterProgress = (float) current_page_index / current_page_count;
    }
    final float bookProgress = (float) this.view_progress_bar.getProgress() / this.view_progress_bar.getMax();

    final SelectorNode selectorNode = new SelectorNode(selectorType, value);
    final TargetNode targetNode = new TargetNode(bookID, selectorNode);
    final BodyNode bodyNode = new BodyNode(timestamp, deviceID, this.current_chapter_title, chapterProgress, bookProgress);
    return new BookmarkAnnotation(annotContext, bodyNode, id, type, motivation, targetNode);
  }

  private String getDeviceIDString() {
    final AccountCredentials creds = Objects.requireNonNull(this.credentials);
    OptionType<String> opt_deviceID = creds.getAdobeDeviceID().map(AdobeDeviceID::toString);
    final String deviceID;
    if (opt_deviceID.isSome()) {
      deviceID = ((Some<String>) opt_deviceID).get();
    } else {
      deviceID = "null";
    }
    return deviceID;
  }

  private @Nullable ReaderBookLocation createReaderLocation(
    final BookmarkAnnotation bm)
  {
    final String loc_value = bm.getTarget().getSelector().getValue(); //raw content cfi
    try {
      final JSONObject loc_json = new JSONObject(loc_value);
      return ReaderBookLocation.fromJSON(loc_json);
    } catch (Exception e) {
      ErrorDialogUtilities.showError(
        this,
        LOG,
        getString(R.string.bookmark_navigation_error), null);
      return null;
    }
  }

  private void checkForExistingBookmarkAtLocation(final @NonNull ReaderBookLocation loc) {

    Objects.requireNonNull(this.bookmarks);

    this.current_bookmark = null;
    for (int i = 0; i < this.bookmarks.size(); i++) {
      final ReaderBookLocation mark_loc = createReaderLocation(this.bookmarks.get(i));
      if (mark_loc == null) {
        continue;
      }
      if (mark_loc.equals(loc)) {
        this.current_bookmark = this.bookmarks.get(i);
        break;
      }
    }
    this.updateBookmarkIconUI();
  }

  @Override public void onMediaOverlayIsAvailable(
    final boolean available)
  {
    ReaderActivity.LOG.debug(
      "media overlay status changed: available: {}", available);

    final ViewGroup in_media_hud = Objects.requireNonNull(this.view_media);
    final TextView in_title = Objects.requireNonNull(this.view_title_text);
    UIThread.runOnUIThread(() -> {
      in_media_hud.setVisibility(available ? View.VISIBLE : View.GONE);
      in_title.setVisibility(available ? View.GONE : View.VISIBLE);
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
      Objects.requireNonNull(this.readium_js_api);
    js.setPageStyleSettings(s);

    final ReaderColorScheme cs = s.getColorScheme();
    this.applyViewerColorScheme(cs);

    UIThread.runOnUIThreadDelayed(() -> {
      final ReaderReadiumJavaScriptAPIType readium_js =
        Objects.requireNonNull(ReaderActivity.this.readium_js_api);
      readium_js.getCurrentPage(ReaderActivity.this);
      readium_js.mediaOverlayIsAvailable(ReaderActivity.this);
    }, 300L);
  }

  @Override public void onReadiumFunctionDispatchError(
    final Throwable x)
  {
    ReaderActivity.LOG.error("{}", x.getMessage(), x);
  }

  @Override public void onReadiumFunctionInitialize()
  {
    ReaderActivity.LOG.debug("readium initialize requested");

    final SimplifiedReaderAppServicesType rs =
      Simplified.getReaderAppServices();

    final ReaderHTTPServerType hs = rs.getHTTPServer();
    final Container c = Objects.requireNonNull(this.epub_container);
    final Package p = Objects.requireNonNull(c.getDefaultPackage());
    p.setRootUrls(hs.getURIBase().toString(), null);

    final ReaderReadiumViewerSettings vs =
      Objects.requireNonNull(this.viewer_settings);
    final ReaderReadiumJavaScriptAPIType js =
      Objects.requireNonNull(this.readium_js_api);

    /*
      If there's a bookmark for the current book, send a request to open the
      book to that specific page. Otherwise, start at the beginning.
     */

    final BookID in_book_id = Objects.requireNonNull(this.book_id);
    final OPDSAcquisitionFeedEntry in_entry = Objects.requireNonNull(this.feed_entry);

    final ReaderBookmarksSharedPrefsType bookmarks = rs.getBookmarks();
    final ReaderBookLocation location = bookmarks.getReadingPosition(in_book_id, in_entry);

    final OptionType<ReaderBookLocation> optionLocation = Option.of(location);
    final OptionType<ReaderOpenPageRequestType> page_request = optionLocation.map( l -> {
      ReaderActivity.this.current_location = l;
      return ReaderOpenPageRequest.fromBookLocation(l);
    });

    // is this correct? inject fonts before book opens or after
    js.injectFonts();

    // open book with page request, vs = view settings, p = package , what is package actually ? page_request = idref + contentcfi
    js.openBook(p, vs, page_request);

    /*
      Configure the visibility of UI elements.
     */

    final WebView in_web_view = Objects.requireNonNull(this.view_web_view);
    final ProgressBar in_loading = Objects.requireNonNull(this.view_loading);
    final ProgressBar in_progress_bar =
      Objects.requireNonNull(this.view_progress_bar);
    final TextView in_progress_text =
      Objects.requireNonNull(this.view_progress_text);
    final ImageView in_media_play = Objects.requireNonNull(this.view_media_play);
    final ImageView in_media_next = Objects.requireNonNull(this.view_media_next);
    final ImageView in_media_prev = Objects.requireNonNull(this.view_media_prev);

    in_loading.setVisibility(View.GONE);
    in_web_view.setVisibility(View.VISIBLE);
    in_progress_bar.setVisibility(View.VISIBLE);
    in_progress_text.setVisibility(View.INVISIBLE);

    final ReaderSettingsType settings = rs.getSettings();
    this.onReaderSettingsChanged(settings);

    UIThread.runOnUIThread(() -> {
      in_media_play.setOnClickListener(view -> {
        ReaderActivity.LOG.debug("toggling media overlay");
        js.mediaOverlayToggle();
      });

      in_media_next.setOnClickListener(
        view -> {
          ReaderActivity.LOG.debug("next media overlay");
          js.mediaOverlayNext();
        });

      in_media_prev.setOnClickListener(
        view -> {
          ReaderActivity.LOG.debug("previous media overlay");
          js.mediaOverlayPrevious();
        });
    });
  }

  /*
    Reader Sync Manager
   */

  private void uploadReadingPosition(final ReaderBookLocation location)
  {
    final ReaderSyncManager mgr = Objects.requireNonNull(this.sync_manager);
    mgr.updateServerReadingLocation(location);
  }

  private void initiateSyncManagement()
  {
    if (this.sync_manager != null) {
      return;
    }

    final ReaderBookLocation current_loc = Objects.requireNonNull(this.current_location);
    final List<BookmarkAnnotation> current_marks = Objects.requireNonNull(this.bookmarks);

    final AccountsControllerType accountController = Simplified.getCatalogAppServices().getBooks();
    if (this.credentials == null || accountController == null) {
      LOG.error("Parameter(s) were unexpectedly null before creating Sync Manager. Abandoning attempt.");
      return;
    }

    this.sync_manager = new ReaderSyncManager(
      this.feed_entry,
      this.credentials,
      Simplified.getCurrentAccount(),
      this,
      (location) -> {
        navigateTo(location);
        return Unit.INSTANCE;
      });

    final Container c = Objects.requireNonNull(this.epub_container);
    final Package p = c.getDefaultPackage();
    this.sync_manager.setBookPackage(p);

    this.sync_manager.serverSyncPermission(Simplified.getCatalogAppServices().getBooks(), () -> {
      //Sync Permitted or Successfully Enabled
      sync_manager.syncReadingLocation(getDeviceIDString(), current_loc, this);
      sync_manager.retryOnDiskBookmarksUpload(current_marks);
      sync_manager.syncBookmarks(current_marks, (syncedMarks) -> {
        synchronized(this) {
          this.bookmarks = syncedMarks;
          //Update bookmarks on disk
          final BookDatabaseType db = Simplified.getCatalogAppServices().getBooks().bookGetWritableDatabase();
          final BookDatabaseEntryWritableType entry = db.databaseOpenEntryForWriting(this.book_id);
          try {
            entry.entrySetBookmarks(syncedMarks);
          } catch (IOException e) {
            LOG.error("Error writing annotation to app database: {}", syncedMarks);
          }
          return Unit.INSTANCE;
        }
      });
      return Unit.INSTANCE;
    });
  }

  @Override public void onReadiumFunctionInitializeError(
    final Throwable e)
  {
    ErrorDialogUtilities.showErrorWithRunnable(
      this,
      ReaderActivity.LOG,
      "Unable to initialize Readium",
      e,
      ReaderActivity.this::finish);
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
    final WebView in_web_view = Objects.requireNonNull(this.view_web_view);

    /*
      Configure the progress bar and text.
     */

    final TextView in_progress_text =
      Objects.requireNonNull(this.view_progress_text);
    final ProgressBar in_progress_bar =
      Objects.requireNonNull(this.view_progress_bar);

    final Container container = Objects.requireNonNull(this.epub_container);
    final Package default_package = Objects.requireNonNull(container.getDefaultPackage());

    UIThread.runOnUIThread(() -> {
      final double p = e.getProgressFractional();
      in_progress_bar.setMax(100);
      in_progress_bar.setProgress((int) (100.0 * p));

      final List<OpenPage> pages = e.getOpenPages();
      if (pages.isEmpty()) {
        in_progress_text.setText("");
      } else {
        final OpenPage page = Objects.requireNonNull(pages.get(0));

        current_page_index = page.getSpineItemPageIndex();
        current_page_count = page.getSpineItemPageCount();
        current_chapter_title = default_package.getSpineItem(page.getIDRef()).getTitle();

        in_progress_text.setText(
          Objects.requireNonNull(
            String.format(
              Locale.ENGLISH,
              "Page %d of %d (%s)",
              page.getSpineItemPageIndex() + 1,
              page.getSpineItemPageCount(),
              default_package.getSpineItem(page.getIDRef()).getTitle())));
      }

      /*
        Ask for Readium to deliver the unique identifier of the current page,
        and tell Simplified that the page has changed and so any Javascript
        state should be reconfigured.
       */
      UIThread.runOnUIThreadDelayed(() -> {
        final ReaderReadiumJavaScriptAPIType readium_js =
          Objects.requireNonNull(ReaderActivity.this.readium_js_api);
        readium_js.getCurrentPage(ReaderActivity.this);
        readium_js.mediaOverlayIsAvailable(ReaderActivity.this);
      }, 300L);
    });

    final ReaderSimplifiedJavaScriptAPIType simplified_js =
      Objects.requireNonNull(this.simplified_js_api);

    /*
      Make the web view visible with a slight delay (as sometimes a
      pagination-change event will be sent even though the content has not
      yet been laid out in the web view). Only do this if the screen
      orientation has just changed.
     */

    if (this.web_view_resized) {
      this.web_view_resized = false;
      UIThread.runOnUIThreadDelayed(() -> {
        in_web_view.setVisibility(View.VISIBLE);
        in_progress_bar.setVisibility(View.VISIBLE);
        in_progress_text.setVisibility(View.VISIBLE);
        simplified_js.pageHasChanged();
      }, 200L);
    } else {
      UIThread.runOnUIThread(simplified_js::pageHasChanged);
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

    final Resources rr = Objects.requireNonNull(this.getResources());
    final ImageView play = Objects.requireNonNull(this.view_media_play);

    UIThread.runOnUIThread(() -> {
      if (playing) {
        play.setImageDrawable(rr.getDrawable(R.drawable.circle_pause_8x));
      } else {
        play.setImageDrawable(rr.getDrawable(R.drawable.circle_play_8x));
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
      ReaderActivity.this::finish);
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
    final ViewGroup in_hud = Objects.requireNonNull(this.view_hud);
    UIThread.runOnUIThread(() -> {
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
      Objects.requireNonNull(this.readium_js_api);
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
      Objects.requireNonNull(this.readium_js_api);
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
      Objects.requireNonNull(this.readium_js_api);

    js.openContentURL(e.getContentRef(), e.getSourceHref());
  }

  @Override public void onBookmarkSelectionReceived(
    final BookmarkAnnotation bm)
  {
    ReaderActivity.LOG.debug("received bookmark selection: {}", bm);
    final ReaderBookLocation loc = createReaderLocation(bm);
    if (loc != null) {
      this.navigateTo(loc);
    }
  }

  private void navigateTo(final ReaderBookLocation location) {
    final OptionType<ReaderBookLocation> optLocation = Option.some(location);

    OptionType<ReaderOpenPageRequestType> page_request;
    page_request = optLocation.map((l) -> {
      LOG.debug("CurrentPage location {}", l);
      ReaderActivity.this.current_location = l;
      return ReaderOpenPageRequest.fromBookLocation(l);
    });

    final OptionType<ReaderOpenPageRequestType> page = page_request;

    final ReaderReadiumJavaScriptAPIType js =
        Objects.requireNonNull(ReaderActivity.this.readium_js_api);
    final ReaderReadiumViewerSettings vs =
      Objects.requireNonNull(ReaderActivity.this.viewer_settings);
    final Container c = Objects.requireNonNull(ReaderActivity.this.epub_container);
    final Package p = Objects.requireNonNull(c.getDefaultPackage());

    js.openBook(p, vs, page);
  }
}
