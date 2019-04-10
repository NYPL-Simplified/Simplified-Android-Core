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

import com.io7m.jfunctional.None;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.OptionVisitorType;
import com.io7m.jfunctional.Some;
import com.io7m.junreachable.UnreachableCodeException;

import org.jetbrains.annotations.NotNull;
import org.joda.time.LocalDateTime;
import org.nypl.simplified.app.ApplicationColorScheme;
import org.nypl.simplified.app.R;
import org.nypl.simplified.app.Simplified;
import org.nypl.simplified.app.profiles.ProfileTimeOutActivity;
import org.nypl.simplified.app.reader.ReaderPaginationChangedEvent.OpenPage;
import org.nypl.simplified.app.reader.toc.ReaderTOC;
import org.nypl.simplified.app.reader.toc.ReaderTOCActivity;
import org.nypl.simplified.app.reader.toc.ReaderTOCElement;
import org.nypl.simplified.app.reader.toc.ReaderTOCParameters;
import org.nypl.simplified.app.reader.toc.ReaderTOCSelection;
import org.nypl.simplified.app.reader.toc.ReaderTOCSelectionListenerType;
import org.nypl.simplified.app.utilities.ErrorDialogUtilities;
import org.nypl.simplified.app.utilities.UIThread;
import org.nypl.simplified.books.accounts.AccountAuthenticationAdobePostActivationCredentials;
import org.nypl.simplified.books.accounts.AccountAuthenticationCredentials;
import org.nypl.simplified.books.accounts.AccountID;
import org.nypl.simplified.books.accounts.AccountType;
import org.nypl.simplified.books.accounts.AccountsDatabaseNonexistentException;
import org.nypl.simplified.books.book_database.Book;
import org.nypl.simplified.books.book_database.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleEPUB;
import org.nypl.simplified.books.book_database.BookDatabaseException;
import org.nypl.simplified.books.book_database.BookFormat;
import org.nypl.simplified.books.book_database.BookID;
import org.nypl.simplified.books.feeds.FeedEntry.FeedEntryOPDS;
import org.nypl.simplified.books.profiles.ProfileEvent;
import org.nypl.simplified.books.profiles.ProfileNoneCurrentException;
import org.nypl.simplified.books.profiles.ProfilePreferencesChanged;
import org.nypl.simplified.books.reader.ReaderBookLocation;
import org.nypl.simplified.books.reader.ReaderBookmark;
import org.nypl.simplified.books.reader.ReaderColorScheme;
import org.nypl.simplified.books.reader.ReaderPreferences;
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkKind;
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarks;
import org.nypl.simplified.observable.ObservableSubscriptionType;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.readium.sdk.android.Container;
import org.readium.sdk.android.Package;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.util.Collections;
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

  private static final String BOOK_ID =
    "org.nypl.simplified.app.ReaderActivity.book";
  private static final String FILE_ID =
    "org.nypl.simplified.app.ReaderActivity.file";
  private static final String ENTRY =
    "org.nypl.simplified.app.ReaderActivity.entry";
  private static final Logger  LOG =
    LoggerFactory.getLogger(ReaderActivity.class);

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
   * @param book_id  The unique ID of the book
   * @param file  The actual EPUB file
   * @param entry The OPD feed entry
   */

  public static void startActivity(
    final Activity from,
    final BookID book_id,
    final File file,
    final FeedEntryOPDS entry) {
    Objects.requireNonNull(file);
    final Bundle b = new Bundle();
    b.putSerializable(ReaderActivity.BOOK_ID, book_id);
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
    LOG.debug("applyViewerColorScheme: {}", cs);

    final View in_root = Objects.requireNonNull(this.view_root);
    UIThread.runOnUIThread(() -> {
      in_root.setBackgroundColor(ReaderColorSchemes.background(cs));
      this.applyViewerColorFilters();
    });
  }

  private void makeInitialReadiumRequest(final ReaderHTTPServerType hs) {
    final URI reader_uri = URI.create(hs.getURIBase() + "reader.html");
    UIThread.runOnUIThreadDelayed(() -> {
      LOG.debug("makeInitialReadiumRequest: {}", reader_uri);
      this.view_web_view.loadUrl(reader_uri.toString());
    }, 300L);
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
        final Bundle b =
          Objects.requireNonNull(nnd.getExtras());
        final ReaderTOCSelection selection =
          (ReaderTOCSelection) b.getSerializable(ReaderTOCActivity.TOC_SELECTED_ID);
        this.onTOCItemSelected(selection);
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
      this.readium_js_api.getCurrentPage(this);
      this.readium_js_api.mediaOverlayIsAvailable(this);
    }, 300L);
  }

  @SuppressLint("SetJavaScriptEnabled")
  @Override
  protected void onCreate(final @Nullable Bundle state) {
    this.setTheme(Simplified.getMainColorScheme().getActivityThemeResourceWithoutActionBar());

    super.onCreate(state);
    this.setContentView(R.layout.reader);

    LOG.debug("onCreate: starting");

    final Intent i = Objects.requireNonNull(this.getIntent());
    final Bundle a = Objects.requireNonNull(i.getExtras());

    final File in_epub_file =
      Objects.requireNonNull((File) a.getSerializable(ReaderActivity.FILE_ID));
    this.book_id =
      Objects.requireNonNull((BookID) a.getSerializable(ReaderActivity.BOOK_ID));
    final FeedEntryOPDS entry =
      Objects.requireNonNull((FeedEntryOPDS) a.getSerializable(ReaderActivity.ENTRY));
    this.feed_entry = entry.getFeedEntry();

    LOG.debug("onCreate: epub file:  {}", in_epub_file);
    LOG.debug("onCreate: book id:    {}", this.book_id);
    LOG.debug("onCreate: entry id:   {}", entry.getFeedEntry().getID());

    try {
      this.current_account =
        Simplified.getProfilesController()
          .profileAccountForBook(this.book_id);
    } catch (ProfileNoneCurrentException | AccountsDatabaseNonexistentException e) {
      this.onEPUBLoadFailed(e);
      this.finish();
      return;
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
      this.onEPUBLoadFailed(e);
      this.finish();
      return;
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
        this.current_account.bookDatabase()
          .entry(this.book_id)
          .findFormatHandle(BookDatabaseEntryFormatHandleEPUB.class);
    } catch (BookDatabaseException e) {
      this.onEPUBLoadFailed(e);
      this.finish();
      return;
    }

    Objects.requireNonNull(this.formatHandle, "formatHandle");

    final BookFormat.BookFormatEPUB format = this.formatHandle.getFormat();
    final ReaderReadiumEPUBLoaderType loader = Simplified.getReadiumEPUBLoader();
    final ReaderReadiumEPUBLoadRequest request =
      ReaderReadiumEPUBLoadRequest.builder(in_epub_file)
        .setAdobeRightsFile(Option.of(format.getAdobeRightsFile()))
        .build();

    LOG.debug("onCreate: loading EPUB");
    loader.loadEPUB(request, this);
    LOG.debug("onCreate: applying viewer color filters");
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
      LOG.debug("onProfileEventPreferencesChanged: reader settings changed");

      final ReaderPreferences preferences =
        Simplified.getProfilesController()
          .profileCurrent()
          .preferences()
          .readerPreferences();

      applyReaderPreferences(preferences);
    }
  }

  private void applyReaderPreferences(final ReaderPreferences preferences) {
    LOG.debug("applyReaderPreferences: scheduling task to run later");

    UIThread.runOnUIThreadDelayed(() -> {
      LOG.debug("applyReaderPreferences: executing now");

      // Get the CFI from the ReadiumSDK before applying the new
      // page style settings.
      this.simplified_js_api.getReadiumCFI();

      this.readium_js_api.setPageStyleSettings(preferences);

      // Once they are applied, go to the CFI that is stored in the
      // JS ReadiumSDK instance.
      this.simplified_js_api.setReadiumCFI();

      this.applyViewerColorScheme(preferences.colorScheme());

      this.readium_js_api.getCurrentPage(this);
      this.readium_js_api.mediaOverlayIsAvailable(this);
    }, 300L);
  }

  @Override
  public void onCurrentPageError(final Throwable x) {
    LOG.error("onCurrentPageError: {}", x.getMessage(), x);
  }

  @Override
  public void onCurrentPageReceived(final ReaderBookLocation location) {
    Objects.requireNonNull(location);
    LOG.debug("onCurrentPageReceived: {}", location);

    final ReaderBookmark bookmark =
      new ReaderBookmark(
        this.feed_entry.getID(),
        location,
        ReaderBookmarkKind.ReaderBookmarkLastReadLocation.INSTANCE,
        LocalDateTime.now(),
        this.current_chapter_title,
        currentChapterProgress(),
        currentBookProgress(),
        getDeviceIDString(),
        null);

    this.current_location = bookmark;

    Simplified.getReaderBookmarksService().bookmarkCreate(this.current_account.id(), bookmark);
  }

  @Override
  protected void onPause() {
    super.onPause();

    Simplified.getReaderBookmarksService()
      .bookmarkCreate(this.current_account.id(), this.current_location);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

    this.readium_js_api.getCurrentPage(this);
    this.readium_js_api.mediaOverlayIsAvailable(this);

    final ObservableSubscriptionType<ProfileEvent> sub = this.profile_subscription;
    if (sub != null) {
      sub.unsubscribe();
    }
    this.profile_subscription = null;
  }

  @Override
  public void onEPUBLoadFailed(final Throwable x) {
    ErrorDialogUtilities.showErrorWithRunnable(
      this, LOG,
      "Could not load EPUB file",
      x,
      this::finish);
  }

  @Override
  public void onEPUBLoadSucceeded(final Container c) {
    LOG.debug("onEPUBLoadSucceeded: {}", c.getName());

    this.epub_container = c;
    final Package p = Objects.requireNonNull(c.getDefaultPackage());

    final TextView in_title_text = Objects.requireNonNull(this.view_title_text);
    UIThread.runOnUIThread(() -> in_title_text.setText(Objects.requireNonNull(p.getTitle())));

    /*
     * Configure the TOC and Bookmark buttons.
     */

    UIThread.runOnUIThread(() -> {
      this.view_toc.setOnClickListener(v -> {
        final ReaderBookmarks bookmarks = loadBookmarks();

        final ReaderTOC toc =
          ReaderTOC.Companion.fromPackage(p);
        final ReaderTOCParameters parameters =
          new ReaderTOCParameters(bookmarks, toc.getElements());

        ReaderTOCActivity.Companion.startActivityForResult(ReaderActivity.this, parameters);
        this.overridePendingTransition(0, 0);
      });

      this.view_bookmark.setOnClickListener(v -> {
        Simplified.getReaderBookmarksService()
          .bookmarkCreate(this.current_account.id(), this.current_location.toExplicit());
      });
    });

    /*
     * Get a reference to the web server. Start it if necessary (the callbacks
     * will still be executed if the server is already running).
     */

    final ReaderHTTPServerType server = Simplified.getReaderHTTPServer();
    server.startIfNecessaryForPackage(p, this);
  }

  private ReaderBookmarks loadBookmarks() {
    try {
      return Simplified.getReaderBookmarksService()
        .bookmarkLoad(this.current_account.id(), this.book_id)
        .get(10L, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      LOG.error("could not load bookmarks: ", e);
      return new ReaderBookmarks(null, Collections.emptyList());
    } catch (ExecutionException e) {
      LOG.error("could not load bookmarks: ", e);
      return new ReaderBookmarks(null, Collections.emptyList());
    } catch (TimeoutException e) {
      LOG.error("could not load bookmarks: ", e);
      return new ReaderBookmarks(null, Collections.emptyList());
    }
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

  @Override
  public void onMediaOverlayIsAvailable(final boolean available) {
    LOG.debug("onMediaOverlayIsAvailable: available: {}", available);

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
    LOG.debug("onReadiumFunctionInitialize: readium initialize requested");

    final ReaderHTTPServerType hs =
      Simplified.getReaderHTTPServer();
    final Container c =
      Objects.requireNonNull(this.epub_container);
    final Package p =
      Objects.requireNonNull(c.getDefaultPackage());

    p.setRootUrls(hs.getURIBase().toString(), null);

    LOG.debug("onReadiumFunctionInitialize: scheduling font injection");
    // is this correct? inject fonts before book opens or after
    UIThread.runOnUIThreadDelayed(() -> {
      LOG.debug("onReadiumFunctionInitialize: injecting fonts now");
      this.readium_js_api.injectFonts();
    }, 300L);

    /*
     * If there's a bookmark for the current book, send a request to open the
     * book to that specific page. Otherwise, start at the beginning.
     */

    navigateTo(Option.of(this.formatHandle.getFormat().getLastReadLocation()));

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
        this.readium_js_api.mediaOverlayToggle();
      });

      in_media_next.setOnClickListener(
        view -> {
          LOG.debug("next media overlay");
          this.readium_js_api.mediaOverlayNext();
        });

      in_media_prev.setOnClickListener(
        view -> {
          LOG.debug("previous media overlay");
          this.readium_js_api.mediaOverlayPrevious();
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
    LOG.debug("onReadiumFunctionPaginationChanged: {}", e);
    final WebView in_web_view = Objects.requireNonNull(this.view_web_view);

    /*
     * Configure the progress bar and text.
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
       * Ask for Readium to deliver the unique identifier of the current page,
       * and tell Simplified that the page has changed and so any Javascript
       * state should be reconfigured.
       */

      UIThread.runOnUIThreadDelayed(() -> {
        this.readium_js_api.getCurrentPage(this);
        this.readium_js_api.mediaOverlayIsAvailable(this);
      }, 300L);
    });

    /*
     * Make the web view visible with a slight delay (as sometimes a
     * pagination-change event will be sent even though the content has not
     * yet been laid out in the web view). Only do this if the screen
     * orientation has just changed.
     */

    if (this.web_view_resized) {
      this.web_view_resized = false;
      UIThread.runOnUIThreadDelayed(() -> {
        in_web_view.setVisibility(View.VISIBLE);
        in_progress_bar.setVisibility(View.VISIBLE);
        in_progress_text.setVisibility(View.VISIBLE);
        this.simplified_js_api.pageHasChanged();
      }, 300L);
    } else {
      UIThread.runOnUIThread(this.simplified_js_api::pageHasChanged);
    }
  }

  @Override
  public void onReadiumFunctionPaginationChangedError(final Throwable x) {
    LOG.error("onReadiumFunctionPaginationChangedError: {}", x.getMessage(), x);
  }

  @Override
  public void onReadiumFunctionSettingsApplied() {
    LOG.debug("onReadiumFunctionSettingsApplied");
  }

  @Override
  public void onReadiumFunctionSettingsAppliedError(final Throwable e) {
    LOG.error("onReadiumFunctionSettingsAppliedError: {}", e.getMessage(), e);
  }

  @Override
  public void onReadiumFunctionUnknown(final String text) {
    LOG.error("onReadiumFunctionUnknown: unknown readium function: {}", text);
  }

  @Override
  public void onReadiumMediaOverlayStatusChangedIsPlaying(final boolean playing) {
    LOG.debug("onReadiumMediaOverlayStatusChangedIsPlaying: playing: {}", playing);

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
  public void onReadiumMediaOverlayStatusError(final Throwable e) {
    LOG.error("onReadiumMediaOverlayStatusError: {}", e.getMessage(), e);
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
      LOG.debug("onServerStartSucceeded: http server started");
    } else {
      LOG.debug("onServerStartSucceeded: http server already running");
    }

    this.makeInitialReadiumRequest(hs);
  }

  @Override
  public void onSimplifiedFunctionDispatchError(final Throwable x) {
    LOG.error("onSimplifiedFunctionDispatchError: {}", x.getMessage(), x);
  }

  @Override
  public void onSimplifiedFunctionUnknown(final String text) {
    LOG.error("onSimplifiedFunctionUnknown: unknown function: {}", text);
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
    LOG.error("onSimplifiedGestureCenterError: {}", x.getMessage(), x);
  }

  @Override
  public void onSimplifiedGestureLeft() {
    this.readium_js_api.pagePrevious();
  }

  @Override
  public void onSimplifiedGestureLeftError(final Throwable x) {
    LOG.error("onSimplifiedGestureLeftError: {}", x.getMessage(), x);
  }

  @Override
  public void onSimplifiedGestureRight() {
    this.readium_js_api.pageNext();
  }

  @Override
  public void onSimplifiedGestureRightError(final Throwable x) {
    LOG.error("onSimplifiedGestureRightError: {}", x.getMessage(), x);
  }

  private void onTOCSelectionReceived(final ReaderTOCElement e) {
    LOG.debug("onTOCSelectionReceived: received TOC selection: {}", e);
    this.readium_js_api.openContentURL(e.getContentRef(), e.getSourceHref());
  }

  private void onBookmarkSelectionReceived(final ReaderBookmark bookmark) {
    LOG.debug("onTOCBookmarkSelectionReceived: received bookmark selection: {}", bookmark);
    this.navigateTo(Option.some(bookmark));
  }

  private void navigateTo(final OptionType<ReaderBookmark> location) {
    LOG.debug("navigateTo: {}", location);

    OptionType<ReaderOpenPageRequestType> page_request = location.map((actual) -> {
      LOG.debug("navigateTo: Creating Page Req for: {}", actual);
      this.current_location = actual;
      return ReaderOpenPageRequest.fromBookLocation(actual.getLocation());
    });

    this.readium_js_api.openBook(
      this.epub_container.getDefaultPackage(),
      this.viewer_settings,
      page_request);
  }

  @Override
  public void onTOCItemSelected(final @NotNull ReaderTOCSelection selection) {
    if (selection instanceof ReaderTOCSelection.ReaderSelectedBookmark) {
      final ReaderTOCSelection.ReaderSelectedBookmark bookmark =
        (ReaderTOCSelection.ReaderSelectedBookmark) selection;
      this.onBookmarkSelectionReceived(bookmark.getReaderBookmark());
    } else if (selection instanceof ReaderTOCSelection.ReaderSelectedTOCElement) {
      final ReaderTOCSelection.ReaderSelectedTOCElement element =
        (ReaderTOCSelection.ReaderSelectedTOCElement) selection;
      this.onTOCSelectionReceived(element.component1());
    } else {
      throw new UnreachableCodeException();
    }
  }
}
