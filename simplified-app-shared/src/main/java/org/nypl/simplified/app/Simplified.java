package org.nypl.simplified.app;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.multidex.MultiDexApplication;
import android.util.DisplayMetrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.squareup.picasso.Picasso;

import org.nypl.drm.core.AdobeAdeptExecutorType;
import org.nypl.simplified.analytics.api.Analytics;
import org.nypl.simplified.analytics.api.AnalyticsType;
import org.nypl.simplified.app.catalog.CatalogCoverBadgeImages;
import org.nypl.simplified.app.helpstack.Helpstack;
import org.nypl.simplified.app.helpstack.HelpstackType;
import org.nypl.simplified.app.images.ImageAccountIconRequestHandler;
import org.nypl.simplified.app.reader.ReaderHTTPMimeMap;
import org.nypl.simplified.app.reader.ReaderHTTPMimeMapType;
import org.nypl.simplified.app.reader.ReaderHTTPServerAAsync;
import org.nypl.simplified.app.reader.ReaderHTTPServerType;
import org.nypl.simplified.app.reader.ReaderReadiumEPUBLoader;
import org.nypl.simplified.app.reader.ReaderReadiumEPUBLoaderType;
import org.nypl.simplified.books.accounts.AccountAuthenticationCredentialsStore;
import org.nypl.simplified.books.accounts.AccountAuthenticationCredentialsStoreType;
import org.nypl.simplified.books.accounts.AccountBundledCredentialsEmpty;
import org.nypl.simplified.books.accounts.AccountBundledCredentialsJSON;
import org.nypl.simplified.books.accounts.AccountBundledCredentialsType;
import org.nypl.simplified.books.accounts.AccountEvent;
import org.nypl.simplified.books.accounts.AccountProvider;
import org.nypl.simplified.books.accounts.AccountProviderCollection;
import org.nypl.simplified.books.accounts.AccountProvidersJSON;
import org.nypl.simplified.books.accounts.AccountType;
import org.nypl.simplified.books.accounts.AccountsDatabases;
import org.nypl.simplified.books.analytics.AnalyticsLogger;
import org.nypl.simplified.books.authentication_document.AuthenticationDocumentValuesType;
import org.nypl.simplified.books.book_database.BookFormats;
import org.nypl.simplified.books.book_registry.BookRegistry;
import org.nypl.simplified.books.book_registry.BookRegistryReadableType;
import org.nypl.simplified.books.book_registry.BookRegistryType;
import org.nypl.simplified.books.bundled_content.BundledContentResolverType;
import org.nypl.simplified.books.clock.Clock;
import org.nypl.simplified.books.clock.ClockType;
import org.nypl.simplified.books.controller.AnalyticsControllerType;
import org.nypl.simplified.books.controller.BooksControllerType;
import org.nypl.simplified.books.controller.Controller;
import org.nypl.simplified.books.controller.ProfilesControllerType;
import org.nypl.simplified.books.covers.BookCoverBadgeLookupType;
import org.nypl.simplified.books.covers.BookCoverGenerator;
import org.nypl.simplified.books.covers.BookCoverProvider;
import org.nypl.simplified.books.covers.BookCoverProviderType;
import org.nypl.simplified.books.document_store.DocumentStore;
import org.nypl.simplified.books.document_store.DocumentStoreBuilderType;
import org.nypl.simplified.books.document_store.DocumentStoreType;
import org.nypl.simplified.books.feeds.FeedHTTPTransport;
import org.nypl.simplified.books.feeds.FeedLoader;
import org.nypl.simplified.books.feeds.FeedLoaderType;
import org.nypl.simplified.books.profiles.ProfileDatabaseException;
import org.nypl.simplified.books.profiles.ProfileEvent;
import org.nypl.simplified.books.profiles.ProfileType;
import org.nypl.simplified.books.profiles.ProfilesDatabase;
import org.nypl.simplified.books.profiles.ProfilesDatabaseType;
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkEvent;
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkHTTPCalls;
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkService;
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkServiceProviderType;
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkServiceType;
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkServiceUsableType;
import org.nypl.simplified.branding.BrandingThemeOverrideServiceType;
import org.nypl.simplified.bugsnag.IfBugsnag;
import org.nypl.simplified.cardcreator.CardCreator;
import org.nypl.simplified.downloader.core.DownloaderHTTP;
import org.nypl.simplified.downloader.core.DownloaderType;
import org.nypl.simplified.files.DirectoryUtilities;
import org.nypl.simplified.http.core.HTTP;
import org.nypl.simplified.http.core.HTTPAuthType;
import org.nypl.simplified.http.core.HTTPType;
import org.nypl.simplified.observable.Observable;
import org.nypl.simplified.observable.ObservableType;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParser;
import org.nypl.simplified.opds.core.OPDSAuthenticationDocumentParser;
import org.nypl.simplified.opds.core.OPDSAuthenticationDocumentParserType;
import org.nypl.simplified.opds.core.OPDSFeedParser;
import org.nypl.simplified.opds.core.OPDSFeedParserType;
import org.nypl.simplified.opds.core.OPDSFeedTransportType;
import org.nypl.simplified.opds.core.OPDSSearchParser;
import org.nypl.simplified.opds.core.OPDSSearchParserType;
import org.nypl.simplified.tenprint.TenPrintGenerator;
import org.nypl.simplified.tenprint.TenPrintGeneratorType;
import org.nypl.simplified.theme.ThemeControl;
import org.nypl.simplified.theme.ThemeValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.SortedMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Global application state.
 */

public final class Simplified extends MultiDexApplication {

  private static final Logger LOG = LoggerFactory.getLogger(Simplified.class);
  private static volatile Simplified INSTANCE;

  private CardCreator cardcreator;
  private ListeningScheduledExecutorService exec_catalog_feeds;
  private ListeningScheduledExecutorService exec_covers;
  private ListeningScheduledExecutorService exec_downloader;
  private ListeningScheduledExecutorService exec_books;
  private ListeningScheduledExecutorService exec_epub;
  private ListeningScheduledExecutorService exec_background;
  private ListeningScheduledExecutorService exec_profile_timer;
  private ScreenSizeInformation screen;
  private File directory_base;
  private File directory_documents;
  private File directory_downloads;
  private File directory_profiles;
  private File directory_analytics;
  private AnalyticsLogger analytics_logger;
  private OptionType<AdobeAdeptExecutorType> adobe_drm;
  private BookCoverGenerator cover_generator;
  private HTTPType http;
  private DownloaderType downloader;
  private ReaderReadiumEPUBLoaderType epub_loader;
  private ReaderHTTPMimeMapType mime;
  private ReaderHTTPServerType httpd;
  private BookCoverProviderType cover_provider;
  private OptionType<HelpstackType> helpstack;
  private ClockType clock;
  private DocumentStoreType documents;
  private OPDSFeedParserType feed_parser;
  private OPDSSearchParserType feed_search_parser;
  private OPDSFeedTransportType<OptionType<HTTPAuthType>> feed_transport;
  private FeedLoaderType feed_loader;
  private ProfilesDatabaseType profiles;
  private AccountProviderCollection account_providers;
  private NetworkConnectivity network_connectivity;
  private BookRegistryType book_registry;
  private Controller book_controller;
  private BundledContentResolverType bundled_content_resolver;
  private BookCoverBadgeLookupType cover_badges;
  private ReaderBookmarkServiceType readerBookmarksService;
  private Picasso local_image_loader;
  private OptionType<ThemeValue> branding_theme_override;
  private AccountBundledCredentialsType bundled_credentials;
  private AccountAuthenticationCredentialsStoreType account_credentials_store;
  private AnalyticsType analytics;

  /**
   * A specification of whether or not an action bar is wanted in an activity.
   */

  public enum WantActionBar {

    /**
     * An action bar is required.
     */

    WANT_ACTION_BAR,

    /**
     * No action bar is required.
     */

    WANT_NO_ACTION_BAR
  }

  /**
   * Construct the application.
   */

  public Simplified() {

  }

  private static Simplified checkInitialized() {
    final Simplified i = Simplified.INSTANCE;
    if (i == null) {
      throw new IllegalStateException("Application is not yet initialized");
    }
    return i;
  }

  /**
   * @return The local image loader
   */

  public static Picasso getLocalImageLoader() {
    final Simplified i = Simplified.checkInitialized();
    return i.local_image_loader;
  }

  /**
   * @return The HTTP interface
   */

  public static HTTPType getHTTP() {
    final Simplified i = Simplified.checkInitialized();
    return i.http;
  }

  /**
   * @return The Card Creator
   */

  public static CardCreator getCardCreator() {
    final Simplified i = Simplified.checkInitialized();
    return i.cardcreator;
  }

  /**
   * @return The account providers
   */

  public static AccountProviderCollection getAccountProviders() {
    final Simplified i = Simplified.checkInitialized();
    return i.account_providers;
  }

  /**
   * @return The network connectivity interface
   */

  public static NetworkConnectivityType getNetworkConnectivity() {
    final Simplified i = Simplified.checkInitialized();
    return i.network_connectivity;
  }

  /**
   * @return The screen size controller interface
   */

  public static ScreenSizeInformationType getScreenSizeInformation() {
    final Simplified i = Simplified.checkInitialized();
    return i.screen;
  }

  /**
   * @return The book cover provider
   */

  public static BookCoverProviderType getCoverProvider() {
    final Simplified i = Simplified.checkInitialized();
    return i.cover_provider;
  }

  /**
   * @return The profiles controller
   */

  public static ProfilesControllerType getProfilesController() {
    final Simplified i = Simplified.checkInitialized();
    return i.book_controller;
  }

  /**
   * @return The analytics controller
   */

  public static AnalyticsControllerType getAnalyticsController() {
    final Simplified i = Simplified.checkInitialized();
    return i.book_controller;
  }

  /**
   * @return The books controller
   */

  public static BooksControllerType getBooksController() {
    final Simplified i = Simplified.checkInitialized();
    return i.book_controller;
  }

  /**
   * @return The reader bookmarks service
   */

  public static ReaderBookmarkServiceUsableType getReaderBookmarksService() {
    final Simplified i = Simplified.checkInitialized();
    return i.readerBookmarksService;
  }

  /**
   * @return A general executor service for background tasks
   */

  public static ListeningScheduledExecutorService getBackgroundTaskExecutor() {
    final Simplified i = Simplified.checkInitialized();
    return i.exec_background;
  }

  /**
   * @return The books registry
   */

  public static BookRegistryReadableType getBooksRegistry() {
    final Simplified i = Simplified.checkInitialized();
    return i.book_registry;
  }

  /**
   * @return The feed loader
   */

  public static FeedLoaderType getFeedLoader() {
    final Simplified i = Simplified.checkInitialized();
    return i.feed_loader;
  }

  /**
   * @return The Helpstack interface, if one is available
   */

  public static OptionType<HelpstackType> getHelpStack() {
    final Simplified i = Simplified.checkInitialized();
    return i.helpstack;
  }

  /**
   * @return The EPUB loader
   */

  public static ReaderReadiumEPUBLoaderType getReadiumEPUBLoader() {
    final Simplified i = Simplified.checkInitialized();
    return i.epub_loader;
  }

  /**
   * @return The HTTP server for the reader
   */

  public static ReaderHTTPServerType getReaderHTTPServer() {
    final Simplified i = Simplified.checkInitialized();
    return i.httpd;
  }

  /**
   * @return The HTTP server for the reader
   */

  public static AnalyticsType getAnalytics() {
    final Simplified i = Simplified.checkInitialized();
    return i.analytics;
  }

  @NonNull
  private static File determineDiskDataDirectory(
    final Context context) {

    /*
     * If external storage is mounted and is on a device that doesn't allow
     * the storage to be removed, use the external storage for data.
     */

    if (Environment.MEDIA_MOUNTED.equals(
      Environment.getExternalStorageState())) {

      LOG.debug("trying external storage");
      if (!Environment.isExternalStorageRemovable()) {
        final File r = context.getExternalFilesDir(null);
        LOG.debug("external storage is not removable, using it ({})", r);
        Preconditions.checkArgument(r.isDirectory(), "Data directory {} is a directory", r);
        return NullCheck.notNull(r);
      }
    }

    /*
     * Otherwise, use internal storage.
     */

    final File r = context.getFilesDir();
    LOG.debug("no non-removable external storage, using internal storage ({})", r);
    Preconditions.checkArgument(r.isDirectory(), "Data directory {} is a directory", r);
    return NullCheck.notNull(r);
  }

  private static ListeningScheduledExecutorService createNamedThreadPool(
    final int count,
    final String base,
    final int priority) {

    LOG.debug("creating named thread pool: {} ({} threads at priority {})", base, count, priority);

    final ThreadFactory tf = Executors.defaultThreadFactory();

    final ThreadFactory named = createNamedThreadFactory(base, priority, tf);

    return MoreExecutors.listeningDecorator(Executors.newScheduledThreadPool(count, named));
  }

  @NonNull
  private static ThreadFactory createNamedThreadFactory(
    final String name,
    final int priority,
    final ThreadFactory base) {
    return new ThreadFactory() {
      private int id;

      @Override
      public Thread newThread(final @Nullable Runnable runnable) {

        /*
         * Apparently, it's necessary to use {@link android.os.Process} to set
         * the thread priority, rather than the standard Java thread
         * functions.
         */

        final Thread thread = base.newThread(
          () -> {
            android.os.Process.setThreadPriority(priority);
            NullCheck.notNull(runnable).run();
          });
        thread.setName(String.format("simplified-%s-tasks-%d", name, this.id));
        ++this.id;
        return thread;
      }
    };
  }

  public static DocumentStoreType getDocumentStore() {
    final Simplified i = Simplified.checkInitialized();
    return i.documents;
  }

  public static ThemeValue getCurrentTheme() {
    final Simplified i = Simplified.checkInitialized();

    if (i.branding_theme_override.isSome()) {
      return ((Some<ThemeValue>) i.branding_theme_override).get();
    }

    final OptionType<ProfileType> currentProfileOpt = i.profiles.currentProfile();
    if (currentProfileOpt.isSome()) {
      final ProfileType currentProfile =
        ((Some<ProfileType>) currentProfileOpt).get();
      final AccountType accountCurrent =
        currentProfile.accountCurrent();
      final ThemeValue theme =
        ThemeControl.getThemesByName().get(accountCurrent.provider().mainColor());
      if (theme != null) {
        return theme;
      }
    }

    return ThemeControl.getThemeFallback();
  }

  private static int fetchUnusedHTTPPort() {
    // Fallback port
    Integer port = 8080;
    try {
      final ServerSocket s = new ServerSocket(0);
      port = s.getLocalPort();
      s.close();
    } catch (final IOException e) {
      // Ignore
    }

    LOG.debug("HTTP server will run on port {}", port);
    return port;
  }

  private static AccountProviderCollection createAccountProviders(
    final Resources resources,
    final AssetManager asset_manager)
    throws IOException {

    try (InputStream stream = asset_manager.open("Accounts.json")) {
      final AccountProviderCollection providers =
        AccountProvidersJSON.deserializeFromStream(stream);

      /*
       * If a default provider is specified, use that as the default.
       */

      final String default_uri_text = resources.getString(R.string.feature_default_provider_uri);
      if (!default_uri_text.isEmpty()) {
        try {
          final URI default_uri = new URI(default_uri_text);
          final SortedMap<URI, AccountProvider> available = providers.providers();
          if (available.containsKey(default_uri)) {
            LOG.debug("using default provider {}", default_uri);
            return AccountProviderCollection.create(available.get(default_uri), available);
          } else {
            LOG.error(
              "specified feature_default_provider_uri {} but that provider does not exist",
              default_uri_text);
          }
        } catch (URISyntaxException e) {
          LOG.error("could not parse feature_default_provider_uri: ", e);
        }
      } else {
        LOG.debug("no default provider is specified");
      }

      return providers;
    }
  }

  private static ProfilesDatabaseType createProfileDatabase(
    final Context context,
    final Resources resources,
    final ObservableType<AccountEvent> account_events,
    final ObservableType<ProfileEvent> profile_events,
    final AccountProviderCollection account_providers,
    final AccountBundledCredentialsType account_bundled_credentials,
    final AccountAuthenticationCredentialsStoreType account_credentials_store,
    final File directory)
    throws ProfileDatabaseException {

    /*
     * If profiles are enabled, then disable the anonymous profile.
     */

    final boolean anonymous = !resources.getBoolean(R.bool.feature_profiles_enabled);

    if (anonymous) {
      LOG.debug("opening profile database with anonymous profile");
      return ProfilesDatabase.openWithAnonymousProfileEnabled(
        context,
        account_events,
        account_providers,
        account_bundled_credentials,
        account_credentials_store,
        AccountsDatabases.INSTANCE,
        account_providers.providerDefault(),
        directory);
    }

    LOG.debug("opening profile database without anonymous profile");
    return ProfilesDatabase.openWithAnonymousProfileDisabled(
      context,
      account_events,
      account_providers,
      account_bundled_credentials,
      account_credentials_store,
      AccountsDatabases.INSTANCE,
      directory);
  }

  @NonNull
  private static OPDSFeedParserType createFeedParser() {
    return OPDSFeedParser.newParser(
      OPDSAcquisitionFeedEntryParser.newParser(BookFormats.Companion.supportedBookMimeTypes()));
  }

  /**
   * Create a document store and conditionally enable each of the documents based on the
   * presence of assets.
   */

  private static DocumentStoreType createDocumentStore(
    final AssetManager assets,
    final Resources resources,
    final ClockType clock,
    final HTTPType http,
    final ExecutorService exec,
    final File directory) {

    final OPDSAuthenticationDocumentParserType auth_doc_parser =
      OPDSAuthenticationDocumentParser.get();

    /*
     * Default authentication document values.
     */

    final AuthenticationDocumentValuesType auth_doc_values =
      new AuthenticationDocumentValuesType() {
        @Override
        public String getLabelLoginUserID() {
          return resources.getString(R.string.settings_barcode);
        }

        @Override
        public String getLabelLoginPassword() {
          return resources.getString(R.string.settings_pin);
        }

        @Override
        public String getLabelLoginPatronName() {
          return resources.getString(R.string.settings_name);
        }
      };

    final DocumentStoreBuilderType documents_builder =
      DocumentStore.newBuilder(clock, http, exec, directory, auth_doc_values, auth_doc_parser);

    try {
      final InputStream stream = assets.open("eula.html");
      documents_builder.enableEULA(x -> stream);
    } catch (final IOException e) {
      LOG.debug("No EULA defined: ", e);
    }

    try {
      final InputStream stream = assets.open("software-licenses.html");
      documents_builder.enableLicenses(x -> stream);
    } catch (final IOException e) {
      LOG.debug("No licenses defined: ", e);
    }

    return documents_builder.build();
  }

  private void initBugsnag(
    final OptionType<String> api_token_opt) {
    if (api_token_opt.isSome()) {
      final String api_token = ((Some<String>) api_token_opt).get();
      LOG.debug("IfBugsnag: init live interface");
      IfBugsnag.init(this, api_token);
    } else {
      LOG.debug("IfBugsnag: init no-op interface");
      IfBugsnag.init();
    }
  }

  @Override
  public void onCreate() {
    super.onCreate();

    LOG.debug("starting app: pid {}", android.os.Process.myPid());
    final Resources resources = this.getResources();
    final AssetManager asset_manager = this.getAssets();

    LOG.debug("build: {}", BuildConfig.GIT_COMMIT);

    LOG.debug("creating thread pools");
    this.exec_catalog_feeds =
      Simplified.createNamedThreadPool(1, "catalog-feed", 19);
    this.exec_covers =
      Simplified.createNamedThreadPool(2, "cover", 19);
    this.exec_downloader =
      Simplified.createNamedThreadPool(4, "downloader", 19);
    this.exec_books =
      Simplified.createNamedThreadPool(1, "books", 19);
    this.exec_epub =
      Simplified.createNamedThreadPool(1, "epub", 19);
    this.exec_background =
      MoreExecutors.listeningDecorator(
        Simplified.createNamedThreadPool(1, "background", 19));
    this.exec_profile_timer =
      Simplified.createNamedThreadPool(1, "profile-timer", 19);

    LOG.debug("initializing Bugsnag");
    this.initBugsnag(Bugsnag.getApiToken(asset_manager));

    LOG.debug("initializing DRM (if required)");
    this.adobe_drm =
      AdobeDRMServices.newAdobeDRMOptional(this, AdobeDRMServices.getPackageOverride(resources));

    this.screen = new ScreenSizeInformation(LOG, resources);

    LOG.debug("initializing directories");
    this.directory_base = determineDiskDataDirectory(this);
    this.directory_downloads = new File(this.directory_base, "downloads");
    this.directory_documents = new File(this.directory_base, "documents");
    this.directory_profiles = new File(this.directory_base, "profiles");
    this.directory_analytics = new File(this.directory_base, "analytics");

    LOG.debug("directory_base:      {}", this.directory_base);
    LOG.debug("directory_downloads: {}", this.directory_downloads);
    LOG.debug("directory_documents: {}", this.directory_documents);
    LOG.debug("directory_profiles:  {}", this.directory_profiles);
    LOG.debug("directory_analytics:  {}", this.directory_analytics);

    /*
     * Make sure the required directories exist. There is no sane way to
     * recover if they cannot be created!
     */

    try {
      DirectoryUtilities.directoryCreate(this.directory_base);
      DirectoryUtilities.directoryCreate(this.directory_downloads);
      DirectoryUtilities.directoryCreate(this.directory_documents);
      DirectoryUtilities.directoryCreate(this.directory_profiles);
      DirectoryUtilities.directoryCreate(this.directory_analytics);
    } catch (final IOException e) {
      LOG.error("could not create directories: {}", e.getMessage(), e);
      throw new IllegalStateException(e);
    }

    LOG.debug("initializing downloader");
    this.http = HTTP.newHTTP();
    this.downloader =
      DownloaderHTTP.newDownloader(this.exec_downloader, this.directory_downloads, this.http);

    LOG.debug("initializing book registry");
    this.book_registry = BookRegistry.create();

    LOG.debug("initializing optional branding services");
    this.branding_theme_override = loadOptionalBrandingThemeOverride();

    LOG.debug("initializing cover generator");
    final TenPrintGeneratorType ten_print = TenPrintGenerator.newGenerator();
    this.cover_generator = new BookCoverGenerator(ten_print);
    this.cover_badges =
      CatalogCoverBadgeImages.Companion.create(
        resources,
        ThemeControl.resolveColorAttribute(this.getTheme(), R.attr.colorPrimary),
        this.screen);

    this.cover_provider =
      BookCoverProvider.Companion.newCoverProvider(
        this,
        this.book_registry,
        this.cover_generator,
        this.cover_badges,
        this.exec_covers,
        true,
        false);

    LOG.debug("initializing local image loader");
    this.local_image_loader =
      new Picasso.Builder(this)
        .indicatorsEnabled(false)
        .loggingEnabled(false)
        .addRequestHandler(new ImageAccountIconRequestHandler())
        .build();

    LOG.debug("initializing EPUB loader and HTTP server");
    this.mime = ReaderHTTPMimeMap.newMap("application/octet-stream");
    this.httpd = ReaderHTTPServerAAsync.newServer(asset_manager, this.mime, fetchUnusedHTTPPort());
    this.epub_loader = ReaderReadiumEPUBLoader.newLoader(this, this.exec_epub);
    this.clock = Clock.get();

    LOG.debug("initializing document store");
    this.documents = createDocumentStore(
      asset_manager,
      resources,
      this.clock,
      this.http,
      this.exec_downloader,
      this.directory_documents);

    try {
      LOG.debug("initializing account providers");
      this.account_providers = createAccountProviders(resources, asset_manager);
      for (final URI id : this.account_providers.providers().keySet()) {
        LOG.debug("loaded account provider: {}", id);
      }
    } catch (final IOException e) {
      throw new IllegalStateException("Could not initialize account providers", e);
    }

    try {
      LOG.debug("initializing bundled credentials");
      this.bundled_credentials = createBundledCredentials(asset_manager);
    } catch (final FileNotFoundException e) {
      LOG.debug("could not initialize bundled credentials: ", e);
      this.bundled_credentials = AccountBundledCredentialsEmpty.getInstance();
    } catch (final IOException e) {
      LOG.debug("could not initialize bundled credentials: ", e);
      throw new IllegalStateException("could not initialize bundled credentials", e);
    }

    try {
      LOG.debug("initializing credentials store");

      final File privateDirectory =
        this.getApplicationContext().getFilesDir();
      final File credentials =
        new File(privateDirectory, "credentials.json");
      final File credentialsTemp =
        new File(privateDirectory, "credentials.json.tmp");

      LOG.debug("credentials store path: {}", credentials);
      this.account_credentials_store =
        AccountAuthenticationCredentialsStore.Companion.open(credentials, credentialsTemp);
      LOG.debug("credentials loaded: {}", this.account_credentials_store.size());
    } catch (final Exception e) {
      LOG.debug("could not initialize credentials store: ", e);
      throw new IllegalStateException("could not initialize credentials store", e);
    }

    final ObservableType<AccountEvent> account_events = Observable.create();
    final ObservableType<ProfileEvent> profile_events = Observable.create();
    final ObservableType<ReaderBookmarkEvent> reader_bookmark_events = Observable.create();

    try {
      LOG.debug("initializing profiles and accounts");
      this.profiles =
        createProfileDatabase(
          this.getApplicationContext(),
          resources,
          account_events,
          profile_events,
          this.account_providers,
          this.bundled_credentials,
          this.account_credentials_store,
          this.directory_profiles);
    } catch (final ProfileDatabaseException e) {
      throw new IllegalStateException("Could not initialize profile database", e);
    }

    try {
      LOG.debug("initializing analytics log");
      analytics_logger = AnalyticsLogger.create(this.directory_analytics);
    } catch (Exception e) {
      LOG.debug("Ignoring exception: AnalyticsLogger.create raised: ", e);
    }

    try {
      final PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
      analytics_logger.logToAnalytics("app_open,"
        + packageInfo.packageName + ","
        + packageInfo.versionName + ","
        + Integer.toString(packageInfo.versionCode));
    } catch (PackageManager.NameNotFoundException e) {
      LOG.warn("Could not get package info for analytics");
    }

    LOG.debug("initializing bundled content");
    this.bundled_content_resolver = BundledContentResolver.create(this.getAssets());

    LOG.debug("initializing feed loader");
    this.feed_parser = createFeedParser();
    this.feed_search_parser = OPDSSearchParser.newParser();
    this.feed_transport = FeedHTTPTransport.newTransport(this.http);
    this.feed_loader =
      FeedLoader.Companion.create(
        this.exec_catalog_feeds,
        this.feed_parser,
        this.feed_search_parser,
        this.feed_transport,
        this.book_registry,
        this.bundled_content_resolver);

    LOG.debug("initializing book controller");
    this.book_controller =
      Controller.Companion.create(
        this.exec_books,
        account_events,
        profile_events,
        reader_bookmark_events,
        this.http,
        this.feed_parser,
        this.feed_loader,
        this.downloader,
        this.profiles,
        this.analytics_logger,
        this.book_registry,
        this.bundled_content_resolver,
        ignored -> this.account_providers,
        this.exec_profile_timer,
        null);

    LOG.debug("initializing reader bookmark service");
    this.readerBookmarksService =
      ReaderBookmarkService.Companion.createService(
        new ReaderBookmarkServiceProviderType.Requirements(
          (runnable) -> createNamedThreadFactory("reader-bookmarks", 19, Executors.defaultThreadFactory()).newThread(runnable),
          reader_bookmark_events,
          new ReaderBookmarkHTTPCalls(new ObjectMapper(), this.http),
          this.book_controller
        ));

    /*
     * Log out the current profile after ten minutes, warning one minute before this happens.
     */

    this.book_controller.profileIdleTimer().setWarningIdleSecondsRemaining(60);
    this.book_controller.profileIdleTimer().setMaximumIdleSeconds(10 * 60);

    LOG.debug("initializing network connectivity checker");
    this.network_connectivity = new NetworkConnectivity(this);

    LOG.debug("initializing CardCreator");
    this.cardcreator =
      new CardCreator(
        asset_manager,
        resources.getString(R.string.feature_environment),
        resources);

    LOG.debug("initializing HelpStack");
    this.helpstack = Helpstack.get(this, asset_manager);

    LOG.debug("initializing analytics");
    this.analytics = Analytics.Companion.create(this);

    LOG.debug("finished booting");
    Simplified.INSTANCE = this;
  }

  private OptionType<ThemeValue> loadOptionalBrandingThemeOverride() {
    final Iterator<BrandingThemeOverrideServiceType> iter =
      ServiceLoader.load(BrandingThemeOverrideServiceType.class)
        .iterator();

    if (iter.hasNext()) {
      final BrandingThemeOverrideServiceType service = iter.next();
      return Option.some(service.overrideTheme());
    }

    return Option.none();
  }

  private AccountBundledCredentialsType createBundledCredentials(
    final AssetManager asset_manager) throws IOException {

    try (final InputStream stream = asset_manager.open("account_bundled_credentials.json")) {
      return AccountBundledCredentialsJSON.deserializeFromStream(new ObjectMapper(), stream);
    }
  }

  private static final class NetworkConnectivity implements NetworkConnectivityType {

    private final Context context;

    NetworkConnectivity(
      final Context in_context) {
      this.context = NullCheck.notNull(in_context, "Context");
    }

    @Override
    public boolean isNetworkAvailable() {
      final ConnectivityManager service =
        (ConnectivityManager) this.context.getSystemService(Context.CONNECTIVITY_SERVICE);
      final NetworkInfo info =
        service.getActiveNetworkInfo();

      if (info == null) {
        return false;
      }

      return info.isConnectedOrConnecting();
    }

    @Override
    public boolean isWifiAvailable() {
      final ConnectivityManager service =
        (ConnectivityManager) this.context.getSystemService(Context.CONNECTIVITY_SERVICE);
      final NetworkInfo info =
        service.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

      if (info == null) {
        return false;
      }

      return info.isConnectedOrConnecting();
    }
  }

  private static final class ScreenSizeInformation implements ScreenSizeInformationType {

    private final Resources resources;

    private ScreenSizeInformation(
      final Logger log,
      final Resources rr) {
      this.resources = NullCheck.notNull(rr);

      final DisplayMetrics dm = this.resources.getDisplayMetrics();
      final float dp_height = (float) dm.heightPixels / dm.density;
      final float dp_width = (float) dm.widthPixels / dm.density;
      log.debug("screen ({} x {})", dp_width, dp_height);
      log.debug("screen ({} x {})", dm.widthPixels, dm.heightPixels);
    }

    @Override
    public double screenDPToPixels(
      final int dp) {
      final float scale = this.resources.getDisplayMetrics().density;
      return ((double) (dp * scale) + 0.5);
    }

    @Override
    public double screenGetDPI() {
      final DisplayMetrics metrics = this.resources.getDisplayMetrics();
      return (double) metrics.densityDpi;
    }

    @Override
    public int screenGetHeightPixels() {
      final Resources rr = NullCheck.notNull(this.resources);
      final DisplayMetrics dm = rr.getDisplayMetrics();
      return dm.heightPixels;
    }

    @Override
    public int screenGetWidthPixels() {
      final Resources rr = NullCheck.notNull(this.resources);
      final DisplayMetrics dm = rr.getDisplayMetrics();
      return dm.widthPixels;
    }
  }
}
