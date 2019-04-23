package org.nypl.simplified.books.book_registry;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;

import org.jetbrains.annotations.NotNull;
import org.nypl.drm.core.AdobeAdeptLoan;
import org.nypl.simplified.books.book_database.Book;
import org.nypl.simplified.books.book_database.BookFormat;
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

public final class BookStatus {
  private BookStatus() {
    throw new UnreachableCodeException();
  }

  public static BookStatusType fromBook(final Book book) {
    NullCheck.notNull(book, "Book");

    final OptionType<Calendar> no_expiry = Option.none();
    final OPDSAcquisitionFeedEntry e = book.getEntry();
    final boolean downloaded = book.isDownloaded();
    final boolean adobe_returnable = BookStatus.isAdobeReturnable(book);

    final OPDSAvailabilityType availability = e.getAvailability();
    return availability.matchAvailability(
      new OPDSAvailabilityMatcherType<BookStatusType, UnreachableCodeException>() {
        @Override
        public BookStatusType onHeldReady(final OPDSAvailabilityHeldReady a) {
          return onIsHeldReady(a, book);
        }

        @Override
        public BookStatusType onHeld(final OPDSAvailabilityHeld a) {
          return onIsHeldNotReady(a, book);
        }

        @Override
        public BookStatusType onHoldable(final OPDSAvailabilityHoldable a) {
          return onIsHoldable(book);
        }

        @Override
        public BookStatusType onLoaned(final OPDSAvailabilityLoaned a) {
          return onIsLoaned(a, adobe_returnable, downloaded, book);
        }

        @Override
        public BookStatusType onLoanable(final OPDSAvailabilityLoanable a) {
          return onIsLoanable(book);
        }

        @Override
        public BookStatusType onOpenAccess(final OPDSAvailabilityOpenAccess a) {
          return onIsOpenAccess(a, downloaded, book, no_expiry);
        }

        @Override
        public BookStatusType onRevoked(final OPDSAvailabilityRevoked a) {
          return onIsRevoked(a, book);
        }
      });
  }

  @NotNull
  private static BookStatusType onIsRevoked(
    final OPDSAvailabilityRevoked a,
    final Book book) {
    return new BookStatusRevoked(book.getId(), a.getRevoke());
  }

  @NotNull
  private static BookStatusType onIsOpenAccess(
    final OPDSAvailabilityOpenAccess a,
    final boolean downloaded,
    final Book book,
    final OptionType<Calendar> no_expiry) {
    final boolean returnable = a.getRevoke().isSome();
    if (downloaded) {
      return new BookStatusDownloaded(book.getId(), no_expiry, returnable);
    }
    return new BookStatusLoaned(book.getId(), no_expiry, returnable);
  }

  @NotNull
  private static BookStatusType onIsLoanable(final Book book) {
    return new BookStatusLoanable(book.getId());
  }

  @NotNull
  private static BookStatusType onIsLoaned(
    final OPDSAvailabilityLoaned a,
    final boolean adobe_returnable,
    final boolean downloaded,
    final Book book) {
    final boolean has_revoke = a.getRevoke().isSome();
    final boolean returnable = (has_revoke && adobe_returnable) || (has_revoke && downloaded);

    if (downloaded) {
      return new BookStatusDownloaded(book.getId(), a.getEndDate(), returnable);
    }
    return new BookStatusLoaned(book.getId(), a.getEndDate(), returnable);
  }

  @NotNull
  private static BookStatusType onIsHoldable(final Book book) {
    return new BookStatusHoldable(book.getId());
  }

  @NotNull
  private static BookStatusType onIsHeldNotReady(
    final OPDSAvailabilityHeld a,
    final Book book) {
    final boolean revocable = a.getRevoke().isSome();
    return new BookStatusHeld(
      book.getId(),
      a.getPosition(),
      a.getStartDate(),
      a.getEndDate(),
      revocable);
  }

  @NotNull
  private static BookStatusType onIsHeldReady(
    final OPDSAvailabilityHeldReady a,
    final Book book) {
    final boolean revocable = a.getRevoke().isSome();
    return new BookStatusHeldReady(book.getId(), a.getEndDate(), revocable);
  }

  private static boolean isAdobeReturnable(final Book book) {
    final BookFormat.BookFormatEPUB format =
      book.findFormat(BookFormat.BookFormatEPUB.class);
    if (format != null) {
      final AdobeAdeptLoan adobe = format.getAdobeRights();
      if (adobe != null) {
        return adobe.isReturnable();
      }
    }

    return false;
  }
}
