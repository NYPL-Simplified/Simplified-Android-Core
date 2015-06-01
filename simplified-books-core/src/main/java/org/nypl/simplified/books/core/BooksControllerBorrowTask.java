package org.nypl.simplified.books.core;

import java.io.File;
import java.io.IOError;
import java.io.IOException;

import org.nypl.simplified.downloader.core.DownloadListenerType;
import org.nypl.simplified.downloader.core.DownloadSnapshot;
import org.nypl.simplified.downloader.core.DownloaderType;
import org.nypl.simplified.http.core.HTTPAuthBasic;
import org.nypl.simplified.http.core.HTTPAuthType;
import org.nypl.simplified.opds.core.OPDSAcquisition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.Pair;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnimplementedCodeException;

@SuppressWarnings("boxing") final class BooksControllerBorrowTask implements
  Runnable,
  DownloadListenerType
{
  private static final Logger          LOG;

  static {
    LOG =
      NullCheck.notNull(LoggerFactory
        .getLogger(BooksControllerBorrowTask.class));
  }

  private final OPDSAcquisition        acq;
  private final BookID                 book_id;
  private final BookDatabaseType       books_database;
  private final BooksStatusCacheType   books_status;
  private final DownloaderType         downloader;
  private final BookBorrowListenerType listener;
  private final String                 title;

  BooksControllerBorrowTask(
    final BookDatabaseType in_books_database,
    final BooksStatusCacheType in_books_status,
    final DownloaderType in_downloader,
    final BookID in_book_id,
    final OPDSAcquisition in_acq,
    final String in_title,
    final BookBorrowListenerType in_listener)
  {
    this.downloader = NullCheck.notNull(in_downloader);
    this.book_id = NullCheck.notNull(in_book_id);
    this.acq = NullCheck.notNull(in_acq);
    this.listener = NullCheck.notNull(in_listener);
    this.books_database = NullCheck.notNull(in_books_database);
    this.books_status = NullCheck.notNull(in_books_status);
    this.title = NullCheck.notNull(in_title);
  }

  @Override public void downloadCancelled(
    final DownloadSnapshot snap)
  {
    final BookStatusDownloadCancelled status =
      new BookStatusDownloadCancelled(this.book_id, snap);
    this.books_status.booksStatusUpdate(status);
  }

  @Override public void downloadCleanedUp(
    final DownloadSnapshot snap)
  {
    // Don't care
  }

  @Override public void downloadCompleted(
    final DownloadSnapshot snap)
  {
    final BookStatusDownloadInProgress status =
      new BookStatusDownloadInProgress(this.book_id, snap);
    this.books_status.booksStatusUpdate(status);
  }

  @Override public void downloadCompletedTake(
    final DownloadSnapshot snap,
    final File file_data)
  {
    try {
      final BookDatabaseEntryType e =
        this.books_database.getBookDatabaseEntry(this.book_id);

      e.copyInBookFromSameFilesystem(file_data);
      final BookStatusDownloaded status =
        new BookStatusDownloaded(this.book_id);
      this.books_status.booksSnapshotUpdate(this.book_id, e.getSnapshot());
      this.books_status.booksStatusUpdate(status);
    } catch (final IOException e) {
      throw new IOError(e);
    }
  }

  @Override public void downloadCompletedTakeFailed(
    final DownloadSnapshot snap,
    final Throwable x)
  {
    final BookStatusDownloadFailed status =
      new BookStatusDownloadFailed(this.book_id, snap, Option.some(x));
    this.books_status.booksStatusUpdate(status);
  }

  @Override public void downloadCompletedTaken(
    final DownloadSnapshot snap)
  {
    // Don't care
  }

  @Override public void downloadFailed(
    final DownloadSnapshot snap,
    final Throwable e)
  {
    final BookStatusDownloadFailed status =
      new BookStatusDownloadFailed(this.book_id, snap, Option.some(e));
    this.books_status.booksStatusUpdate(status);
  }

  @Override public void downloadPaused(
    final DownloadSnapshot snap)
  {
    final BookStatusDownloadingPaused status =
      new BookStatusDownloadingPaused(this.book_id, snap);
    this.books_status.booksStatusUpdate(status);
  }

  @Override public void downloadReceivedData(
    final DownloadSnapshot snap)
  {
    final BookStatusDownloadInProgress status =
      new BookStatusDownloadInProgress(this.book_id, snap);
    this.books_status.booksStatusUpdate(status);
  }

  @Override public void downloadResumed(
    final DownloadSnapshot snap)
  {
    final BookStatusDownloadInProgress status =
      new BookStatusDownloadInProgress(this.book_id, snap);
    this.books_status.booksStatusUpdate(status);
  }

  @Override public void downloadStarted(
    final DownloadSnapshot snap)
  {
    final BookStatusDownloadInProgress status =
      new BookStatusDownloadInProgress(this.book_id, snap);
    this.books_status.booksStatusUpdate(status);
  }

  @Override public void downloadStartedReceivingData(
    final DownloadSnapshot snap)
  {
    final BookStatusDownloadInProgress status =
      new BookStatusDownloadInProgress(this.book_id, snap);
    this.books_status.booksStatusUpdate(status);
  }

  @Override public void run()
  {
    try {
      this.runAcquisition();
      this.listener.onBookBorrowSuccess(this.book_id);
    } catch (final Throwable x) {
      this.listener.onBookBorrowFailure(this.book_id, Option.some(x));
    }
  }

  private void runAcquisition()
    throws Exception
  {
    switch (this.acq.getType()) {
      case ACQUISITION_GENERIC:
      case ACQUISITION_BORROW:
      case ACQUISITION_OPEN_ACCESS:
      {
        this.runAcquisitionBorrow();
        break;
      }
      case ACQUISITION_BUY:
      case ACQUISITION_SAMPLE:
      case ACQUISITION_SUBSCRIBE:
      {
        throw new UnimplementedCodeException();
      }
    }
  }

  private void runAcquisitionBorrow()
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

    final long did =
      this.downloader.downloadEnqueue(
        Option.some(auth),
        this.acq.getURI(),
        this.title,
        this);

    BooksControllerBorrowTask.LOG.debug(
      "book {}: download id {}",
      this.book_id,
      did);
    e.setDownloadID(did);
  }
}
