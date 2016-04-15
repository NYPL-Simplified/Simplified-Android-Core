package org.nypl.simplified.books.core;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;
import org.nypl.drm.core.AdobeAdeptLoan;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.nypl.simplified.opds.core.OPDSAvailabilityHeld;
import org.nypl.simplified.opds.core.OPDSAvailabilityHeldReady;
import org.nypl.simplified.opds.core.OPDSAvailabilityHoldable;
import org.nypl.simplified.opds.core.OPDSAvailabilityLoanable;
import org.nypl.simplified.opds.core.OPDSAvailabilityLoaned;
import org.nypl.simplified.opds.core.OPDSAvailabilityMatcherType;
import org.nypl.simplified.opds.core.OPDSAvailabilityOpenAccess;
import org.nypl.simplified.opds.core.OPDSAvailabilityRevoked;
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
    final BookDatabaseEntrySnapshot in_snap)
  {
    NullCheck.notNull(in_id);
    NullCheck.notNull(in_snap);

    final OptionType<Calendar> no_expiry = Option.none();
    final OPDSAcquisitionFeedEntry e = in_snap.getEntry();
    final boolean downloaded = in_snap.getBook().isSome();
    final boolean adobe_returnable = BookStatus.isAdobeReturnable(in_snap);

    final OPDSAvailabilityType availability = e.getAvailability();
    return availability.matchAvailability(
      new OPDSAvailabilityMatcherType<BookStatusType,
        UnreachableCodeException>()
      {
        @Override public BookStatusType onHeldReady(
          final OPDSAvailabilityHeldReady a)
        {
          final boolean revocable = a.getRevoke().isSome();
          return new BookStatusHeldReady(in_id, a.getEndDate(), revocable);
        }

        @Override public BookStatusType onHeld(
          final OPDSAvailabilityHeld a)
        {
          final boolean revocable = a.getRevoke().isSome();
          return new BookStatusHeld(
            in_id,
            a.getPosition(),
            a.getStartDate(),
            a.getEndDate(),
            revocable);
        }

        @Override public BookStatusType onHoldable(
          final OPDSAvailabilityHoldable a)
        {
          return new BookStatusHoldable(in_id);
        }

        @Override public BookStatusType onLoaned(
          final OPDSAvailabilityLoaned a)
        {
          final boolean has_revoke = a.getRevoke().isSome();
          final boolean returnable = (has_revoke && adobe_returnable) || (has_revoke && downloaded);

          if (downloaded) {
            return new BookStatusDownloaded(in_id, a.getEndDate(), returnable);
          }
          return new BookStatusLoaned(in_id, a.getEndDate(), returnable);
        }

        @Override public BookStatusType onLoanable(
          final OPDSAvailabilityLoanable a)
        {
          return new BookStatusLoanable(in_id);
        }

        @Override public BookStatusType onOpenAccess(
          final OPDSAvailabilityOpenAccess a)
        {
          final boolean returnable = a.getRevoke().isSome();
          if (downloaded) {
            return new BookStatusDownloaded(in_id, no_expiry, returnable);
          }
          return new BookStatusLoaned(in_id, no_expiry, returnable);
        }

        @Override
        public BookStatusType onRevoked(final OPDSAvailabilityRevoked a)
        {
          return new BookStatusRevoked(in_id, a.getRevoke());
        }
      });
  }

  private static boolean isAdobeReturnable(
    final BookDatabaseEntrySnapshot in_snap)
  {
    final OptionType<AdobeAdeptLoan> adobe_opt = in_snap.getAdobeRights();
    if (adobe_opt.isSome()) {
      final AdobeAdeptLoan adobe = ((Some<AdobeAdeptLoan>) adobe_opt).get();
      return adobe.isReturnable();
    } else {
      return false;
    }
  }

}
