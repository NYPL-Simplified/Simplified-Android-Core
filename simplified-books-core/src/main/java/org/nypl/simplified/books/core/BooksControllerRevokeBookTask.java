package org.nypl.simplified.books.core;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Pair;
import com.io7m.jfunctional.PartialFunctionType;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NonNull;
import com.io7m.jnull.NullCheck;
import org.nypl.drm.core.AdobeAdeptExecutorType;
import org.nypl.simplified.http.core.HTTPAuthBasic;
import org.nypl.simplified.http.core.HTTPAuthType;
import org.nypl.simplified.http.core.HTTPResultError;
import org.nypl.simplified.http.core.HTTPResultException;
import org.nypl.simplified.http.core.HTTPResultMatcherType;
import org.nypl.simplified.http.core.HTTPResultOKType;
import org.nypl.simplified.http.core.HTTPResultType;
import org.nypl.simplified.http.core.HTTPType;
import org.nypl.simplified.opds.core.OPDSAvailabilityHeld;
import org.nypl.simplified.opds.core.OPDSAvailabilityHeldReady;
import org.nypl.simplified.opds.core.OPDSAvailabilityHoldable;
import org.nypl.simplified.opds.core.OPDSAvailabilityLoanable;
import org.nypl.simplified.opds.core.OPDSAvailabilityLoaned;
import org.nypl.simplified.opds.core.OPDSAvailabilityMatcherType;
import org.nypl.simplified.opds.core.OPDSAvailabilityOpenAccess;
import org.nypl.simplified.opds.core.OPDSAvailabilityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

final class BooksControllerRevokeBookTask
  implements Runnable, OPDSAvailabilityMatcherType<Unit, IOException>
{
  private static final Logger LOG;

  static {
    LOG = NullCheck.notNull(
      LoggerFactory.getLogger(BooksControllerRevokeBookTask.class));
  }

  private final BookID                                      book_id;
  private final BookDatabaseType                            books_database;
  private final BooksStatusCacheType                        books_status;
  private final HTTPType                                    http;
  private final OptionType<AdobeAdeptExecutorType>          adobe_drm;
  private final PartialFunctionType<URI, Unit, IOException> revoke_hold;
  private final PartialFunctionType<URI, Unit, IOException> revoke_loan;

  BooksControllerRevokeBookTask(
    final BookDatabaseType in_books_database,
    final BooksStatusCacheType in_books_status,
    final HTTPType in_http,
    final BookID in_book_id,
    final OptionType<AdobeAdeptExecutorType> in_adobe_drm)
  {
    this.http = NullCheck.notNull(in_http);
    this.book_id = NullCheck.notNull(in_book_id);
    this.books_database = NullCheck.notNull(in_books_database);
    this.books_status = NullCheck.notNull(in_books_status);
    this.adobe_drm = NullCheck.notNull(in_adobe_drm);

    this.revoke_hold = new PartialFunctionType<URI, Unit, IOException>()
    {
      @Override public Unit call(final URI u)
        throws IOException
      {
        BooksControllerRevokeBookTask.this.revoke(u, RevokeType.HOLD);
        return Unit.unit();
      }
    };

    this.revoke_loan = new PartialFunctionType<URI, Unit, IOException>()
    {
      @Override public Unit call(final URI u)
        throws IOException
      {
        BooksControllerRevokeBookTask.this.revoke(u, RevokeType.LOAN);
        return Unit.unit();
      }
    };
  }

  @Override public void run()
  {
    try {
      final BookDatabaseEntryType e =
        this.books_database.getBookDatabaseEntry(this.book_id);
      final BookSnapshot snap = e.getSnapshot();
      final OPDSAvailabilityType avail = snap.getEntry().getAvailability();

      avail.matchAvailability(this);
    } catch (final Throwable e) {
      BooksControllerRevokeBookTask.LOG.error(
        "could not revoke book {}: ", this.book_id, e);
    }
  }

  @Override public Unit onHeldReady(final OPDSAvailabilityHeldReady a)
    throws IOException
  {
    a.getRevoke().mapPartial(this.revoke_hold);
    return Unit.unit();
  }

  private void revoke(
    final URI u,
    final RevokeType type)
    throws IOException
  {
    final HTTPAuthType auth = this.getHTTPAuth();
    final HTTPResultType<InputStream> r =
      this.http.get(Option.some(auth), u, 0L);
    r.matchResult(
      new HTTPResultMatcherType<InputStream, Unit, IOException>()
      {
        @Override public Unit onHTTPError(final HTTPResultError<InputStream> e)
          throws IOException
        {
          final OptionType<Throwable> none = Option.none();
          final BookStatusRevokeFailed status = new BookStatusRevokeFailed(
            BooksControllerRevokeBookTask.this.book_id, none);
          BooksControllerRevokeBookTask.this.books_status.booksStatusUpdate(
            status);
          return Unit.unit();
        }

        @Override
        public Unit onHTTPException(final HTTPResultException<InputStream> e)
          throws IOException
        {
          final OptionType<Throwable> error =
            Option.some((Throwable) e.getError());
          final BookStatusRevokeFailed status = new BookStatusRevokeFailed(
            BooksControllerRevokeBookTask.this.book_id, error);
          BooksControllerRevokeBookTask.this.books_status.booksStatusUpdate(
            status);
          return Unit.unit();
        }

        @Override public Unit onHTTPOK(final HTTPResultOKType<InputStream> e)
          throws IOException
        {
          BooksControllerRevokeBookTask.this.deleteBookEntry();
          return Unit.unit();
        }
      });
  }

  private void deleteBookEntry()
    throws IOException
  {
    final BookDatabaseEntryType e =
      this.books_database.getBookDatabaseEntry(this.book_id);
    e.destroy();
    this.books_status.booksStatusClearFor(this.book_id);
  }

  @NonNull private HTTPAuthType getHTTPAuth()
    throws IOException
  {
    final Pair<AccountBarcode, AccountPIN> pair =
      this.books_database.credentialsGet();
    final AccountBarcode barcode = pair.getLeft();
    final AccountPIN pin = pair.getRight();
    return new HTTPAuthBasic(barcode.toString(), pin.toString());
  }

  @Override public Unit onHeld(final OPDSAvailabilityHeld a)
    throws IOException
  {
    a.getRevoke().mapPartial(this.revoke_hold);
    return Unit.unit();
  }

  @Override public Unit onHoldable(final OPDSAvailabilityHoldable a)
    throws IOException
  {
    BooksControllerRevokeBookTask.LOG.debug(
      "book status is {}, no revocation possible", a);
    return Unit.unit();
  }

  @Override public Unit onLoaned(final OPDSAvailabilityLoaned a)
    throws IOException
  {
    a.getRevoke().mapPartial(this.revoke_loan);
    return Unit.unit();
  }

  @Override public Unit onLoanable(final OPDSAvailabilityLoanable a)
    throws IOException
  {
    BooksControllerRevokeBookTask.LOG.debug(
      "book status is {}, no revocation possible", a);
    return Unit.unit();
  }

  @Override public Unit onOpenAccess(final OPDSAvailabilityOpenAccess a)
    throws IOException
  {
    a.getRevoke().mapPartial(this.revoke_loan);
    return Unit.unit();
  }

  private enum RevokeType
  {
    LOAN, HOLD
  }
}
