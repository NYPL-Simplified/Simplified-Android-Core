package org.nypl.simplified.app.catalog

import android.content.res.Resources
import org.nypl.simplified.app.R
import org.nypl.simplified.books.controller.api.BookBorrowStringResourcesType
import org.nypl.simplified.opds.core.OPDSAcquisition
import org.nypl.simplified.opds.core.OPDSAvailabilityType
import java.io.File

class CatalogBookBorrowStrings(val resources: Resources) : BookBorrowStringResourcesType {

  override fun borrowBookFeedLoadingFailed(cause: String): String =
    this.resources.getString(R.string.borrowBookFeedLoadingFailed, cause)

  override val borrowBookCoverUnexpectedException: String
    get() = this.resources.getString(R.string.unexpectedException)

  override val borrowBookSavingCover: String
    get() = this.resources.getString(R.string.borrowBookSavingCover)

  override val borrowBookFetchingCover: String
    get() = this.resources.getString(R.string.borrowBookFetchingCover)

  override val borrowBookUnexpectedException: String
    get() = this.resources.getString(R.string.unexpectedException)

  override fun borrowBookUnsupportedAcquisition(type: OPDSAcquisition.Relation): String {
    return this.resources.getString(R.string.borrowBookUnsupportedAcquisition, type)
  }

  override fun borrowBookFulfillACSMConnectorFailed(errorCode: String): String {
    return this.resources.getString(R.string.borrowBookFulfillACSMConnectorFailed, errorCode)
  }

  override val borrowBookFulfillACSMFailed: String
    get() = this.resources.getString(R.string.borrowBookFulfillACSMFailed)

  override val borrowBookFulfillACSMGettingDeviceCredentialsNotActivated: String
    get() = this.resources.getString(R.string.borrowBookFulfillACSMGettingDeviceCredentialsNotActivated)

  override val borrowBookFulfillACSMConnectorOK: String
    get() = this.resources.getString(R.string.borrowBookFulfillACSMConnectorOK)

  override val borrowBookFulfillACSMGettingDeviceCredentialsOK: String
    get() = this.resources.getString(R.string.borrowBookFulfillACSMGettingDeviceCredentialsOK)

  override val borrowBookFulfillACSMGettingDeviceCredentials: String
    get() = this.resources.getString(R.string.borrowBookFulfillACSMGettingDeviceCredentials)

  override val borrowBookFulfillACSMReadFailed: String
    get() = this.resources.getString(R.string.borrowBookFulfillACSMReadFailed)

  override val borrowBookFulfillACSMRead: String
    get() = this.resources.getString(R.string.borrowBookFulfillACSMRead)

  override fun borrowBookBorrowAvailabilityInappropriate(availability: OPDSAvailabilityType): String {
    return this.resources.getString(R.string.borrowBookBorrowAvailabilityInappropriate, availability.javaClass.simpleName)
  }

  override fun borrowBookFulfillDownloadFailed(cause: String): String =
    this.resources.getString(R.string.borrowBookFulfillDownloadFailed, cause)

  override val borrowBookFulfillCancelled: String
    get() = this.resources.getString(R.string.borrowBookFulfillCancelled)

  override fun borrowBookBorrowForAvailability(availability: OPDSAvailabilityType): String {
    return this.resources.getString(
      R.string.borrowBookBorrowForAvailability,
      availability.javaClass.simpleName)
  }

  override fun borrowBookFulfillDownloaded(file: File, contentType: String): String {
    return this.resources.getString(R.string.borrowBookFulfillDownloaded, contentType)
  }

  override fun borrowBookFulfillACSMCheckContentTypeOK(contentType: String): String {
    return this.resources.getString(R.string.borrowBookFulfillACSMCheckContentTypeOK, contentType)
  }

  override fun borrowBookSaving(receivedContentType: String, expectedContentTypes: Set<String>): String {
    return this.resources.getString(R.string.borrowBookSaving, receivedContentType)
  }

  override fun borrowBookSavingCheckingContentType(receivedContentType: String, expectedContentTypes: Set<String>): String {
    return this.resources.getString(R.string.borrowBookSavingCheckingContentType, receivedContentType)
  }

  override val borrowBookSavingCheckingContentTypeUnacceptable: String
    get() = this.resources.getString(R.string.borrowBookSavingCheckingContentTypeUnacceptable)

  override val borrowBookSavingCheckingContentTypeOK: String
    get() = this.resources.getString(R.string.borrowBookSavingCheckingContentTypeOK)

  override val borrowBookFulfillBearerTokenOK: String
    get() = this.resources.getString(R.string.borrowBookFulfillBearerTokenOK)

  override val borrowBookFulfillBearerToken: String
    get() = this.resources.getString(R.string.borrowBookFulfillBearerToken)

  override val borrowBookFulfillACSMCheckContentType: String
    get() = this.resources.getString(R.string.borrowBookFulfillACSMCheckContentType)

  override val borrowBookFulfillACSMUnsupportedContentType: String
    get() = this.resources.getString(R.string.borrowBookFulfillACSMUnsupportedContentType)

  override val borrowBookFulfillACSMParseFailed: String
    get() = this.resources.getString(R.string.borrowBookFulfillACSMParseFailed)

  override val borrowBookFulfillACSMParse: String
    get() = this.resources.getString(R.string.borrowBookFulfillACSMParse)

  override val borrowBookFulfillACSMConnector: String
    get() = this.resources.getString(R.string.borrowBookFulfillACSMConnector)

  override val borrowBookFulfillUnparseableBearerToken: String
    get() = this.resources.getString(R.string.borrowBookFulfillUnparseableBearerToken)

  override val borrowBookFulfillDRMNotSupported: String
    get() = this.resources.getString(R.string.borrowBookFulfillDRMNotSupported)

  override val borrowBookFulfillACSM: String
    get() = this.resources.getString(R.string.borrowBookFulfillACSM)

  override val borrowBookFulfillTimedOut: String
    get() = this.resources.getString(R.string.borrowBookFulfillTimedOut)

  override val borrowBookFulfillDownload: String
    get() = this.resources.getString(R.string.borrowBookFulfillDownload)

  override val borrowBookFulfillNoUsableAcquisitions: String
    get() = this.resources.getString(R.string.borrowBookFulfillNoUsableAcquisitions)

  override val borrowBookFulfill: String
    get() = this.resources.getString(R.string.borrowBookFulfill)

  override val borrowBookBundledCopyFailed: String
    get() = this.resources.getString(R.string.borrowBookBundledCopyFailed)

  override val borrowBookBundledCopy: String
    get() = this.resources.getString(R.string.borrowBookBundledCopy)

  override val borrowBookBadBorrowFeed: String
    get() = this.resources.getString(R.string.borrowBookBadBorrowFeed)

  override val borrowBookGetFeedEntry: String
    get() = this.resources.getString(R.string.borrowBookGetFeedEntry)

  override val borrowBookDatabaseFailed: String
    get() = this.resources.getString(R.string.borrowBookDatabaseFailed)

  override val borrowStarted: String
    get() = this.resources.getString(R.string.borrowStarted)

  override val borrowBookDatabaseCreateOrUpdate: String
    get() = this.resources.getString(R.string.borrowBookDatabaseCreateOrUpdate)

  override val borrowBookDatabaseUpdated: String
    get() = this.resources.getString(R.string.borrowBookDatabaseUpdated)
}
