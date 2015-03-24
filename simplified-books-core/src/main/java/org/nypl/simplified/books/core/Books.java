package org.nypl.simplified.books.core;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import org.nypl.simplified.http.core.HTTPAuthBasic;
import org.nypl.simplified.http.core.HTTPAuthType;
import org.nypl.simplified.http.core.HTTPResultError;
import org.nypl.simplified.http.core.HTTPResultException;
import org.nypl.simplified.http.core.HTTPResultMatcherType;
import org.nypl.simplified.http.core.HTTPResultOKType;
import org.nypl.simplified.http.core.HTTPResultToException;
import org.nypl.simplified.http.core.HTTPResultType;
import org.nypl.simplified.http.core.HTTPType;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeed;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.nypl.simplified.opds.core.OPDSFeedParserType;
import org.nypl.simplified.opds.core.OPDSFeedType;
import org.nypl.simplified.opds.core.OPDSNavigationFeed;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.TreeTraverser;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.PartialFunctionType;
import com.io7m.jfunctional.Some;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

@SuppressWarnings("synthetic-access") public final class Books implements
  BooksType
{
  private static final class DataLoadTask implements Runnable
  {
    private final File                        base;
    private final BooksRegistryType           books;
    private final BooksConfiguration          config;
    private final AccountDataLoadListenerType listener;

    public DataLoadTask(
      final BooksRegistryType in_books,
      final AccountDataLoadListenerType in_listener,
      final BooksConfiguration in_config)
    {
      this.books = NullCheck.notNull(in_books);
      this.config = NullCheck.notNull(in_config);
      this.listener = NullCheck.notNull(in_listener);
      this.base = new File(this.config.getDirectory(), "data");
    }

    @Override public void run()
    {
      if (this.base.isDirectory() == false) {
        try {
          this.listener.onAccountUnavailable();
        } catch (final Throwable x) {
          // Ignore
        }
        return;
      }

      final File[] book_list = this.base.listFiles(new FileFilter() {
        @Override public boolean accept(
          final @Nullable File path)
        {
          assert path != null;
          return path.isDirectory();
        }
      });

      for (final File f : book_list) {
        final BookID id = BookID.fromString(NullCheck.notNull(f.getName()));
        try {
          final Book b = Book.loadFromDirectory(f);
          this.books.bookUpdate(b);
          this.listener.onAccountDataBookLoadSucceeded(b);
        } catch (final Throwable e) {
          this.listener.onAccountDataBookLoadFailed(
            id,
            Option.some(e),
            e.getMessage());
        }
      }
    }
  }

  private static final class DataSetupTask implements Runnable
  {
    private final AccountBarcode               barcode;
    private final File                         base;
    private final BooksConfiguration           config;
    private final File                         file_barcode;
    private final File                         file_barcode_tmp;
    private final AccountDataSetupListenerType listener;

    public DataSetupTask(
      final BooksConfiguration in_config,
      final AccountBarcode in_barcode,
      final AccountDataSetupListenerType in_listener)
    {
      this.config = NullCheck.notNull(in_config);
      this.barcode = NullCheck.notNull(in_barcode);
      this.listener = NullCheck.notNull(in_listener);

      this.base = new File(this.config.getDirectory(), "data");
      this.file_barcode = new File(this.base, "barcode.txt");
      this.file_barcode_tmp = new File(this.base, "barcode.txt.tmp");
    }

    @Override public void run()
    {
      try {
        FileUtilities.createDirectory(this.base);
        this.barcode.writeToFile(this.file_barcode, this.file_barcode_tmp);
        this.listener.onAccountDataSetupSuccess();
      } catch (final Throwable x) {
        this.listener.onAccountDataSetupFailure(
          Option.some(x),
          x.getMessage());
      }
    }
  }

  private static final class LoginTask implements
    Runnable,
    AccountDataSetupListenerType
  {
    private final AccountBarcode           barcode;
    private final File                     base;
    private final Books                    books;
    private final BooksConfiguration       config;
    private final File                     file_barcode;
    private final File                     file_barcode_tmp;
    private final File                     file_pin;
    private final File                     file_pin_tmp;
    private final HTTPType                 http;
    private final AccountLoginListenerType listener;
    private final AccountPINListenerType   pin_listener;

    public LoginTask(
      final Books in_books,
      final HTTPType in_http,
      final BooksConfiguration in_config,
      final AccountBarcode in_barcode,
      final AccountPINListenerType in_pin_listener,
      final AccountLoginListenerType in_listener)
    {
      this.books = NullCheck.notNull(in_books);
      this.http = NullCheck.notNull(in_http);
      this.config = NullCheck.notNull(in_config);
      this.barcode = NullCheck.notNull(in_barcode);
      this.pin_listener = NullCheck.notNull(in_pin_listener);
      this.listener = NullCheck.notNull(in_listener);

      this.base = new File(this.config.getDirectory(), "data");
      this.file_barcode = new File(this.base, "barcode.txt");
      this.file_barcode_tmp = new File(this.base, "barcode.txt.tmp");
      this.file_pin = new File(this.base, "pin.txt");
      this.file_pin_tmp = new File(this.base, "pin.txt.tmp");
    }

    private void loginCheckCredentials()
      throws Exception
    {
      /**
       * Always request a PIN when doing an explicit login.
       */

      final OptionType<File> no_file = Option.none();
      final AccountPIN pin = Books.getInitialPIN(no_file, this.pin_listener);

      /**
       * Try hitting the loans URI to see if the credentials are valid. Loop
       * until either the given credentials are valid, or the user gives up.
       */

      final HTTPType h = LoginTask.this.http;
      final BooksConfiguration c = LoginTask.this.config;
      final AtomicReference<AccountPIN> chosen_pin =
        new AtomicReference<AccountPIN>();

      final PartialFunctionType<HTTPAuthType, HTTPResultType<Unit>, Exception> request =
        new PartialFunctionType<HTTPAuthType, HTTPResultType<Unit>, Exception>() {
          @Override public HTTPResultType<Unit> call(
            final HTTPAuthType auth)
            throws Exception
          {
            return h.head(Option.some(auth), c.getLoansURI());
          }
        };

      final HTTPResultOKType<Unit> r =
        Books.requestAuthenticationLoop(
          request,
          this.pin_listener,
          this.barcode,
          pin,
          chosen_pin);
      r.close();

      /**
       * Credentials were accepted, write them to files.
       */

      this.saveCredentials(NullCheck.notNull(chosen_pin.get()));
    }

    @Override public void onAccountDataSetupFailure(
      final OptionType<Throwable> error,
      final String message)
    {
      this.listener.onAccountLoginFailure(error, message);
    }

    @Override public void onAccountDataSetupSuccess()
    {
      try {
        this.loginCheckCredentials();
        this.listener.onAccountLoginSuccess(this.barcode);
      } catch (final Throwable e) {
        this.listener.onAccountLoginFailure(Option.some(e), e.getMessage());
      }
    }

    @Override public void run()
    {
      this.books.submitRunnable(new DataSetupTask(
        this.config,
        this.barcode,
        this));
    }

    private void saveCredentials(
      final AccountPIN actual_pin)
      throws IOException
    {
      this.barcode.writeToFile(this.file_barcode, this.file_barcode_tmp);
      actual_pin.writeToFile(this.file_pin, this.file_pin_tmp);
    }
  }

  private static final class LogoutTask implements Runnable
  {
    private final File                      base;
    private final BooksConfiguration        config;
    private final AccountLogoutListenerType listener;

    public LogoutTask(
      final BooksConfiguration in_config,
      final AccountLogoutListenerType in_listener)
    {
      this.config = NullCheck.notNull(in_config);
      this.listener = NullCheck.notNull(in_listener);
      this.base = new File(this.config.getDirectory(), "data");
    }

    @Override public void run()
    {
      try {
        final TreeTraverser<File> trav = Files.fileTreeTraverser();
        final ImmutableList<File> list =
          trav.postOrderTraversal(this.base).toList();

        for (int index = 0; index < list.size(); ++index) {
          final File file = list.get(index);
          final boolean ok = file.delete();
          if (ok == false) {
            throw new IOException("Unable to delete: " + file);
          }
        }

        this.listener.onAccountLogoutSuccess();
      } catch (final Throwable e) {
        this.listener.onAccountLogoutFailure(Option.some(e), e.getMessage());
      }
    }
  }

  private static final class SyncTask implements Runnable
  {
    private static OptionType<File> makeCover(
      final HTTPType http,
      final OptionType<URI> cover_opt,
      final File book_dir)
      throws Exception
    {
      final File cover_file = new File(book_dir, "cover.jpg");
      final File cover_file_tmp = new File(book_dir, "cover.jpg.tmp");

      if (cover_opt.isSome()) {
        final Some<URI> some = (Some<URI>) cover_opt;
        final URI cover_uri = some.get();
        SyncTask.makeCoverDownload(http, cover_file_tmp, cover_uri);
        FileUtilities.fileRename(cover_file_tmp, cover_file);
        return Option.some(cover_file);
      }

      return Option.none();
    }

    private static void makeCoverDownload(
      final HTTPType http,
      final File cover_file_tmp,
      final URI cover_uri)
      throws Exception
    {
      final OptionType<HTTPAuthType> no_auth = Option.none();
      final HTTPResultOKType<InputStream> r =
        http.get(no_auth, cover_uri, 0).matchResult(
          new HTTPResultToException<InputStream>());

      try {
        final FileOutputStream fs = new FileOutputStream(cover_file_tmp);
        try {
          ByteStreams.copy(r.getValue(), fs);
          fs.flush();
        } finally {
          fs.close();
        }
      } finally {
        r.close();
      }
    }

    /**
     * Save the acquisition feed entry.
     */

    private static void makeFeedEntry(
      final OPDSAcquisitionFeedEntry e,
      final File book_dir)
      throws IOException,
        FileNotFoundException
    {
      final File meta = new File(book_dir, "meta.dat");
      final File meta_tmp = new File(book_dir, "meta.dat.tmp");
      final ObjectOutputStream os =
        new ObjectOutputStream(new FileOutputStream(meta_tmp));
      os.writeObject(e);
      os.flush();
      os.close();
      FileUtilities.fileRename(meta_tmp, meta);
    }

    private final File                    base;
    private final BooksRegistryType       books;
    private final BooksConfiguration      config;
    private final OPDSFeedParserType      feed_parser;
    private final File                    file_barcode;
    private final File                    file_pin;
    private final HTTPType                http;
    private final AccountSyncListenerType listener;
    private final AccountPINListenerType  pin_listener;

    public SyncTask(
      final BooksConfiguration in_config,
      final BooksRegistryType in_books,
      final HTTPType in_http,
      final OPDSFeedParserType in_feed_parser,
      final AccountPINListenerType in_pin_listener,
      final AccountSyncListenerType in_listener)
    {
      this.books = NullCheck.notNull(in_books);
      this.config = NullCheck.notNull(in_config);
      this.http = NullCheck.notNull(in_http);
      this.feed_parser = NullCheck.notNull(in_feed_parser);
      this.listener = NullCheck.notNull(in_listener);
      this.pin_listener = NullCheck.notNull(in_pin_listener);

      this.base = new File(this.config.getDirectory(), "data");
      this.file_barcode = new File(this.base, "barcode.txt");
      this.file_pin = new File(this.base, "pin.txt");
    }

    @Override public void run()
    {
      try {
        this.sync();
        this.listener.onAccountSyncSuccess();
      } catch (final Throwable x) {
        this.listener.onAccountSyncFailure(Option.some(x), x.getMessage());
      }
    }

    private void sync()
      throws Exception
    {
      final AccountBarcode barcode =
        new AccountBarcode(FileUtilities.fileReadUTF8(this.file_barcode));

      final AccountPIN pin =
        Books.getInitialPIN(Option.some(this.file_pin), this.pin_listener);
      final URI loans_uri = this.config.getLoansURI();

      final PartialFunctionType<HTTPAuthType, HTTPResultType<InputStream>, Exception> r_feed_req =
        new PartialFunctionType<HTTPAuthType, HTTPResultType<InputStream>, Exception>() {
          @Override public HTTPResultType<InputStream> call(
            final HTTPAuthType auth)
            throws Exception
          {
            return SyncTask.this.http.get(Option.some(auth), loans_uri, 0);
          }
        };

      final AtomicReference<AccountPIN> chosen_pin =
        new AtomicReference<AccountPIN>();

      final HTTPResultOKType<InputStream> r_feed =
        Books.requestAuthenticationLoop(
          r_feed_req,
          this.pin_listener,
          barcode,
          pin,
          chosen_pin);

      try {
        this.syncFeedEntries(loans_uri, r_feed);
      } finally {
        r_feed.close();
      }
    }

    private void syncFeedEntries(
      final URI loans_uri,
      final HTTPResultOKType<InputStream> r_feed)
      throws Exception
    {
      final OPDSFeedType feed =
        this.feed_parser.parse(loans_uri, r_feed.getValue());

      if (feed instanceof OPDSNavigationFeed) {
        throw new IOException(
          "Expected an acquisition feed, but received a navigation feed");
      }

      final OPDSAcquisitionFeed acq_feed = (OPDSAcquisitionFeed) feed;
      final List<OPDSAcquisitionFeedEntry> entries =
        acq_feed.getFeedEntries();

      for (final OPDSAcquisitionFeedEntry e : entries) {
        final Book b = this.syncFeedEntry(NullCheck.notNull(e));
        this.books.bookUpdate(b);

        try {
          this.listener.onAccountSyncBook(b);
        } catch (final Throwable x) {
          // Ignore
        }
      }
    }

    private Book syncFeedEntry(
      final OPDSAcquisitionFeedEntry e)
      throws Exception
    {
      final BookID book_id = BookID.newIDFromEntry(e);
      final File book_dir = new File(this.base, book_id.toString());

      FileUtilities.createDirectory(book_dir);
      SyncTask.makeFeedEntry(e, book_dir);
      final OptionType<File> cover_opt =
        SyncTask.makeCover(this.http, e.getCover(), book_dir);

      return new Book(book_id, e, book_dir, cover_opt);
    }
  }

  private static AccountPIN getInitialPIN(
    final OptionType<File> file_pin_opt,
    final AccountPINListenerType pin_listener)
    throws AccountAuthenticationPINNotGivenError
  {
    OptionType<AccountPIN> pin_opt;

    /**
     * Try reading the PIN from the given file, if any. Otherwise, request a
     * PIN. Request a PIN if reading the file fails.
     */

    if (file_pin_opt.isSome()) {
      final Some<File> some = (Some<File>) file_pin_opt;
      final File file_pin = some.get();
      if (file_pin.isFile()) {
        try {
          pin_opt =
            Option.some(new AccountPIN(FileUtilities.fileReadUTF8(file_pin)));
        } catch (final IOException e) {
          pin_opt = pin_listener.onAccountPINRequested();
        }
      } else {
        pin_opt = pin_listener.onAccountPINRequested();
      }
    } else {
      pin_opt = pin_listener.onAccountPINRequested();
    }

    if (pin_opt.isNone()) {
      throw new AccountAuthenticationPINNotGivenError("No PIN given");
    }

    final Some<AccountPIN> pin_some = (Some<AccountPIN>) pin_opt;
    final AccountPIN pin = pin_some.get();
    return pin;
  }

  public static AccountsType newBooks(
    final ExecutorService in_exec,
    final OPDSFeedParserType in_feeds,
    final HTTPType in_http,
    final BooksConfiguration in_config)
  {
    return new Books(in_exec, in_feeds, in_http, in_config);
  }

  /**
   * Repeatedly try to perform the given http request <tt>c</tt>, using the
   * initial pin <tt>pin_initial</tt> and barcode <tt>barcode</tt>. The
   * request will be repeated until either the request succeeds, fails due to
   * a server or connection error unrelated to authentication, or the pin
   * listener fails to return a pin (typically because the user gave up trying
   * to provide one).
   */

  private static <T> HTTPResultOKType<T> requestAuthenticationLoop(
    final PartialFunctionType<HTTPAuthType, HTTPResultType<T>, Exception> c,
    final AccountPINListenerType pin_listener,
    final AccountBarcode barcode,
    final AccountPIN pin_initial,
    final AtomicReference<AccountPIN> chosen_pin)
    throws Exception
  {
    chosen_pin.set(pin_initial);

    final AtomicReference<HTTPResultOKType<T>> success =
      new AtomicReference<HTTPResultOKType<T>>();

    while (success.get() == null) {
      final AccountPIN pin = chosen_pin.get();
      if (pin == null) {
        throw new AccountAuthenticationPINNotGivenError("No PIN given");
      }

      final HTTPAuthBasic auth =
        new HTTPAuthBasic(barcode.toString(), pin.toString());
      final HTTPResultType<T> r = c.call(auth);

      r.matchResult(new HTTPResultMatcherType<T, Unit, Exception>() {
        @Override public Unit onHTTPError(
          final HTTPResultError<T> e)
          throws Exception
        {
          final String m =
            NullCheck.notNull(String.format(
              "%d: %s",
              e.getStatus(),
              e.getMessage()));

          switch (e.getStatus()) {
            case HttpURLConnection.HTTP_UNAUTHORIZED:
            {
              final OptionType<AccountPIN> pin_opt =
                pin_listener.onAccountPINRejected();
              if (pin_opt.isNone()) {
                chosen_pin.set(null);
              } else {
                final Some<AccountPIN> some = (Some<AccountPIN>) pin_opt;
                chosen_pin.set(some.get());
              }
              return Unit.unit();
            }
            default:
            {
              throw new IOException(m);
            }
          }
        }

        @Override public Unit onHTTPException(
          final HTTPResultException<T> e)
          throws Exception
        {
          throw e.getError();
        }

        @Override public Unit onHTTPOK(
          final HTTPResultOKType<T> e)
          throws Exception
        {
          success.set(e);
          return Unit.unit();
        }
      });
    }

    return NullCheck.notNull(success.get());
  }

  private final ConcurrentHashMap<BookID, Book> books;
  private final BooksConfiguration              config;
  private final ExecutorService                 exec;
  private final OPDSFeedParserType              feed_parser;
  private final HTTPType                        http;
  private final List<Future<?>>                 tasks;

  private Books(
    final ExecutorService in_exec,
    final OPDSFeedParserType in_feeds,
    final HTTPType in_http,
    final BooksConfiguration in_config)
  {
    this.exec = NullCheck.notNull(in_exec);
    this.feed_parser = NullCheck.notNull(in_feeds);
    this.http = NullCheck.notNull(in_http);
    this.config = NullCheck.notNull(in_config);
    this.books = new ConcurrentHashMap<BookID, Book>();
    this.tasks = new ArrayList<Future<?>>();
  }

  @Override public void accountLoadBooks(
    final AccountDataLoadListenerType listener)
  {
    NullCheck.notNull(listener);
    this.submitRunnable(new DataLoadTask(this, listener, this.config));
  }

  @Override public void accountLogin(
    final AccountBarcode barcode,
    final AccountPINListenerType pin_listener,
    final AccountLoginListenerType listener)
  {
    NullCheck.notNull(barcode);
    NullCheck.notNull(pin_listener);
    NullCheck.notNull(listener);

    this.submitRunnable(new LoginTask(
      this,
      this.http,
      this.config,
      barcode,
      pin_listener,
      listener));
  }

  @Override public void accountLogout(
    final AccountLogoutListenerType listener)
  {
    NullCheck.notNull(listener);

    this.stopAllTasks();
    this.submitRunnable(new LogoutTask(this.config, listener));
  }

  @Override public void accountSync(
    final AccountPINListenerType pin_listener,
    final AccountSyncListenerType listener)
  {
    NullCheck.notNull(listener);
    this.submitRunnable(new SyncTask(
      this.config,
      this,
      this.http,
      this.feed_parser,
      pin_listener,
      listener));
  }

  @Override public OptionType<Book> bookGet(
    final BookID id)
  {
    return Option.of(this.books.get(NullCheck.notNull(id)));
  }

  @Override public void bookUpdate(
    final Book b)
  {
    NullCheck.notNull(b);
    this.books.put(b.getID(), b);
  }

  private void stopAllTasks()
  {
    synchronized (this.tasks) {
      final Iterator<Future<?>> iter = this.tasks.iterator();
      while (iter.hasNext()) {
        try {
          final Future<?> f = iter.next();
          f.cancel(true);
          iter.remove();
        } catch (final Throwable x) {
          // Ignore
        }
      }
    }
  }

  private void submitRunnable(
    final Runnable r)
  {
    synchronized (this.tasks) {
      this.tasks.add(this.exec.submit(r));
    }
  }
}
