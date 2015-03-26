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
import java.util.concurrent.atomic.AtomicBoolean;

import org.nypl.simplified.downloader.core.DownloaderType;
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
    private final AtomicBoolean            logged_in;
    private final AccountPIN               pin;

    public LoginTask(
      final Books in_books,
      final HTTPType in_http,
      final BooksConfiguration in_config,
      final AccountBarcode in_barcode,
      final AccountPIN in_pin,
      final AccountLoginListenerType in_listener,
      final AtomicBoolean in_logged_in)
    {
      this.books = NullCheck.notNull(in_books);
      this.http = NullCheck.notNull(in_http);
      this.config = NullCheck.notNull(in_config);
      this.barcode = NullCheck.notNull(in_barcode);
      this.pin = NullCheck.notNull(in_pin);
      this.listener = NullCheck.notNull(in_listener);
      this.logged_in = NullCheck.notNull(in_logged_in);

      this.base = new File(this.config.getDirectory(), "data");
      this.file_barcode = new File(this.base, "barcode.txt");
      this.file_barcode_tmp = new File(this.base, "barcode.txt.tmp");
      this.file_pin = new File(this.base, "pin.txt");
      this.file_pin_tmp = new File(this.base, "pin.txt.tmp");
    }

    private void loginCheckCredentials()
      throws Exception
    {
      final HTTPAuthType auth =
        new HTTPAuthBasic(this.barcode.toString(), this.pin.toString());
      final HTTPResultType<Unit> r =
        this.http.head(Option.some(auth), this.config.getLoansURI());

      r.matchResult(new HTTPResultMatcherType<Unit, Unit, Exception>() {
        @Override public Unit onHTTPError(
          final HTTPResultError<Unit> e)
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
              throw new AccountAuthenticationPINRejectedError(
                "Invalid barcode or PIN");
            }
            default:
            {
              throw new IOException(m);
            }
          }
        }

        @Override public Unit onHTTPException(
          final HTTPResultException<Unit> e)
          throws Exception
        {
          throw e.getError();
        }

        @Override public Unit onHTTPOK(
          final HTTPResultOKType<Unit> e)
          throws Exception
        {
          /**
           * Credentials were accepted, write them to files.
           */

          LoginTask.this.saveCredentials(LoginTask.this.pin);
          LoginTask.this.logged_in.set(true);
          return Unit.unit();
        }
      });
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
        this.listener.onAccountLoginSuccess(this.barcode, this.pin);
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
    private final AtomicBoolean             logged_in;

    public LogoutTask(
      final BooksConfiguration in_config,
      final AtomicBoolean in_logged_in,
      final AccountLogoutListenerType in_listener)
    {
      this.config = NullCheck.notNull(in_config);
      this.listener = NullCheck.notNull(in_listener);
      this.logged_in = NullCheck.notNull(in_logged_in);
      this.base = new File(this.config.getDirectory(), "data");
    }

    @Override public void run()
    {
      try {
        this.logged_in.set(false);

        if (this.base.isDirectory()) {
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
        } else {
          throw new IllegalStateException("Not logged in");
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

    public SyncTask(
      final BooksConfiguration in_config,
      final BooksRegistryType in_books,
      final HTTPType in_http,
      final OPDSFeedParserType in_feed_parser,
      final AccountSyncListenerType in_listener)
    {
      this.books = NullCheck.notNull(in_books);
      this.config = NullCheck.notNull(in_config);
      this.http = NullCheck.notNull(in_http);
      this.feed_parser = NullCheck.notNull(in_feed_parser);
      this.listener = NullCheck.notNull(in_listener);

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
        new AccountPIN(FileUtilities.fileReadUTF8(this.file_pin));

      final AccountSyncListenerType in_listener = this.listener;
      final URI loans_uri = this.config.getLoansURI();

      final HTTPAuthType auth =
        new HTTPAuthBasic(barcode.toString(), pin.toString());
      final HTTPResultType<InputStream> r =
        this.http.get(Option.some(auth), this.config.getLoansURI(), 0);

      r
        .matchResult(new HTTPResultMatcherType<InputStream, Unit, Exception>() {
          @Override public Unit onHTTPError(
            final HTTPResultError<InputStream> e)
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
                in_listener.onAccountSyncAuthenticationFailure("Invalid PIN");
                return Unit.unit();
              }
              default:
              {
                throw new IOException(m);
              }
            }
          }

          @Override public Unit onHTTPException(
            final HTTPResultException<InputStream> e)
            throws Exception
          {
            throw e.getError();
          }

          @Override public Unit onHTTPOK(
            final HTTPResultOKType<InputStream> e)
            throws Exception
          {
            try {
              SyncTask.this.syncFeedEntries(loans_uri, e);
              return Unit.unit();
            } finally {
              e.close();
            }
          }
        });
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

  public static BooksType newBooks(
    final ExecutorService in_exec,
    final OPDSFeedParserType in_feeds,
    final HTTPType in_http,
    final DownloaderType in_downloader,
    final BooksConfiguration in_config)
  {
    return new Books(in_exec, in_feeds, in_http, in_downloader, in_config);
  }

  private final ConcurrentHashMap<BookID, Book> books;
  private final BooksConfiguration              config;
  private final DownloaderType                  downloader;
  private final ExecutorService                 exec;
  private final OPDSFeedParserType              feed_parser;
  private final HTTPType                        http;
  private final AtomicBoolean                   logged_in;
  private final List<Future<?>>                 tasks;

  private Books(
    final ExecutorService in_exec,
    final OPDSFeedParserType in_feeds,
    final HTTPType in_http,
    final DownloaderType in_downloader,
    final BooksConfiguration in_config)
  {
    this.exec = NullCheck.notNull(in_exec);
    this.feed_parser = NullCheck.notNull(in_feeds);
    this.http = NullCheck.notNull(in_http);
    this.config = NullCheck.notNull(in_config);
    this.downloader = NullCheck.notNull(in_downloader);
    this.books = new ConcurrentHashMap<BookID, Book>();
    this.tasks = new ArrayList<Future<?>>();
    this.logged_in = new AtomicBoolean(false);
  }

  @Override public boolean accountIsLoggedIn()
  {
    return this.logged_in.get();
  }

  @Override public void accountLoadBooks(
    final AccountDataLoadListenerType listener)
  {
    NullCheck.notNull(listener);
    this.submitRunnable(new DataLoadTask(this, listener, this.config));
  }

  @Override public void accountLogin(
    final AccountBarcode barcode,
    final AccountPIN pin,
    final AccountLoginListenerType listener)
  {
    NullCheck.notNull(barcode);
    NullCheck.notNull(pin);
    NullCheck.notNull(listener);

    this.submitRunnable(new LoginTask(
      this,
      this.http,
      this.config,
      barcode,
      pin,
      listener,
      this.logged_in));
  }

  @Override public void accountLogout(
    final AccountLogoutListenerType listener)
  {
    NullCheck.notNull(listener);

    this.stopAllTasks();
    this.books.clear();
    this.downloader.downloadDestroyAll();
    this
      .submitRunnable(new LogoutTask(this.config, this.logged_in, listener));
  }

  @Override public void accountSync(
    final AccountSyncListenerType listener)
  {
    NullCheck.notNull(listener);
    this.submitRunnable(new SyncTask(
      this.config,
      this,
      this.http,
      this.feed_parser,
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
