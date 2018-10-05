package org.nypl.simplified.app;

import android.app.Application;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.os.Process;
import android.support.multidex.MultiDexApplication;
import android.util.DisplayMetrics;

import com.bugsnag.android.Severity;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.io7m.jfunctional.FunctionType;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

import org.nypl.drm.core.AdobeAdeptExecutorType;
import org.nypl.simplified.app.catalog.CatalogBookCoverGenerator;
import org.nypl.simplified.app.reader.ReaderBookmarksSharedPrefs;
import org.nypl.simplified.app.reader.ReaderBookmarksSharedPrefsType;
import org.nypl.simplified.app.reader.ReaderHTTPMimeMap;
import org.nypl.simplified.app.reader.ReaderHTTPMimeMapType;
import org.nypl.simplified.app.reader.ReaderHTTPServerAAsync;
import org.nypl.simplified.app.reader.ReaderHTTPServerType;
import org.nypl.simplified.app.reader.ReaderReadiumEPUBLoader;
import org.nypl.simplified.app.reader.ReaderReadiumEPUBLoaderType;
import org.nypl.simplified.app.reader.ReaderSettings;
import org.nypl.simplified.app.reader.ReaderSettingsType;
import org.nypl.simplified.assertions.Assertions;
import org.nypl.simplified.books.core.AccountDataLoadListenerType;
import org.nypl.simplified.books.core.AccountSyncListenerType;
import org.nypl.simplified.books.core.AccountsDatabase;
import org.nypl.simplified.books.core.AccountsDatabaseType;
import org.nypl.simplified.books.core.AuthenticationDocumentValuesType;
import org.nypl.simplified.books.core.BookDatabase;
import org.nypl.simplified.books.core.BookDatabaseEntrySnapshot;
import org.nypl.simplified.books.core.BookDatabaseReadableType;
import org.nypl.simplified.books.core.BookDatabaseType;
import org.nypl.simplified.books.core.BookFormats;
import org.nypl.simplified.books.core.BookID;
import org.nypl.simplified.books.core.BooksController;
import org.nypl.simplified.books.core.BooksControllerConfigurationType;
import org.nypl.simplified.books.core.BooksType;
import org.nypl.simplified.books.core.Clock;
import org.nypl.simplified.books.core.ClockType;
import org.nypl.simplified.books.core.DeviceActivationListenerType;
import org.nypl.simplified.books.core.DocumentStore;
import org.nypl.simplified.books.core.DocumentStoreBuilderType;
import org.nypl.simplified.books.core.DocumentStoreType;
import org.nypl.simplified.books.core.FeedHTTPTransport;
import org.nypl.simplified.books.core.FeedLoader;
import org.nypl.simplified.books.core.FeedLoaderType;
import org.nypl.simplified.books.core.LogUtilities;
import org.nypl.simplified.bugsnag.IfBugsnag;
import org.nypl.simplified.cardcreator.CardCreator;
import org.nypl.simplified.downloader.core.DownloaderHTTP;
import org.nypl.simplified.downloader.core.DownloaderType;
import org.nypl.simplified.files.DirectoryUtilities;
import org.nypl.simplified.http.core.HTTP;
import org.nypl.simplified.http.core.HTTPAuthType;
import org.nypl.simplified.http.core.HTTPType;
import org.nypl.simplified.multilibrary.Account;
import org.nypl.simplified.multilibrary.AccountsRegistry;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParser;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParserType;
import org.nypl.simplified.opds.core.OPDSAuthenticationDocumentParser;
import org.nypl.simplified.opds.core.OPDSAuthenticationDocumentParserType;
import org.nypl.simplified.opds.core.OPDSFeedParser;
import org.nypl.simplified.opds.core.OPDSFeedParserType;
import org.nypl.simplified.opds.core.OPDSFeedTransportType;
import org.nypl.simplified.opds.core.OPDSJSONParser;
import org.nypl.simplified.opds.core.OPDSJSONParserType;
import org.nypl.simplified.opds.core.OPDSJSONSerializer;
import org.nypl.simplified.opds.core.OPDSJSONSerializerType;
import org.nypl.simplified.opds.core.OPDSSearchParser;
import org.nypl.simplified.opds.core.OPDSSearchParserType;
import org.nypl.simplified.prefs.Prefs;
import org.nypl.simplified.tenprint.TenPrintGenerator;
import org.nypl.simplified.tenprint.TenPrintGeneratorType;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Global application state.
 */

public final class Simplified extends MultiDexApplication
{
  private static final              Logger     LOG;
  private static volatile @Nullable Simplified INSTANCE;

  static {
    LOG = LogUtilities.getLog(Simplified.class);
  }

  private @Nullable CatalogAppServices app_services;
  private @Nullable ReaderAppServices  reader_services;
  private @Nullable CardCreator cardcreator;

  /**
   * Protect the singleton from getting constructed by outside sources.
   */

  public Simplified()
  {

  }

  /**
   * Checks if Simplified singleton has been initialized already.
   *
   * @return the one singleton instance of Simplified
   * @throws IllegalStateException if Simplified is not yet initialized
   */
  private static Simplified checkInitialized()
  {
    final Simplified i = Simplified.INSTANCE;
    if (i == null) {
      throw new IllegalStateException("Application is not yet initialized");
    }
    return i;
  }

  /**
   * @return cardcreator
   */
  public static CardCreator getCardCreator()
  {
    final Simplified i = Simplified.checkInitialized();
    return i.getActualCardCreator();
  }


  /**
   * @return The application services provided to the Catalog.
   */

  public static SimplifiedCatalogAppServicesType getCatalogAppServices()
  {
    final Simplified i = Simplified.checkInitialized();

    return i.getActualAppServices(Simplified.getCurrentAccount().getPathComponent(), false);
  }

  /**
   * @return Shared Preferences
   */
  public static Prefs getSharedPrefs() {
    final Simplified i = Simplified.checkInitialized();
    return new Prefs(i.getApplicationContext());
  }

  /**
   * @return Current Library Account
   */
  public static Account getCurrentAccount() {
    final Simplified i = Simplified.checkInitialized();
    Account account = new AccountsRegistry(i.getApplicationContext()).getAccount(Simplified.getSharedPrefs().getInt("current_account"));
    if (account == null)
    {
      account = new AccountsRegistry(i.getApplicationContext()).getAccount(0);
    }
    return account;
  }

  /**
   * @param account  Library Account
   * @param context Context
   * @return Accounts Database
   */
  public static AccountsDatabaseType getAccountsDatabase(final Account account, final Context context)
  {

    File base_accounts_dir = context.getFilesDir();
    if (account.getId() > 0)
    {
      base_accounts_dir =
        new File(context.getFilesDir(), account.getPathComponent());
    }
    final File accounts_dir = new File(base_accounts_dir, "accounts");

    final File base_dir = Simplified.getDiskDataDir(context);
    File base_library_dir = base_dir;
    if (account.getId() > 0)
    {
      base_library_dir = new File(base_dir, account.getPathComponent());
    }

    final File downloads_dir = new File(base_library_dir, "downloads");
    final File books_dir = new File(base_library_dir, "books");

    try {
      DirectoryUtilities.directoryCreate(accounts_dir);
      DirectoryUtilities.directoryCreate(downloads_dir);
      DirectoryUtilities.directoryCreate(books_dir);
    } catch (final IOException e) {
      Simplified.LOG.error(
        "could not create directories: {}", e.getMessage(), e);
      throw new IllegalStateException(e);
    }

    AccountsDatabase.openDatabase(accounts_dir);

    return AccountsDatabase.openDatabase(accounts_dir);
  }

  /**
   * @return Current Theme
   */
  public static Resources.Theme getCurrentTheme() {
    final Simplified i = Simplified.checkInitialized();

    return i.getApplicationContext().getTheme();
  }

  static File getDiskDataDir(
    final Context context)
  {
    /**
     * If external storage is mounted and is on a device that doesn't allow
     * the storage to be removed, use the external storage for data.
     */

    if (Environment.MEDIA_MOUNTED.equals(
      Environment.getExternalStorageState())) {

      Simplified.LOG.debug("trying external storage");
      if (!Environment.isExternalStorageRemovable()) {
        final File r = context.getExternalFilesDir(null);
        Simplified.LOG.debug(
          "external storage is not removable, using it ({})", r);
        Assertions.checkPrecondition(
          r.isDirectory(), "Data directory {} is a directory", r);
        return NullCheck.notNull(r);
      }
    }

    /**
     * Otherwise, use internal storage.
     */

    final File r = context.getFilesDir();
    Simplified.LOG.debug(
      "no non-removable external storage, using internal storage ({})", r);
    Assertions.checkPrecondition(
      r.isDirectory(), "Data directory {} is a directory", r);
    return NullCheck.notNull(r);
  }

  /**
   * @return The application services provided to the Reader.
   */

  public static SimplifiedReaderAppServicesType getReaderAppServices()
  {
    final Simplified i = Simplified.checkInitialized();
    return i.getActualReaderAppServices(Simplified.getCurrentAccount().getPathComponent());
  }

  private static FeedLoaderType makeFeedLoader(
    final ListeningExecutorService exec,
    final BookDatabaseReadableType db,
    final HTTPType http,
    final OPDSSearchParserType s,
    final OPDSFeedParserType p)
  {
    final OPDSFeedTransportType<OptionType<HTTPAuthType>> t =
      FeedHTTPTransport.newTransport(http);
    return FeedLoader.newFeedLoader(exec, db, p, t, s);
  }

  private static ExecutorService namedThreadPool(
    final int count,
    final String base,
    final int priority)
  {
    final ThreadFactory tf = Executors.defaultThreadFactory();
    final ThreadFactory named = new ThreadFactory()
    {
      private int id;

      @Override public Thread newThread(
        final @Nullable Runnable r)
      {
        /**
         * Apparently, it's necessary to use {@link android.os.Process} to set
         * the thread priority, rather than the standard Java thread
         * functions.
         */

        final Thread t = tf.newThread(
          new Runnable()
          {
            @Override public void run()
            {
              android.os.Process.setThreadPriority(priority);
              NullCheck.notNull(r).run();
            }
          });
        t.setName(
          String.format(
            "simplified-%s-tasks-%d", base, this.id));
        ++this.id;
        return t;
      }
    };

    final ExecutorService pool = Executors.newFixedThreadPool(count, named);
    return NullCheck.notNull(pool);
  }

  /**
   * @param account Library Account
   * @param rr Resources
   * @return Document Store
   */
  public static DocumentStoreType getDocumentStore(final Account account, final Resources rr) {

    final Simplified i = Simplified.checkInitialized();
    final ClockType clock = Clock.get();
    final OPDSAuthenticationDocumentParserType auth_doc_parser =
      OPDSAuthenticationDocumentParser.get();

    /**
     * Default authentication document values.
     */

    final AuthenticationDocumentValuesType auth_doc_values =
      new AuthenticationDocumentValuesType()
      {
        @Override public String getLabelLoginUserID()
        {
          return rr.getString(R.string.settings_barcode);
        }

        @Override public String getLabelLoginPassword()
        {
          return rr.getString(R.string.settings_pin);
        }
        @Override public String getLabelLoginPatronName()
        {
          return rr.getString(R.string.settings_name);
        }
      };

    final File base_dir = Simplified.getDiskDataDir(i.getApplicationContext());

    File base_library_dir = base_dir;
    if (account.getId() > 0)
    {
      base_library_dir = new File(base_dir, account.getPathComponent());
    }

    final File books_dir = new File(base_library_dir, "books");


    final DocumentStoreBuilderType documents_builder =
      DocumentStore.newBuilder(
        clock,
        HTTP.newHTTP(),
        Simplified.namedThreadPool(1, "books", 19),
        books_dir,
        auth_doc_values,
        auth_doc_parser);

    /**
     * Conditionally enable each of the documents based on the
     * presence of assets.
     */

    final AssetManager assets = i.getApplicationContext().getAssets();

    {
      try {
        final InputStream stream = assets.open("eula.html");
        documents_builder.enableEULA(
          new FunctionType<Unit, InputStream>()
          {
            @Override public InputStream call(final Unit x)
            {
              return stream;
            }
          });
      } catch (final IOException e) {
        Simplified.LOG.debug("No EULA defined: ", e);
      }

    }

    return documents_builder.build();
  }

  /**
   * @param account Library Account
   * @param context Context
   * @param in_adobe_drm Adobe Adept Excecutor
   * @return Books
   */
  public static BooksType getBooks(final Account account, final Context context, final OptionType<AdobeAdeptExecutorType> in_adobe_drm) {

    final ListeningExecutorService exec_books =
      MoreExecutors.listeningDecorator(Simplified.namedThreadPool(1, "books", 19));
    final HTTPType http = HTTP.newHTTP();

    File base_accounts_dir = context.getFilesDir();
    if (account.getId() > 0)
    {
      base_accounts_dir =
        new File(context.getFilesDir(), account.getPathComponent());
    }

    final File accounts_dir = new File(base_accounts_dir, "accounts");


    final File base_dir = Simplified.getDiskDataDir(context);
    File base_library_dir = base_dir;
    if (account.getId() > 0)
    {
      base_library_dir = new File(base_dir, account.getPathComponent());
    }

    final File downloads_dir = new File(base_library_dir, "downloads");
    final File books_dir = new File(base_library_dir, "books");
    final File books_database_directory = new File(books_dir, "data");

    final DownloaderType downloader = DownloaderHTTP.newDownloader(
      exec_books, downloads_dir, http);

    final ListeningExecutorService exec_catalog_feeds =
      MoreExecutors.listeningDecorator(Simplified.namedThreadPool(1, "catalog-feed", 19));

    final OPDSJSONSerializerType in_json_serializer =
      OPDSJSONSerializer.newSerializer();
    final OPDSJSONParserType in_json_parser = OPDSJSONParser.newParser();

    final BookDatabaseType books_database = BookDatabase.Companion.newDatabase(
      in_json_serializer, in_json_parser, books_database_directory);

    final OPDSAcquisitionFeedEntryParserType in_entry_parser =
      OPDSAcquisitionFeedEntryParser.newParser(BookFormats.Companion.supportedBookMimeTypes());

    final OPDSFeedParserType p = OPDSFeedParser.newParser(in_entry_parser);
    final OPDSSearchParserType s = OPDSSearchParser.newParser();

    final FeedLoaderType  feed_loader =
      Simplified.makeFeedLoader(exec_catalog_feeds, books_database, http, s, p);


    final AccountsDatabaseType accounts_database = AccountsDatabase.openDatabase(accounts_dir);

    String catalog = account.getCatalogUrl();
    String adobe = account.getCatalogUrl();

    if (!account.needsAuth() && account.getId() == 2 && Simplified.getSharedPrefs().contains("age13"))
    {
      if (Simplified.getSharedPrefs().getBoolean("age13")) {
        catalog = account.getCatalogUrl13AndOver();
        adobe = account.getCatalogUrl13AndOver();
      }
      else {
        catalog = account.getCatalogUrlUnder13();
        adobe = account.getCatalogUrlUnder13();
      }
    }

    final BooksControllerConfiguration books_config =
      new BooksControllerConfiguration(
        URI.create(catalog),
        URI.create(adobe));

    final URI loans_url_component = books_config.getCurrentRootFeedURI().resolve(context.getResources().getString(R.string.feature_catalog_loans_uri_component));

    return  BooksController.newBooks(
      context,
      exec_books,
      feed_loader,
      http,
      downloader,
      in_json_serializer,
      in_json_parser,
      in_adobe_drm,
      Simplified.getDocumentStore(account, context.getResources()),
      books_database,
      accounts_database,
      books_config,
      loans_url_component);

  }

  private synchronized CardCreator getActualCardCreator()
  {
    CardCreator as = this.cardcreator;
    if (as != null) {
      return as;
    }
    as = new CardCreator(
      this.getAssets(), this.getResources().getString(R.string.feature_environment), this.getResources());
    this.cardcreator = as;
    return as;
  }


  private synchronized SimplifiedCatalogAppServicesType getActualAppServices(final String in_library, final boolean reload)
  {
    CatalogAppServices as = this.app_services;
    if (as == null)
    {
      as = new CatalogAppServices(
        null, this, this, NullCheck.notNull(this.getResources()), in_library);
      this.app_services = as;
      return as;
    }
    else if (this.app_services.library.equals(in_library) && !reload) {
      return as;
    }
    else
    {
      as = new CatalogAppServices(this.app_services.adobe_drm,
        this, this, NullCheck.notNull(this.getResources()), in_library);
      this.app_services = as;
      return as;
    }
  }

  private SimplifiedReaderAppServicesType getActualReaderAppServices(final String in_library)
  {
    ReaderAppServices as = this.reader_services;
    if (as == null)
    {
      as = new ReaderAppServices(null, this, NullCheck.notNull(this.getResources()));
      this.reader_services = as;
      return as;
    }
    else if (this.app_services.library.equals(in_library))
    {
      return as;
    }
    else {
      as = new ReaderAppServices(this.reader_services.epub_exec, this, NullCheck.notNull(this.getResources()));
      this.reader_services = as;
      return as;
    }
  }

  private void initBugsnag(final OptionType<String> api_token_opt)
  {
    if (api_token_opt.isSome()) {
      final String api_token = ((Some<String>) api_token_opt).get();
      Simplified.LOG.debug("IfBugsnag: init live interface");
      IfBugsnag.init(this, api_token);
      reportNewSession();
    } else {
      Simplified.LOG.debug("IfBugsnag: init no-op interface");
      IfBugsnag.init();
    }
  }

  private void reportNewSession() {
    final String libraryName = getCurrentAccount().getName();
    final int libraryID = getCurrentAccount().getId();
    Exception e = new Exception("app_launch");
    com.bugsnag.android.Bugsnag.notify(e, report -> {
      if (report.getError() != null) {
        report.getError().getMetaData().addToTab("Library Info", "LibraryID", libraryID);
        report.getError().getMetaData().addToTab("Library Info", "LibraryName", libraryName);
        report.getError().setSeverity(Severity.INFO);
      }
    });
  }

  @Override
  public void onCreate() {
    super.onCreate();

    Simplified.LOG.debug(
        "starting app: pid {}", Process.myPid());
    Simplified.INSTANCE = this;

    this.initBugsnag(Bugsnag.getApiToken(this.getAssets()));

    this.cardcreator = new CardCreator(this.getAssets(), this.getResources().getString(R.string.feature_environment), this.getResources());

  }

  private static final class CatalogAppServices implements
    SimplifiedCatalogAppServicesType,
    AccountDataLoadListenerType,
    AccountSyncListenerType,
    DeviceActivationListenerType
  {
    private static final Logger LOG_CA;

    static {
      LOG_CA = LogUtilities.getLog(CatalogAppServices.class);
    }

    private final BooksType                          books;
    private final Context                            context;
    private final CatalogBookCoverGenerator          cover_generator;
    private final BookCoverProviderType              cover_provider;
    private final ListeningExecutorService           exec_books;
    private final ListeningExecutorService           exec_catalog_feeds;
    private final ExecutorService                    exec_covers;
    private final ExecutorService                    exec_downloader;
    private URI                                      feed_initial_uri;
    private final FeedLoaderType                     feed_loader;
    private final HTTPType                           http;
    private final ScreenSizeControllerType           screen;
    private final AtomicBoolean                      synced;
    private final DownloaderType                     downloader;
    private OptionType<AdobeAdeptExecutorType>       adobe_drm;
    private final DocumentStoreType                  documents;
    private final OptionType<HelpstackType>          helpstack;
    private final BookDatabaseType                   books_database;
    private final AccountsDatabaseType               accounts_database;


    private final String library;

    private CatalogAppServices(
      final OptionType<AdobeAdeptExecutorType> in_adobe_drm,
      final Application in_app,
      final Context in_context,
      final Resources rr,
      final String in_library)
    {
      NullCheck.notNull(rr);

      this.library = in_library;
      this.adobe_drm = in_adobe_drm;
      this.context = NullCheck.notNull(in_context);
      this.screen = new ScreenSizeController(rr);
      this.exec_catalog_feeds =
        MoreExecutors.listeningDecorator(Simplified.namedThreadPool(1, "catalog-feed", 19));
      this.exec_covers = Simplified.namedThreadPool(2, "cover", 19);
      this.exec_downloader = Simplified.namedThreadPool(4, "downloader", 19);
      this.exec_books =
        MoreExecutors.listeningDecorator(Simplified.namedThreadPool(1, "books", 19));


      final Prefs prefs = getSharedPrefs();

      final Account account = new AccountsRegistry(this.context).getAccount(prefs.getInt("current_account"));

      /**
       * Application paths.
       */

      File base_accounts_dir = this.context.getFilesDir();
      if (account.getId() > 0)
      {
        base_accounts_dir =
          new File(this.context.getFilesDir(), account.getPathComponent());
      }


      final File accounts_dir = new File(base_accounts_dir, "accounts");

      final File base_dir = Simplified.getDiskDataDir(in_context);
      File base_library_dir = base_dir;
      if (account.getId() > 0)
      {
        base_library_dir = new File(base_dir, account.getPathComponent());
      }

      final File downloads_dir = new File(base_library_dir, "downloads");
      final File books_dir = new File(base_library_dir, "books");
      final File books_database_directory = new File(books_dir, "data");

      /**
       * Make sure the required directories exist. There is no sane way to
       * recover if they cannot be created!
       */

      try {
        DirectoryUtilities.directoryCreate(accounts_dir);
        DirectoryUtilities.directoryCreate(downloads_dir);
        DirectoryUtilities.directoryCreate(books_dir);
      } catch (final IOException e) {
        Simplified.LOG.error(
          "could not create directories: {}", e.getMessage(), e);
        throw new IllegalStateException(e);
      }

      CatalogAppServices.LOG_CA.debug("base:                {}", base_dir);
      CatalogAppServices.LOG_CA.debug("base_accounts_dir:   {}", base_accounts_dir);
      CatalogAppServices.LOG_CA.debug("base_library_dir:    {}", base_library_dir);
      CatalogAppServices.LOG_CA.debug("accounts:            {}", accounts_dir);
      CatalogAppServices.LOG_CA.debug("downloads:           {}", downloads_dir);
      CatalogAppServices.LOG_CA.debug("books:               {}", books_dir);

      /**
       * Catalog URIs.
       */

      String catalog = account.getCatalogUrl();
      String adobe = account.getCatalogUrl();

      if (!account.needsAuth() && account.getId() == 2 && Simplified.getSharedPrefs().contains("age13"))
      {
        if (Simplified.getSharedPrefs().getBoolean("age13")) {
          catalog = account.getCatalogUrl13AndOver();
          adobe = account.getCatalogUrl13AndOver();
        }
        else {
          catalog = account.getCatalogUrlUnder13();
          adobe = account.getCatalogUrlUnder13();
        }
      }


      CatalogAppServices.LOG_CA.debug("catalog:     {}", catalog);
      CatalogAppServices.LOG_CA.debug("this.library:     {}", account.getName());

      final BooksControllerConfiguration books_config =
        new BooksControllerConfiguration(
          URI.create(catalog),
          URI.create(adobe));

      this.feed_initial_uri = books_config.getCurrentRootFeedURI();

      /**
       * Feed loaders and parsers.
       */

      this.http = HTTP.newHTTP();
      final OPDSAcquisitionFeedEntryParserType in_entry_parser =
        OPDSAcquisitionFeedEntryParser.newParser(BookFormats.Companion.supportedBookMimeTypes());
      final OPDSJSONSerializerType in_json_serializer =
        OPDSJSONSerializer.newSerializer();
      final OPDSJSONParserType in_json_parser = OPDSJSONParser.newParser();
      final OPDSFeedParserType p = OPDSFeedParser.newParser(in_entry_parser);
      final OPDSSearchParserType s = OPDSSearchParser.newParser();

      this.books_database = BookDatabase.Companion.newDatabase(
        in_json_serializer, in_json_parser, books_database_directory);
      this.accounts_database = AccountsDatabase.openDatabase(accounts_dir);

      this.feed_loader = Simplified.makeFeedLoader(
        this.exec_catalog_feeds, this.books_database, this.http, s, p);

      /**
       * DRM.
       */
      if (this.adobe_drm == null) {
        this.adobe_drm = AdobeDRMServices.newAdobeDRMOptional(
          this.context, AdobeDRMServices.getPackageOverride(rr));
      }
      this.downloader = DownloaderHTTP.newDownloader(
        this.exec_books, downloads_dir, this.http);


      /**
       * Configure EULA, privacy policy, etc.
       */

      final ClockType clock = Clock.get();
      final OPDSAuthenticationDocumentParserType auth_doc_parser =
        OPDSAuthenticationDocumentParser.get();

      /**
       * Default authentication document values.
       */

      final AuthenticationDocumentValuesType auth_doc_values =
        new AuthenticationDocumentValuesType()
        {
          @Override public String getLabelLoginUserID()
          {
            return rr.getString(R.string.settings_barcode);
          }

          @Override public String getLabelLoginPassword()
          {
            return rr.getString(R.string.settings_pin);
          }
          @Override public String getLabelLoginPatronName()
          {
            return rr.getString(R.string.settings_name);
          }
        };



      final DocumentStoreBuilderType documents_builder =
        DocumentStore.newBuilder(
          clock,
          this.http,
          this.exec_books,
          books_dir,
          auth_doc_values,
          auth_doc_parser);

      /**
       * Conditionally enable each of the documents based on the
       * presence of assets.
       */

      final AssetManager assets = this.context.getAssets();

      {
        try {
          final InputStream stream = assets.open("eula.html");
          documents_builder.enableEULA(
            new FunctionType<Unit, InputStream>()
            {
              @Override public InputStream call(final Unit x)
              {
                return stream;
              }
            });
        } catch (final IOException e) {
          Simplified.LOG.debug("No EULA defined: ", e);
        }


        try {
          final InputStream stream = assets.open("software-licenses.html");
          documents_builder.enableLicenses(
            new FunctionType<Unit, InputStream>()
            {
              @Override public InputStream call(final Unit x)
              {
                return stream;
              }
            });
        } catch (final IOException e) {
          Simplified.LOG.debug("No licenses defined: ", e);
        }

      }

      this.documents = documents_builder.build();

      /**
       * Make an attempt to fetch the login form as soon as the application
       * starts, ignoring any failures.
       */

      this.exec_downloader.submit(
        new Runnable()
        {
          @Override public void run()
          {
            try {
              DocumentStore.fetchLoginForm(
                CatalogAppServices.this.documents,
                CatalogAppServices.this.http,
                CatalogAppServices.this.feed_initial_uri);
            } catch (final Throwable x) {
              Simplified.LOG.error("could not fetch login form: ", x);
            }
          }
        });

      /**
       * The main book controller.
       */

      final URI loans_url_component = books_config.getCurrentRootFeedURI().resolve(rr.getString(R.string.feature_catalog_loans_uri_component));

      this.books = BooksController.newBooks(
        in_context,
        this.exec_books,
        this.feed_loader,
        this.http,
        this.downloader,
        in_json_serializer,
        in_json_parser,
        this.adobe_drm,
        this.documents,
        this.books_database,
        this.accounts_database,
        books_config,
        loans_url_component);

      /**
       * Configure cover provider.
       */

      final TenPrintGeneratorType ten_print = TenPrintGenerator.newGenerator();
      this.cover_generator = new CatalogBookCoverGenerator(ten_print);
      this.cover_provider = BookCoverProvider.newCoverProvider(
        in_context,
        this.books.bookGetDatabase(),
        this.cover_generator,
        this.exec_covers);

      /**
       * Has the initial sync operation been carried out?
       */

      this.synced = new AtomicBoolean(false);

      /**
       * HelpStack.
       */

      this.helpstack = Helpstack.get(in_app, in_context.getAssets());
    }

    @Override public DocumentStoreType getDocumentStore()
    {
      return this.documents;
    }

    @Override public BooksType getBooks()
    {
      return this.books;
    }

    @Override public BookCoverProviderType getCoverProvider()
    {
      return this.cover_provider;
    }

    @Override
    public HTTPType getHTTP() {
      return this.http;
    }

    @Override public FeedLoaderType getFeedLoader()
    {
      return this.feed_loader;
    }

    @Override public OptionType<AdobeAdeptExecutorType> getAdobeDRMExecutor()
    {
      return this.adobe_drm;
    }

    @Override public OptionType<HelpstackType> getHelpStack()
    {
      return this.helpstack;
    }

    @Override
    public void reloadCatalog(final boolean delete_books, final Account account) {
      final Simplified i = Simplified.checkInitialized();
      i.getActualAppServices(Simplified.getCurrentAccount().getPathComponent(), true);

      if (delete_books) {
        try {

          if (account.getId() == getCurrentAccount().getId()) {
            this.books_database.databaseDestroy();
            this.getBooks().destroyBookStatusCache();

          }
          else {
            // set database to destroy the next time switched to that account.
            getSharedPrefs().putInt("destroy_database", account.getId());
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    @Override
    public void destroyDatabase() {
      try {
        this.books_database.databaseDestroy();
        this.getBooks().destroyBookStatusCache();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    @Override public boolean isNetworkAvailable()
    {
      final NetworkInfo info =
        ((ConnectivityManager) this.context.getSystemService(
          Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();

      if (info == null) {
        return false;
      }

      return info.isConnected();
    }

    @Override public void onAccountDataBookLoadFailed(
      final BookID id,
      final OptionType<Throwable> error,
      final String message)
    {
      final String s =
        NullCheck.notNull(String.format("failed to load books: %s", message));
      LogUtilities.errorWithOptionalException(
        CatalogAppServices.LOG_CA, s, error);
    }

    @Override public void onAccountDataBookLoadFinished()
    {
      CatalogAppServices.LOG_CA.debug(
        "finished loading books, syncing " + "account");
      final BooksType b = NullCheck.notNull(this.books);
      b.accountSync(this, this, Simplified.getCurrentAccount().needsAuth());
    }

    @Override public void onAccountDataBookLoadSucceeded(
      final BookID book,
      final BookDatabaseEntrySnapshot snap)
    {
      CatalogAppServices.LOG_CA.debug("loaded book: {}", book);
    }

    @Override public void onAccountDataLoadFailedImmediately(
      final Throwable error)
    {
      CatalogAppServices.LOG_CA.error("failed to load books: ", error);
    }

    @Override public void onAccountSyncAuthenticationFailure(
      final String message)
    {
      CatalogAppServices.LOG_CA.debug(
        "failed to sync account due to authentication failure: {}", message);
    }

    @Override public void onAccountSyncBook(
      final BookID book)
    {
      CatalogAppServices.LOG_CA.debug("synced book: {}", book);
    }

    @Override public void onAccountSyncFailure(
      final OptionType<Throwable> error,
      final String message)
    {
      final String s =
        NullCheck.notNull(String.format("failed to sync account: %s", message));
      LogUtilities.errorWithOptionalException(
        CatalogAppServices.LOG_CA, s, error);
    }

    @Override public void onAccountSyncSuccess()
    {
      CatalogAppServices.LOG_CA.debug("synced account");
    }

    @Override public void onAccountSyncBookDeleted(final BookID book)
    {
      CatalogAppServices.LOG_CA.debug("deleted book: {}", book);
    }

    @Override public void onAccountUnavailable()
    {
      CatalogAppServices.LOG_CA.debug("not logged in, not loading books");
    }

    @Override public double screenDPToPixels(
      final int dp)
    {
      return this.screen.screenDPToPixels(dp);
    }

    @Override public double screenGetDPI()
    {
      return this.screen.screenGetDPI();
    }

    @Override public int screenGetHeightPixels()
    {
      return this.screen.screenGetHeightPixels();
    }

    @Override public int screenGetWidthPixels()
    {
      return this.screen.screenGetWidthPixels();
    }

    @Override public void syncInitial()
    {
      if (this.synced.compareAndSet(false, true)) {
        CatalogAppServices.LOG_CA.debug("performing initial sync");
        this.books.accountLoadBooks(this, Simplified.getCurrentAccount().needsAuth());
      } else {
        CatalogAppServices.LOG_CA.debug(
          "initial sync already attempted, not syncing again");
      }
    }

    @Override
    public void onDeviceActivationFailure(final String message) {
      CatalogAppServices.LOG_CA.debug(
        "Could not activate Device");
    }

    @Override
    public void onDeviceActivationSuccess() {
      CatalogAppServices.LOG_CA.debug(
        "Device activated successful");
    }
  }

  private static final class ReaderAppServices
    implements SimplifiedReaderAppServicesType
  {
    private final ReaderBookmarksSharedPrefsType bookmarks;
    private final ExecutorService             epub_exec;
    private final ReaderReadiumEPUBLoaderType epub_loader;
    private final ReaderHTTPServerType        httpd;
    private final ReaderHTTPMimeMapType       mime;
    private final ScreenSizeControllerType    screen;
    private final ReaderSettingsType          settings;

    private ReaderAppServices(
      final ExecutorService in_epub_exec,
      final Context context,
      final Resources rr)
    {
      this.screen = new ScreenSizeController(rr);

      this.mime = ReaderHTTPMimeMap.newMap("application/octet-stream");

      // Fallback port
      Integer port = 8080;
      try {
        final ServerSocket s = new ServerSocket(0);
        port = s.getLocalPort();
        s.close();
      } catch (IOException e) {
        // Ignore
      }

      this.httpd =
        ReaderHTTPServerAAsync.newServer(context.getAssets(), this.mime, port);

      if (in_epub_exec == null) {
        this.epub_exec = Simplified.namedThreadPool(1, "epub", 19);
      }
      else {
        this.epub_exec = in_epub_exec;
      }

      this.epub_loader =
        ReaderReadiumEPUBLoader.newLoader(context, this.epub_exec);

      this.settings = ReaderSettings.openSettings(context);
      this.bookmarks = ReaderBookmarksSharedPrefs.openBookmarksSharedPrefs(context);
    }

    @Override public ReaderBookmarksSharedPrefsType getBookmarks()
    {
      return this.bookmarks;
    }

    @Override public ReaderReadiumEPUBLoaderType getEPUBLoader()
    {
      return this.epub_loader;
    }

    @Override public ReaderHTTPServerType getHTTPServer()
    {
      return this.httpd;
    }

    @Override public ReaderSettingsType getSettings()
    {
      return this.settings;
    }

    @Override public double screenDPToPixels(
      final int dp)
    {
      return this.screen.screenDPToPixels(dp);
    }

    @Override public double screenGetDPI()
    {
      return this.screen.screenGetDPI();
    }

    @Override public int screenGetHeightPixels()
    {
      return this.screen.screenGetHeightPixels();
    }

    @Override public int screenGetWidthPixels()
    {
      return this.screen.screenGetWidthPixels();
    }

  }

  private static final class ScreenSizeController
    implements ScreenSizeControllerType
  {
    private final Resources resources;

    private ScreenSizeController(
      final Resources rr)
    {
      this.resources = NullCheck.notNull(rr);

      final DisplayMetrics dm = this.resources.getDisplayMetrics();
      final float dp_height = (float) dm.heightPixels / dm.density;
      final float dp_width = (float) dm.widthPixels / dm.density;
      CatalogAppServices.LOG_CA.debug(
        "screen ({} x {})", Float.valueOf(dp_width), Float.valueOf(dp_height));
      CatalogAppServices.LOG_CA.debug(
        "screen ({} x {})",
        Integer.valueOf(dm.widthPixels),
        Integer.valueOf(dm.heightPixels));
    }

    @Override public double screenDPToPixels(
      final int dp)
    {
      final float scale = this.resources.getDisplayMetrics().density;
      return ((double) (dp * scale) + 0.5);
    }

    @Override public double screenGetDPI()
    {
      final DisplayMetrics metrics = this.resources.getDisplayMetrics();
      return (double) metrics.densityDpi;
    }

    @Override public int screenGetHeightPixels()
    {
      final Resources rr = NullCheck.notNull(this.resources);
      final DisplayMetrics dm = rr.getDisplayMetrics();
      return dm.heightPixels;
    }

    @Override public int screenGetWidthPixels()
    {
      final Resources rr = NullCheck.notNull(this.resources);
      final DisplayMetrics dm = rr.getDisplayMetrics();
      return dm.widthPixels;
    }

  }

  private final static class BooksControllerConfiguration
    implements BooksControllerConfigurationType
  {
    private URI current_root;
    private URI alternate_root;
    private URI adobe_auth_root;

    BooksControllerConfiguration(
      final URI in_root,
      final URI in_adobe_auth)
    {
      this.current_root = NullCheck.notNull(in_root);
      this.adobe_auth_root = NullCheck.notNull(in_adobe_auth);
    }

    @Override public synchronized URI getCurrentRootFeedURI()
    {
      if (this.alternate_root != null)
      {
        return this.alternate_root;
      }

      return this.current_root;
    }

    @Override public synchronized void setCurrentRootFeedURI(final URI u)
    {
      this.current_root = NullCheck.notNull(u);
    }

    @Override
    public URI getAdobeAuthURI() {
      return this.adobe_auth_root;
    }

    @Override
    public void setAdobeAuthURI(final URI u) {
      this.adobe_auth_root = u;
    }

    @Override public synchronized URI getAlternateRootFeedURI()
    {
      return this.alternate_root;
    }

    @Override public synchronized void setAlternateRootFeedURI(final URI u)
    {
      this.alternate_root = u;
    }
  }
}
