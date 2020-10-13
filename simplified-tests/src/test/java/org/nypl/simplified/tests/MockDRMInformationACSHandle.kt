package org.nypl.simplified.tests

import org.nypl.drm.core.AdobeAdeptLoan
import org.nypl.simplified.books.api.BookDRMInformation
import org.nypl.simplified.books.book_database.api.BookDRMInformationHandle
import java.io.File

class MockDRMInformationACSHandle : BookDRMInformationHandle.ACSHandle() {

  var infoField: BookDRMInformation.ACS =
    BookDRMInformation.ACS(
      acsmFile = null,
      rights = null
    )

  override val info: BookDRMInformation.ACS
    get() = this.infoField

  override fun setACSMFile(
    acsm: File?
  ): BookDRMInformation.ACS {
    this.infoField = this.info.copy(acsmFile = acsm)
    return this.infoField
  }

  override fun setAdobeRightsInformation(
    loan: AdobeAdeptLoan?
  ): BookDRMInformation.ACS {
    if (loan == null) {
      this.infoField = this.info.copy(rights = null)
    } else {
      this.infoField = this.info.copy(rights = Pair(File(""), loan))
    }
    return this.infoField
  }
}
