package org.nypl.simplified.tests.books.audio

import org.librarysimplified.audiobook.license_check.spi.SingleLicenseCheckParameters
import org.librarysimplified.audiobook.license_check.spi.SingleLicenseCheckProviderType
import org.librarysimplified.audiobook.license_check.spi.SingleLicenseCheckResult
import org.librarysimplified.audiobook.license_check.spi.SingleLicenseCheckStatus
import org.librarysimplified.audiobook.license_check.spi.SingleLicenseCheckType

object AudioBookFailingLicenseChecks : SingleLicenseCheckProviderType {

  override val name: String = "Failing"

  override fun createLicenseCheck(parameters: SingleLicenseCheckParameters): SingleLicenseCheckType {
    return object : SingleLicenseCheckType {
      override fun execute(): SingleLicenseCheckResult {
        parameters.onStatusChanged.invoke(
          SingleLicenseCheckStatus("Failing", "About to fail...")
        )
        return SingleLicenseCheckResult.Failed("Failed")
      }
    }
  }
}
