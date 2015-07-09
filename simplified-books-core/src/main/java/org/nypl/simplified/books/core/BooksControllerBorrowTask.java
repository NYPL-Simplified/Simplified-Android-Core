package org.nypl.simplified.books.core;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.concurrent.ConcurrentHashMap;

import org.nypl.simplified.downloader.core.DownloadListenerType;
import org.nypl.simplified.downloader.core.DownloadType;
import org.nypl.simplified.downloader.core.DownloaderType;
import org.nypl.simplified.http.core.HTTPAuthBasic;
import org.nypl.simplified.http.core.HTTPAuthType;
import org.nypl.simplified.opds.core.OPDSAcquisition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Pair;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnimplementedCodeException;
import com.io7m.junreachable.UnreachableCodeException;

final class BooksControllerBorrowTask implements
  Runnable,
  DownloadListenerType
{
  private static final Logger                           LOG;

  static {
    LOG =
      NullCheck.notNull(LoggerFactory
        .getLogger(BooksControllerBorrowTask.class));
  }

  private final OPDSAcquisition                         acq;
  private final BookID                                  book_id;
  private final BookDatabaseType                        books_database;
  private final BooksStatusCacheType                    books_status;
  private final DownloaderType                          downloader;
  private final BookBorrowListenerType                  listener;
  private final ConcurrentHashMap<BookID, DownloadType> downloads;

  BooksControllerBorrowTask(
    final BookDatabaseType in_books_database,
    final BooksStatusCacheType in_books_status,
    final DownloaderType in_downloader,
    final ConcurrentHashMap<BookID, DownloadType> in_downloads,
    final BookID in_book_id,
    final OPDSAcquisition in_acq,
    final BookBorrowListenerType in_listener)
  {
    this.downloader = NullCheck.notNull(in_downloader);
    this.downloads = NullCheck.notNull(in_downloads);
    this.book_id = NullCheck.notNull(in_book_id);
    this.acq = NullCheck.notNull(in_acq);
    this.listener = NullCheck.notNull(in_listener);
    this.books_database = NullCheck.notNull(in_books_database);
    this.books_status = NullCheck.notNull(in_books_status);
  }

  @Override public void run()
  {
    try {
      final DownloadType d = this.runAcquisition();
      this.downloads.put(this.book_id, d);
      this.listener.onBookBorrowSuccess(this.book_id);
    } catch (final Throwable x) {
      this.listener.onBookBorrowFailure(this.book_id, Option.some(x));
    }
  }

  private DownloadType runAcquisition()
    throws Exception
  {
    switch (this.acq.getType()) {
      case ACQUISITION_OPEN_ACCESS:
      case ACQUISITION_GENERIC:
      {
        return this.runAcquisitionBorrow();
      }
      case ACQUISITION_BORROW:
      case ACQUISITION_BUY:
      case ACQUISITION_SAMPLE:
      case ACQUISITION_SUBSCRIBE:
      {
        throw new UnimplementedCodeException();
      }
    }

    throw new UnreachableCodeException();
  }

  private DownloadType runAcquisitionBorrow()
    throws Exception
  {
    BooksControllerBorrowTask.LOG.debug(
      "book {}: creating book database entry",
      this.book_id);

    final BookDatabaseEntryType e =
      this.books_database.getBookDatabaseEntry(this.book_id);
    e.create();

    BooksControllerBorrowTask.LOG.debug(
      "book {}: starting download",
      this.book_id);

    final Pair<AccountBarcode, AccountPIN> p =
      this.books_database.credentialsGet();
    final AccountBarcode barcode = p.getLeft();
    final AccountPIN pin = p.getRight();
    final HTTPAuthType auth =
      new HTTPAuthBasic(barcode.toString(), pin.toString());

    return this.downloader.download(
      this.acq.getURI(),
      Option.some(auth),
      this);
  }

  @Override public void onDownloadStarted(
    final DownloadType d,
    final long expected_total)
  {
    final OptionType<Calendar> none = Option.none();
    final BookStatusDownloadInProgress status =
      new BookStatusDownloadInProgress(this.book_id, 0L, expected_total, none);
    this.books_status.booksStatusUpdate(status);
  }

  @Override public void onDownloadDataReceived(
    final DownloadType d,
    final long running_total,
    final long expected_total)
  {
    final OptionType<Calendar> none = Option.none();
    final BookStatusDownloadInProgress status =
      new BookStatusDownloadInProgress(
        this.book_id,
        running_total,
        expected_total,
        none);
    this.books_status.booksStatusUpdate(status);
  }

  @Override public void onDownloadCancelled(
    final DownloadType d)
  {
    final OptionType<Calendar> none = Option.none();
    final BookStatusLoaned status = new BookStatusLoaned(this.book_id, none);
    this.books_status.booksStatusUpdate(status);
  }

  @Override public void onDownloadFailed(
    final DownloadType d,
    final int status_code,
    final long running_total,
    final OptionType<Exception> exception)
  {
    final OptionType<Calendar> none = Option.none();
    final BookStatusDownloadFailed status =
      new BookStatusDownloadFailed(this.book_id, exception, none);
    this.books_status.booksStatusUpdate(status);
  }

  @Override public void onDownloadCompleted(
    final DownloadType d,
    final File file)
    throws IOException
  {
    final BookDatabaseEntryType e =
      this.books_database.getBookDatabaseEntry(this.book_id);

    e.copyInBookFromSameFilesystem(file);
    file.delete();

    final OptionType<Calendar> none = Option.none();
    final BookStatusDownloaded status =
      new BookStatusDownloaded(this.book_id, none);
    this.books_status.booksStatusUpdate(status);
  }
}
