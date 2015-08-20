package org.nypl.simplified.books.core;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.nypl.simplified.opds.core.OPDSAvailabilityHeld;
import org.nypl.simplified.opds.core.OPDSAvailabilityHoldable;
import org.nypl.simplified.opds.core.OPDSAvailabilityLoanable;
import org.nypl.simplified.opds.core.OPDSAvailabilityLoaned;
import org.nypl.simplified.opds.core.OPDSAvailabilityMatcherType;
import org.nypl.simplified.opds.core.OPDSAvailabilityOpenAccess;
import org.nypl.simplified.opds.core.OPDSAvailabilityReserved;
import org.nypl.simplified.opds.core.OPDSAvailabilityType;

import java.util.Calendar;

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

    final OPDSAvailabilityType availability = e.getAvailability();
    return availability.matchAvailability(
      new OPDSAvailabilityMatcherType<BookStatusType,
        UnreachableCodeException>()
      {
        @Override public BookStatusType onReserved(
          final OPDSAvailabilityReserved a)
        {
          return new BookStatusReserved(in_id, a.getEndDate());
        }

        @Override public BookStatusType onHeld(
          final OPDSAvailabilityHeld a)
        {
          return new BookStatusHeld(
            in_id,
            a.getPosition(),
            a.getStartDate(),
            a.getEndDate());
        }

        @Override public BookStatusType onHoldable(
          final OPDSAvailabilityHoldable a)
        {
          return new BookStatusHoldable(in_id);
        }

        @Override public BookStatusType onLoaned(
          final OPDSAvailabilityLoaned a)
        {
          if (downloaded) {
            return new BookStatusDownloaded(in_id, a.getEndDate());
          }
          return new BookStatusLoaned(in_id, a.getEndDate());
        }

        @Override public BookStatusType onLoanable(
          final OPDSAvailabilityLoanable a)
        {
          return new BookStatusLoanable(in_id);
        }

        @Override public BookStatusType onOpenAccess(
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
