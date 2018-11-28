package org.nypl.simplified.books.core

import com.io7m.jfunctional.Option
import com.io7m.jfunctional.Some
import com.io7m.junreachable.UnreachableCodeException
import org.nypl.drm.core.AdobeAdeptLoan
import org.nypl.simplified.books.core.BookDatabaseEntryFormatSnapshot.BookDatabaseEntryFormatSnapshotAudioBook
import org.nypl.simplified.books.core.BookDatabaseEntryFormatSnapshot.BookDatabaseEntryFormatSnapshotEPUB
import org.nypl.simplified.opds.core.OPDSAvailabilityHeld
import org.nypl.simplified.opds.core.OPDSAvailabilityHeldReady
import org.nypl.simplified.opds.core.OPDSAvailabilityHoldable
import org.nypl.simplified.opds.core.OPDSAvailabilityLoanable
import org.nypl.simplified.opds.core.OPDSAvailabilityLoaned
import org.nypl.simplified.opds.core.OPDSAvailabilityMatcherType
import org.nypl.simplified.opds.core.OPDSAvailabilityOpenAccess
import org.nypl.simplified.opds.core.OPDSAvailabilityRevoked
import java.util.Calendar

internal class BookStatus private constructor() {

  init {
    throw UnreachableCodeException()
  }

  companion object {

    fun fromSnapshot(
      id: BookID,
      snap: BookDatabaseEntrySnapshot): BookStatusType {

      val noExpiry = Option.none<Calendar>()
      val entry = snap.entry

      val downloaded = snap.isDownloaded
      val adobeReturnable = BookStatus.isAdobeReturnable(snap)

      val availability = entry.availability
      return availability.matchAvailability(
        object : OPDSAvailabilityMatcherType<BookStatusType, UnreachableCodeException> {
          override fun onHeldReady(a: OPDSAvailabilityHeldReady): BookStatusType {
            val revocable = a.revoke.isSome
            return BookStatusHeldReady(id, a.endDate, revocable)
          }

          override fun onHeld(a: OPDSAvailabilityHeld): BookStatusType {
            val revocable = a.revoke.isSome
            return BookStatusHeld(id, a.position, a.startDate, a.endDate, revocable)
          }

          override fun onHoldable(a: OPDSAvailabilityHoldable): BookStatusType {
            return BookStatusHoldable(id)
          }

          override fun onLoaned(a: OPDSAvailabilityLoaned): BookStatusType {
            val hasRevoke = a.revoke.isSome
            val returnable = hasRevoke && adobeReturnable || hasRevoke && downloaded

            return if (downloaded) {
              BookStatusDownloaded(id, a.endDate, returnable)
            } else BookStatusLoaned(id, a.endDate, returnable)
          }

          override fun onLoanable(a: OPDSAvailabilityLoanable): BookStatusType {
            return BookStatusLoanable(id)
          }

          override fun onOpenAccess(a: OPDSAvailabilityOpenAccess): BookStatusType {
            val returnable = a.revoke.isSome
            return if (downloaded) {
              BookStatusDownloaded(id, noExpiry, returnable)
            } else BookStatusLoaned(id, noExpiry, returnable)
          }

          override fun onRevoked(a: OPDSAvailabilityRevoked): BookStatusType {
            return BookStatusRevoked(id, a.revoke)
          }
        })
    }

    private fun isAdobeReturnable(snap: BookDatabaseEntrySnapshot): Boolean {
      for (format in snap.formats) {
        when (format) {
          is BookDatabaseEntryFormatSnapshotEPUB -> {
            val rights = format.adobeRights
            if (rights is Some<AdobeAdeptLoan>) {
              return rights.get().isReturnable
            }
            return false
          }
          is BookDatabaseEntryFormatSnapshotAudioBook -> {

          }
        }
      }
      return false
    }
  }
}
