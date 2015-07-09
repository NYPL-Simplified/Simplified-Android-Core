package org.nypl.simplified.books.core;

import java.util.Calendar;

import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.nypl.simplified.opds.core.OPDSAvailabilityHeld;
import org.nypl.simplified.opds.core.OPDSAvailabilityHoldable;
import org.nypl.simplified.opds.core.OPDSAvailabilityLoanable;
import org.nypl.simplified.opds.core.OPDSAvailabilityLoaned;
import org.nypl.simplified.opds.core.OPDSAvailabilityMatcherType;
import org.nypl.simplified.opds.core.OPDSAvailabilityOpenAccess;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnimplementedCodeException;
import com.io7m.junreachable.UnreachableCodeException;

final class BookStatus
{
  private BookStatus()
  {
    throw new UnreachableCodeException();
  }

  public static BookStatusType fromSnapshot(
    final BookID in_id,
    final BookSnapshot in_snap)
  {
    NullCheck.notNull(in_id);
    NullCheck.notNull(in_snap);

    final OptionType<Calendar> no_expiry = Option.none();
    final OPDSAcquisitionFeedEntry e = in_snap.getEntry();
    final boolean downloaded = in_snap.getBook().isSome();
    return e
      .getAvailability()
      .matchAvailability(
        new OPDSAvailabilityMatcherType<BookStatusLoanedType, UnreachableCodeException>() {
          @Override public BookStatusLoanedType onHeld(
            final OPDSAvailabilityHeld a)
          {
            // TODO Auto-generated method stub
            throw new UnimplementedCodeException();
          }

          @Override public BookStatusLoanedType onHoldable(
            final OPDSAvailabilityHoldable a)
          {
            // TODO Auto-generated method stub
            throw new UnimplementedCodeException();
          }

          @Override public BookStatusLoanedType onLoaned(
            final OPDSAvailabilityLoaned a)
          {
            if (downloaded) {
              return new BookStatusDownloaded(in_id, a.getEndDate());
            }
            return new BookStatusLoaned(in_id, a.getEndDate());
          }

          @Override public BookStatusLoanedType onLoanable(
            final OPDSAvailabilityLoanable a)
          {
            return new BookStatusDownloaded(in_id, no_expiry);
          }

          @Override public BookStatusLoanedType onOpenAccess(
            final OPDSAvailabilityOpenAccess a)
          {
            if (downloaded) {
              return new BookStatusDownloaded(in_id, no_expiry);
            }
            return new BookStatusLoaned(in_id, no_expiry);
          }
        });
  }
}
