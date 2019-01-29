package org.nypl.simplified.app.reader;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.ColorMatrixColorFilter;
import android.os.Build;
import android.os.Bundle;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.FluentFuture;
import com.io7m.jfunctional.None;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.OptionVisitorType;
import com.io7m.jfunctional.Some;

import org.joda.time.LocalDateTime;
import org.nypl.simplified.app.ApplicationColorScheme;
import org.nypl.simplified.app.R;
import org.nypl.simplified.app.Simplified;
import org.nypl.simplified.app.profiles.ProfileTimeOutActivity;
import org.nypl.simplified.app.reader.ReaderPaginationChangedEvent.OpenPage;
import org.nypl.simplified.app.reader.ReaderTOC.TOCElement;
import org.nypl.simplified.app.utilities.ErrorDialogUtilities;
import org.nypl.simplified.app.utilities.UIThread;
import org.nypl.simplified.books.accounts.AccountAuthenticationAdobePostActivationCredentials;
import org.nypl.simplified.books.accounts.AccountAuthenticationCredentials;
import org.nypl.simplified.books.accounts.AccountType;
import org.nypl.simplified.books.book_database.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleEPUB;
import org.nypl.simplified.books.book_database.BookDatabaseException;
import org.nypl.simplified.books.book_database.BookFormat;
import org.nypl.simplified.books.book_database.BookID;
import org.nypl.simplified.books.controller.BookmarksControllerType;
import org.nypl.simplified.books.core.BookmarkAnnotation;
import org.nypl.simplified.books.feeds.FeedEntry.FeedEntryOPDS;
import org.nypl.simplified.books.profiles.ProfileEvent;
import org.nypl.simplified.books.profiles.ProfileNoneCurrentException;
import org.nypl.simplified.books.profiles.ProfilePreferencesChanged;
import org.nypl.simplified.books.reader.ReaderBookLocation;
import org.nypl.simplified.books.reader.ReaderBookLocationJSON;
import org.nypl.simplified.books.reader.ReaderBookmark;
import org.nypl.simplified.books.reader.ReaderColorScheme;
import org.nypl.simplified.books.reader.ReaderPreferences;
import org.nypl.simplified.observable.ObservableSubscriptionType;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.readium.sdk.android.Container;
import org.readium.sdk.android.Package;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.nypl.simplified.app.reader.ReaderReadiumViewerSettings.ScrollMode.AUTO;
import static org.nypl.simplified.app.reader.ReaderReadiumViewerSettings.SyntheticSpreadMode.SINGLE;
import static org.nypl.simplified.app.utilities.FadeUtilities.DEFAULT_FADE_DURATION;
import static org.nypl.simplified.app.utilities.FadeUtilities.fadeIn;
import static org.nypl.simplified.app.utilities.FadeUtilities.fadeOut;

/**
 * The main reader activity for reading an EPUB.
 */

public final class ReaderActivity extends ProfileTimeOutActivity implements
  ReaderHTTPServerStartListenerType,
  ReaderSimplifiedFeedbackListenerType,
  ReaderReadiumFeedbackListenerType,
  ReaderReadiumEPUBLoadListenerType,
  ReaderCurrentPageListenerType,
  ReaderTOCSelectionListenerType,
  ReaderMediaOverlayAvailabilityListenerType {
  private static final String BOOK_ID;
  private static final String FILE_ID;
  private static final String ENTRY;
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(ReaderActivity.class);
  }

  static {
    BOOK_ID = "org.nypl.simplified.app.ReaderActivity.book";
    FILE_ID = "org.nypl.simplified.app.ReaderActivity.file";
    ENTRY = "org.nypl.simplified.app.ReaderActivity.entry";
  }

  private BookID book_id;
  private OPDSAcquisitionFeedEntry feed_entry;
  private Container epub_container;
  private ReaderReadiumJavaScriptAPIType readium_js_api;
  private ReaderSimplifiedJavaScriptAPIType simplified_js_api;
  private ImageView view_bookmark;
  private ViewGroup view_hud;
  private ProgressBar view_loading;
  private ViewGroup view_media;
  private ImageView view_media_next;
  private ImageView view_media_play;
  private ImageView view_media_prev;
  private ProgressBar view_progress_bar;
  private TextView view_progress_text;
  private View view_root;
  private ImageView view_settings;
  private TextView view_title_text;
  private ImageView view_toc;
  private WebView view_web_view;
  private ReaderReadiumViewerSettings viewer_settings;
  private boolean web_view_resized;
  private ReaderBookmark current_location;
  private AccountType current_account;
  private int current_page_index = 0;
  private int current_page_count = 1;
  private String current_chapter_title = "Unknown";
  private ObservableSubscriptionType<ProfileEvent> profile_subscription;
  private ObjectMapper json_mapper = new ObjectMapper();
  private BookDatabaseEntryFormatHandleEPUB formatHandle;

  /**
   * Construct an activity.
   */

  public ReaderActivity() {

  }

  /**
   * Start a new reader for the given book.
   *
   * @param from  The parent activity
   * @param book  The unique ID of the book
   * @param file  The actual EPUB file
   * @param entry The OPD feed entry
   */

  public static void startActivity(
    final Activity from,
    final BookID book,
    final File file,
    final FeedEntryOPDS entry) {
    Objects.requireNonNull(file);
    final Bundle b = new Bundle();
    b.putSerializable(ReaderActivity.BOOK_ID, book);
    b.putSerializable(ReaderActivity.FILE_ID, file);
    b.putSerializable(ReaderActivity.ENTRY, entry);
    final Intent i = new Intent(from, ReaderActivity.class);
    i.putExtras(b);
    from.startActivity(i);
  }

  private void applyViewerColorFilters() {
    LOG.debug("applying color filters");

    final TextView in_progress_text = Objects.requireNonNull(this.view_progress_text);
    final TextView in_title_text = Objects.requireNonNull(this.view_title_text);
    final ImageView in_toc = Objects.requireNonNull(this.view_toc);
    final ImageView in_bookmark = Objects.requireNonNull(this.view_bookmark);
    final ImageView in_settings = Objects.requireNonNull(this.view_settings);
    final ImageView in_media_play = Objects.requireNonNull(this.view_media_play);
    final ImageView in_media_next = Objects.requireNonNull(this.view_media_next);
    final ImageView in_media_prev = Objects.requireNonNull(this.view_media_prev);

    final ApplicationColorScheme scheme = Simplified.getMainColorScheme();
    final int mainColor = scheme.getColorRGBA();
    final ColorMatrixColorFilter filter = ReaderColorMatrix.getImageFilterMatrix(mainColor);

    UIThread.runOnUIThread(() -> {
      in_progress_text.setTextColor(mainColor);
      in_title_text.setTextColor(mainColor);
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

  private void applyViewerColorScheme(final ReaderColorScheme cs) {
    LOG.debug("applying color scheme");

    final View in_root = Objects.requireNonNull(this.view_root);
    UIThread.runOnUIThread(() -> {
      in_root.setBackgroundColor(ReaderColorSchemes.background(cs));
      this.applyViewerColorFilters();
    });
  }

  private void makeInitialReadiumRequest(final ReaderHTTPServerType hs) {
    final URI reader_uri = URI.create(hs.getURIBase() + "reader.html");
    final WebView wv = Objects.requireNonNull(this.view_web_view);
    UIThread.runOnUIThread(() -> {
      LOG.debug("making initial reader request: {}", reader_uri);
      wv.loadUrl(reader_uri.toString());
    });
  }

  @Override
  protected void onActivityResult(
    final int request_code,
    final int result_code,
    final @Nullable Intent data) {
    super.onActivityResult(request_code, result_code, data);

    LOG.debug("onActivityResult: {} {} {}", request_code, result_code, data);

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

  @Override
  public void onConfigurationChanged(final @Nullable Configuration c) {
    super.onConfigurationChanged(c);

    LOG.debug("configuration changed");

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
        Objects.requireNonNull(this.readium_js_api);
      readium_js.getCurrentPage(this);
      readium_js.mediaOverlayIsAvailable(this);
    }, 300L);
  }

  @SuppressLint("SetJavaScriptEnabled")
  @Override
  protected void onCreate(final @Nullable Bundle state) {
    this.setTheme(Simplified.getMainColorScheme().getActivityThemeResourceWithoutActionBar());

    super.onCreate(state);
    this.setContentView(R.layout.reader);

    LOG.debug("starting");

    final Intent i = Objects.requireNonNull(this.getIntent());
    final Bundle a = Objects.requireNonNull(i.getExtras());

    final File in_epub_file =
      Objects.requireNonNull((File) a.getSerializable(ReaderActivity.FILE_ID));
    this.book_id =
      Objects.requireNonNull((BookID) a.getSerializable(ReaderActivity.BOOK_ID));
    final FeedEntryOPDS entry =
      Objects.requireNonNull((FeedEntryOPDS) a.getSerializable(ReaderActivity.ENTRY));
    this.feed_entry = entry.getFeedEntry();

    LOG.debug("epub file:  {}", in_epub_file);
    LOG.debug("book id:    {}", this.book_id);
    LOG.debug("entry id:   {}", entry.getFeedEntry().getID());

    try {
      this.current_account =
        Simplified.getProfilesController()
          .profileAccountCurrent();
    } catch (ProfileNoneCurrentException e) {
      throw new IllegalStateException(e);
    }

    this.profile_subscription =
      Simplified.getProfilesController()
        .profileEvents()
        .subscribe(this::onProfileEvent);

    final ReaderPreferences readerPreferences;

    try {
      readerPreferences =
        Simplified.getProfilesController()
          .profileCurrent()
          .preferences()
          .readerPreferences();
    } catch (final ProfileNoneCurrentException e) {
      throw new IllegalStateException(e);
    }

    this.viewer_settings =
      new ReaderReadiumViewerSettings(
        SINGLE, AUTO, (int) readerPreferences.fontScale(), 20);

    final ReaderReadiumFeedbackDispatcherType rd =
      ReaderReadiumFeedbackDispatcher.newDispatcher();
    final ReaderSimplifiedFeedbackDispatcherType sd =
      ReaderSimplifiedFeedbackDispatcher.newDispatcher();

    final ViewGroup in_hud =
      Objects.requireNonNull(this.findViewById(R.id.reader_hud_container));
    final ImageView in_toc =
      Objects.requireNonNull(in_hud.findViewById(R.id.reader_toc));
    final ImageView in_bookmark =
      Objects.requireNonNull(in_hud.findViewById(R.id.reader_bookmark));
    final ImageView in_settings =
      Objects.requireNonNull(in_hud.findViewById(R.id.reader_settings));
    final TextView in_title_text =
      Objects.requireNonNull(in_hud.findViewById(R.id.reader_title_text));
    final TextView in_progress_text =
      Objects.requireNonNull(in_hud.findViewById(R.id.reader_position_text));
    final ProgressBar in_progress_bar =
      Objects.requireNonNull(in_hud.findViewById(R.id.reader_position_progress));

    final ViewGroup in_media_overlay =
      Objects.requireNonNull(this.findViewById(R.id.reader_hud_media));
    final ImageView in_media_previous =
      Objects.requireNonNull(this.findViewById(R.id.reader_hud_media_previous));
    final ImageView in_media_next =
      Objects.requireNonNull(this.findViewById(R.id.reader_hud_media_next));
    final ImageView in_media_play =
      Objects.requireNonNull(this.findViewById(R.id.reader_hud_media_play));

    final ProgressBar in_loading =
      Objects.requireNonNull(this.findViewById(R.id.reader_loading));
    final WebView in_webview =
      Objects.requireNonNull(this.findViewById(R.id.reader_webview));

    this.view_root = Objects.requireNonNull(in_hud.getRootView());

    in_loading.setVisibility(View.VISIBLE);
    in_progress_bar.setVisibility(View.INVISIBLE);
    in_progress_text.setVisibility(View.INVISIBLE);
    in_webview.setVisibility(View.INVISIBLE);
    in_hud.setVisibility(View.VISIBLE);
    in_media_overlay.setVisibility(View.INVISIBLE);

    in_settings.setOnClickListener(view -> {
      final FragmentManager fm = this.getFragmentManager();
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

    final WebChromeClient wc_client = new WebChromeClient() {
      @Override
      public void onShowCustomView(
        final @Nullable View view,
        final @Nullable CustomViewCallback callback) {
        super.onShowCustomView(view, callback);
        LOG.debug("web-chrome: {}", view);
      }
    };

    final WebViewClient wv_client =
      new ReaderWebViewClient(this, sd, this, rd, this);
    in_webview.setBackgroundColor(0x00000000);
    in_webview.setWebChromeClient(wc_client);
    in_webview.setWebViewClient(wv_client);
    in_webview.setOnLongClickListener(view -> {
      LOG.debug("ignoring long click on web view");
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

    try {
      this.formatHandle =
        Simplified.getProfilesController()
          .profileAccountCurrent()
          .bookDatabase()
          .entry(this.book_id)
          .findFormatHandle(BookDatabaseEntryFormatHandleEPUB.class);
    } catch (BookDatabaseException | ProfileNoneCurrentException e) {
      throw new IllegalStateException(e);
    }

    Objects.requireNonNull(this.formatHandle, "formatHandle");

    final BookFormat.BookFormatEPUB format = this.formatHandle.getFormat();
    final ReaderReadiumEPUBLoaderType loader = Simplified.getReadiumEPUBLoader();
    final ReaderReadiumEPUBLoadRequest request =
      ReaderReadiumEPUBLoadRequest.builder(in_epub_file)
        .setAdobeRightsFile(Option.of(format.getAdobeRightsFile()))
        .build();

    loader.loadEPUB(request, this);
    this.applyViewerColorFilters();
  }

  private void onProfileEvent(final ProfileEvent event) {
    if (event instanceof ProfilePreferencesChanged) {
      try {
        onProfileEventPreferencesChanged((ProfilePreferencesChanged) event);
      } catch (ProfileNoneCurrentException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  private void onProfileEventPreferencesChanged(
    final ProfilePreferencesChanged event)
    throws ProfileNoneCurrentException {
    if (event.changedReaderPreferences()) {
      LOG.debug("reader settings changed");

      final ReaderPreferences preferences =
        Simplified.getProfilesController()
          .profileCurrent()
          .preferences()
          .readerPreferences();

      applyReaderPreferences(preferences);
    }
  }

  private void applyReaderPreferences(ReaderPreferences preferences) {
    final ReaderReadiumJavaScriptAPIType js = Objects.requireNonNull(this.readium_js_api);
    js.setPageStyleSettings(preferences);

    final ReaderColorScheme cs = preferences.colorScheme();
    this.applyViewerColorScheme(cs);

    UIThread.runOnUIThreadDelayed(() -> {
      final ReaderReadiumJavaScriptAPIType readium_js =
        Objects.requireNonNull(this.readium_js_api);
      readium_js.getCurrentPage(this);
      readium_js.mediaOverlayIsAvailable(this);
    }, 300L);
  }

  @Override
  public void onCurrentPageError(final Throwable x) {
    LOG.error("onCurrentPageError: {}", x.getMessage(), x);
  }

  @Override
  public void onCurrentPageReceived(final ReaderBookLocation location) {
    Objects.requireNonNull(location);
    LOG.debug("received book location: {}", location);

    final ReaderBookmark bookmark =
      new ReaderBookmark(
        this.book_id,
        location,
        LocalDateTime.now(),
        this.current_chapter_title,
        currentChapterProgress(),
        currentBookProgress(),
        getDeviceIDString());

    this.current_location = bookmark;

    final FluentFuture<BookmarksControllerType.Bookmarks> future =
      Simplified.getBookmarksController()
        .bookmarksUpdate(
          this.current_account,
          this.book_id,
          bookmarks -> new BookmarksControllerType.Bookmarks(bookmark, bookmarks.getBookmarks()));
  }

  @Override
  protected void onPause() {
    super.onPause();

    Simplified.getBookmarksController()
      .bookmarksUpdate(this.current_account, this.book_id, bookmarks -> {
        ReaderBookmark current = this.current_location;
        if (current != null) {
          return new BookmarksControllerType.Bookmarks(current, bookmarks.getBookmarks());
        } else {
          return bookmarks;
        }
      });
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

    final ReaderReadiumJavaScriptAPIType readium_js = Objects.requireNonNull(this.readium_js_api);
    readium_js.getCurrentPage(this);
    readium_js.mediaOverlayIsAvailable(this);

    final ObservableSubscriptionType<ProfileEvent> sub = this.profile_subscription;
    if (sub != null) {
      sub.unsubscribe();
    }
    this.profile_subscription = null;
  }

  @Override
  public void onEPUBLoadFailed(
    final Throwable x) {
    ErrorDialogUtilities.showErrorWithRunnable(
      this, LOG,
      "Could not load EPUB file",
      x,
      this::finish);
  }

  @Override
  public void onEPUBLoadSucceeded(final Container c) {
    this.epub_container = c;
    final Package p = Objects.requireNonNull(c.getDefaultPackage());

    final TextView in_title_text = Objects.requireNonNull(this.view_title_text);
    UIThread.runOnUIThread(() -> in_title_text.setText(Objects.requireNonNull(p.getTitle())));

    try {
      final BookmarksControllerType.Bookmarks bookmarks =
        Simplified.getBookmarksController()
          .bookmarksLoad(this.current_account, this.book_id)
          .get(10L, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      LOG.error("could not load bookmarks: ", e);
    } catch (ExecutionException e) {
      LOG.error("could not load bookmarks: ", e);
    } catch (TimeoutException e) {
      LOG.error("could not load bookmarks: ", e);
    }

    /*
     * Get a reference to the web server. Start it if necessary (the callbacks
     * will still be executed if the server is already running).
     */

    final ReaderHTTPServerType server = Simplified.getReaderHTTPServer();
    server.startIfNecessaryForPackage(p, this);
  }

  private double currentBookProgress() {
    return (double) this.view_progress_bar.getProgress() / (double) this.view_progress_bar.getMax();
  }

  private double currentChapterProgress() {
    if (this.current_page_count > 0) {
      return (float) current_page_index / current_page_count;
    } else {
      return 0.0;
    }
  }

  private String getDeviceIDString() {
    return this.current_account.credentials().accept(
      new OptionVisitorType<AccountAuthenticationCredentials, String>() {
        @Override
        public String none(final None<AccountAuthenticationCredentials> n) {
          return "null";
        }

        @Override
        public String some(final Some<AccountAuthenticationCredentials> someCredentials) {
          return someCredentials.get().adobePostActivationCredentials().accept(
            new OptionVisitorType<AccountAuthenticationAdobePostActivationCredentials, String>() {
              @Override
              public String none(final None<AccountAuthenticationAdobePostActivationCredentials> none) {
                return "null";
              }

              @Override
              public String some(final Some<AccountAuthenticationAdobePostActivationCredentials> someCredentials) {
                return someCredentials.get().deviceID().getValue();
              }
            });
        }
      });
  }

  private @Nullable
  ReaderBookLocation createReaderLocation(final BookmarkAnnotation bm) {
    final String loc_value = bm.getTarget().getSelector().getValue(); //raw content cfi
    try {
      return ReaderBookLocationJSON.deserializeFromString(this.json_mapper, loc_value);
    } catch (Exception e) {
      ErrorDialogUtilities.showError(
        this,
        LOG,
        getString(R.string.bookmark_navigation_error), null);
      return null;
    }
  }

  @Override
  public void onMediaOverlayIsAvailable(final boolean available) {
    LOG.debug("media overlay status changed: available: {}", available);

    final ViewGroup in_media_hud = Objects.requireNonNull(this.view_media);
    final TextView in_title = Objects.requireNonNull(this.view_title_text);
    UIThread.runOnUIThread(() -> {
      in_media_hud.setVisibility(available ? View.VISIBLE : View.GONE);
      in_title.setVisibility(available ? View.GONE : View.VISIBLE);
    });
  }

  @Override
  public void onMediaOverlayIsAvailableError(final Throwable x) {
    LOG.error("{}", x.getMessage(), x);
  }

  @Override
  public void onReadiumFunctionDispatchError(final Throwable x) {
    LOG.error("{}", x.getMessage(), x);
  }

  @Override
  public void onReadiumFunctionInitialize() {
    LOG.debug("readium initialize requested");

    final ReaderHTTPServerType hs =
      Simplified.getReaderHTTPServer();
    final Container c =
      Objects.requireNonNull(this.epub_container);
    final Package p =
      Objects.requireNonNull(c.getDefaultPackage());

    p.setRootUrls(hs.getURIBase().toString(), null);

    final ReaderReadiumJavaScriptAPIType js =
      Objects.requireNonNull(this.readium_js_api);

    // is this correct? inject fonts before book opens or after
    js.injectFonts();

    /*
     * If there's a bookmark for the current book, send a request to open the
     * book to that specific page. Otherwise, start at the beginning.
     */

    final ReaderBookmark location = this.formatHandle.getFormat().getLastReadLocation();
    if (location != null) {
      navigateTo(location);
    }

    /*
     * Configure the visibility of UI elements.
     */

    final WebView in_web_view =
      Objects.requireNonNull(this.view_web_view);
    final ProgressBar in_loading =
      Objects.requireNonNull(this.view_loading);
    final ProgressBar in_progress_bar =
      Objects.requireNonNull(this.view_progress_bar);
    final TextView in_progress_text =
      Objects.requireNonNull(this.view_progress_text);
    final ImageView in_media_play =
      Objects.requireNonNull(this.view_media_play);
    final ImageView in_media_next =
      Objects.requireNonNull(this.view_media_next);
    final ImageView in_media_prev =
      Objects.requireNonNull(this.view_media_prev);

    in_loading.setVisibility(View.GONE);
    in_web_view.setVisibility(View.VISIBLE);
    in_progress_bar.setVisibility(View.VISIBLE);
    in_progress_text.setVisibility(View.INVISIBLE);

    try {
      this.applyReaderPreferences(
        Simplified.getProfilesController()
          .profileCurrent()
          .preferences()
          .readerPreferences());
    } catch (final ProfileNoneCurrentException e) {
      throw new IllegalStateException(e);
    }

    UIThread.runOnUIThread(() -> {
      in_media_play.setOnClickListener(view -> {
        LOG.debug("toggling media overlay");
        js.mediaOverlayToggle();
      });

      in_media_next.setOnClickListener(
        view -> {
          LOG.debug("next media overlay");
          js.mediaOverlayNext();
        });

      in_media_prev.setOnClickListener(
        view -> {
          LOG.debug("previous media overlay");
          js.mediaOverlayPrevious();
        });
    });
  }

  @Override
  public void onReadiumFunctionInitializeError(final Throwable e) {
    ErrorDialogUtilities.showErrorWithRunnable(
      this,
      LOG,
      "Unable to initialize Readium",
      e,
      this::finish);
  }

  /**
   * {@inheritDoc}
   * <p>
   * When the device orientation changes, the configuration change handler
   * {@link #onConfigurationChanged(Configuration)} makes the web view invisible
   * so that the user does not see the now incorrectly-paginated content. When
   * Readium tells the app that the content pagination has changed, it makes the
   * web view visible again.
   */

  @Override
  public void onReadiumFunctionPaginationChanged(final ReaderPaginationChangedEvent e) {
    LOG.debug("pagination changed: {}", e);
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

        this.current_page_index = page.getSpineItemPageIndex();
        this.current_page_count = page.getSpineItemPageCount();
        this.current_chapter_title = default_package.getSpineItem(page.getIDRef()).getTitle();

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
          Objects.requireNonNull(this.readium_js_api);
        readium_js.getCurrentPage(this);
        readium_js.mediaOverlayIsAvailable(this);
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

  @Override
  public void onReadiumFunctionPaginationChangedError(final Throwable x) {
    LOG.error("onReadiumFunctionPaginationChangedError: {}", x.getMessage(), x);
  }

  @Override
  public void onReadiumFunctionSettingsApplied() {
    LOG.debug("received settings applied");
  }

  @Override
  public void onReadiumFunctionSettingsAppliedError(final Throwable e) {
    LOG.error("{}", e.getMessage(), e);
  }

  @Override
  public void onReadiumFunctionUnknown(final String text) {
    LOG.error("unknown readium function: {}", text);
  }

  @Override
  public void onReadiumMediaOverlayStatusChangedIsPlaying(final boolean playing) {
    LOG.debug("media overlay status changed: playing: {}", playing);

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

  @Override
  public void onReadiumMediaOverlayStatusError(
    final Throwable e) {
    LOG.error("{}", e.getMessage(), e);
  }

  @Override
  public void onServerStartFailed(
    final ReaderHTTPServerType hs,
    final Throwable x) {
    ErrorDialogUtilities.showErrorWithRunnable(
      this,
      LOG,
      "Could not start http server.",
      x,
      this::finish);
  }

  @Override
  public void onServerStartSucceeded(
    final ReaderHTTPServerType hs,
    final boolean first) {
    if (first) {
      LOG.debug("http server started");
    } else {
      LOG.debug("http server already running");
    }

    this.makeInitialReadiumRequest(hs);
  }

  @Override
  public void onSimplifiedFunctionDispatchError(
    final Throwable x) {
    LOG.error("{}", x.getMessage(), x);
  }

  @Override
  public void onSimplifiedFunctionUnknown(
    final String text) {
    LOG.error("unknown function: {}", text);
  }

  @Override
  public void onSimplifiedGestureCenter() {
    final ViewGroup in_hud = Objects.requireNonNull(this.view_hud);
    UIThread.runOnUIThread(() -> {
      switch (in_hud.getVisibility()) {
        case View.VISIBLE: {
          fadeOut(in_hud, DEFAULT_FADE_DURATION);
          break;
        }
        case View.INVISIBLE:
        case View.GONE: {
          fadeIn(in_hud, DEFAULT_FADE_DURATION);
          break;
        }
      }
    });
  }

  @Override
  public void onSimplifiedGestureCenterError(final Throwable x) {
    LOG.error("{}", x.getMessage(), x);
  }

  @Override
  public void onSimplifiedGestureLeft() {
    final ReaderReadiumJavaScriptAPIType js = Objects.requireNonNull(this.readium_js_api);
    js.pagePrevious();
  }

  @Override
  public void onSimplifiedGestureLeftError(final Throwable x) {
    LOG.error("{}", x.getMessage(), x);
  }

  @Override
  public void onSimplifiedGestureRight() {
    final ReaderReadiumJavaScriptAPIType js = Objects.requireNonNull(this.readium_js_api);
    js.pageNext();
  }

  @Override
  public void onSimplifiedGestureRightError(final Throwable x) {
    LOG.error("{}", x.getMessage(), x);
  }

  @Override
  public void onTOCSelectionReceived(final TOCElement e) {
    LOG.debug("received TOC selection: {}", e);

    final ReaderReadiumJavaScriptAPIType js = Objects.requireNonNull(this.readium_js_api);
    js.openContentURL(e.getContentRef(), e.getSourceHref());
  }

  @Override
  public void onBookmarkSelectionReceived(final BookmarkAnnotation bm) {
    LOG.debug("received bookmark selection: {}", bm);
    final ReaderBookLocation location = createReaderLocation(bm);
    if (location != null) {
      final ReaderBookmark bookmark =
        new ReaderBookmark(
          this.book_id,
          location,
          LocalDateTime.now(),
          this.current_chapter_title,
          currentChapterProgress(),
          currentBookProgress(),
          getDeviceIDString());
      this.navigateTo(bookmark);
    }
  }

  private void navigateTo(final ReaderBookmark location) {
    final OptionType<ReaderBookmark> optLocation = Option.of(location);
    OptionType<ReaderOpenPageRequestType> page_request = optLocation.map((l) -> {
      LOG.debug("Creating Page Req for: {}", l);
      this.current_location = l;
      return ReaderOpenPageRequest.fromBookLocation(l.getLocation());
    });

    final ReaderReadiumJavaScriptAPIType js =
      Objects.requireNonNull(this.readium_js_api);
    final ReaderReadiumViewerSettings vs =
      Objects.requireNonNull(this.viewer_settings);
    final Container c =
      Objects.requireNonNull(this.epub_container);
    final Package p =
      Objects.requireNonNull(c.getDefaultPackage());

    js.openBook(p, vs, page_request);
  }
}
