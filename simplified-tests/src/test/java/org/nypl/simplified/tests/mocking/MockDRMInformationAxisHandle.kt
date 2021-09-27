package org.nypl.simplified.tests.mocking

import org.nypl.simplified.books.api.BookDRMInformation
import org.nypl.simplified.books.book_database.api.BookDRMInformationHandle
import java.io.File

class MockDRMInformationAxisHandle : BookDRMInformationHandle.AxisHandle() {

  var infoField: BookDRMInformation.AXIS =
    BookDRMInformation.AXIS(
      license = null,
      userKey = null
    )

  override val info: BookDRMInformation.AXIS
    get() = this.infoField

  override fun copyInAxisLicense(file: File): BookDRMInformation.AXIS {
    this.infoField = this.infoField.copy(license = file)
    return this.infoField
  }

  override fun copyInAxisUserKey(file: File): BookDRMInformation.AXIS {
    this.infoField = this.infoField.copy(userKey = file)
    return this.infoField
  }
}
