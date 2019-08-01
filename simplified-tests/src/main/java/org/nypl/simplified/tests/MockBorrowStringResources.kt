package org.nypl.simplified.tests

import org.nypl.simplified.books.controller.api.BookBorrowStringResourcesType
import org.nypl.simplified.opds.core.OPDSAcquisition
import org.nypl.simplified.opds.core.OPDSAvailabilityType
import java.io.File

class MockBorrowStringResources : BookBorrowStringResourcesType {

  override val borrowBookCoverUnexpectedException: String
    get() = "borrowBookCoverUnexpectedException"

  override val borrowBookSavingCover: String
    get() = "borrowBookSavingCover"

  override val borrowBookFetchingCover: String
    get() = "borrowBookFetchingCover"

  override val borrowBookUnexpectedException: String
    get() = "borrowBookUnexpectedException"

  override val borrowBookFulfillACSMFailed: String
    get() = "borrowBookFulfillACSMFailed"

  override val borrowBookFulfillACSMGettingDeviceCredentialsNotActivated: String
    get() = "borrowBookFulfillACSMGettingDeviceCredentialsNotActivated"

  override fun borrowBookFulfillACSMConnectorFailed(errorCode: String): String {
    return "borrowBookFulfillACSMConnectorFailed"
  }

  override val borrowBookFulfillACSMConnectorOK: String
    get() = "borrowBookFulfillACSMConnectorOK"

  override val borrowBookFulfillACSMGettingDeviceCredentialsOK: String
    get() = "borrowBookFulfillACSMGettingDeviceCredentialsOK"

  override val borrowBookFulfillACSMGettingDeviceCredentials: String
    get() = "borrowBookFulfillACSMGettingDeviceCredentials"

  override val borrowBookFulfillACSMReadFailed: String
    get() = "borrowBookFulfillACSMReadFailed"

  override val borrowBookFulfillACSMRead: String
    get() = "borrowBookFulfillACSMRead"

  override fun borrowBookUnsupportedAcquisition(type: OPDSAcquisition.Relation): String {
    return "borrowBookUnsupportedAcquisition"
  }

  override fun borrowBookBorrowAvailabilityInappropriate(availability: OPDSAvailabilityType): String {
    return "borrowBookBorrowAvailabilityInappropriate"
  }

  override val borrowBookFulfillDownloadFailed: String
    get() = "borrowBookFulfillDownloadFailed"

  override val borrowBookFulfillCancelled: String
    get() = "borrowBookFulfillCancelled"

  override fun borrowBookBorrowForAvailability(availability: OPDSAvailabilityType): String {
    return "borrowBookBorrowForAvailability"
  }

  override fun borrowBookFulfillDownloaded(file: File, contentType: String): String {
    return "borrowBookFulfillDownloaded"
  }

  override fun borrowBookFulfillACSMCheckContentTypeOK(contentType: String): String {
    return "borrowBookFulfillACSMCheckContentTypeOK"
  }

  override fun borrowBookSaving(receivedContentType: String, expectedContentTypes: Set<String>): String {
    return "borrowBookSaving"
  }

  override fun borrowBookSavingCheckingContentType(receivedContentType: String, expectedContentTypes: Set<String>): String {
    return "borrowBookSavingCheckingContentType"
  }

  override val borrowBookSavingCheckingContentTypeUnacceptable: String
    get() = "borrowBookSavingCheckingContentTypeUnacceptable"

  override val borrowBookSavingCheckingContentTypeOK: String
    get() = "borrowBookSavingCheckingContentTypeOK"

  override val borrowBookFulfillBearerTokenOK: String
    get() = "borrowBookFulfillBearerTokenOK"

  override val borrowBookFulfillBearerToken: String
    get() = "borrowBookFulfillBearerToken"

  override val borrowBookFulfillACSMCheckContentType: String
    get() = "borrowBookFulfillACSMCheckContentType"

  override val borrowBookFulfillACSMUnsupportedContentType: String
    get() = "borrowBookFulfillACSMUnsupportedContentType"

  override val borrowBookFulfillACSMParseFailed: String
    get() = "borrowBookFulfillACSMParseFailed"

  override val borrowBookFulfillACSMParse: String
    get() = "borrowBookFulfillACSMParse"

  override val borrowBookFulfillACSMConnector: String
    get() = "borrowBookFulfillACSMConnector"

  override val borrowBookFulfillUnparseableBearerToken: String
    get() = "borrowBookFulfillUnparseableBearerToken"

  override val borrowBookFulfillDRMNotSupported: String
    get() = "borrowBookFulfillDRMNotSupported"

  override val borrowBookFulfillACSM: String
    get() = "borrowBookFulfillACSM"

  override val borrowBookFulfillTimedOut: String
    get() = "borrowBookFulfillTimedOut"

  override val borrowBookFulfillDownload: String
    get() = "borrowBookFulfillDownload"

  override val borrowBookFulfillNoUsableAcquisitions: String
    get() = "borrowBookFulfillNoUsableAcquisitions"

  override val borrowBookFulfill: String
    get() = "borrowBookFulfill"

  override val borrowBookBundledCopyFailed: String
    get() = "borrowBookBundledCopyFailed"

  override val borrowBookBundledCopy: String
    get() = "borrowBookBundledCopy"

  override val borrowBookFeedLoadingFailed: String
    get() = "borrowBookFeedLoadingFailed"

  override val borrowBookBadBorrowFeed: String
    get() = "borrowBookBadBorrowFeed"

  override val borrowBookGetFeedEntry: String
    get() = "borrowBookGetFeedEntry"

  override val borrowBookDatabaseFailed: String
    get() = "borrowBookDatabaseFailed"

  override val borrowStarted: String
    get() = "borrowStarted"

  override val borrowBookDatabaseCreateOrUpdate: String
    get() = "borrowBookDatabaseCreateOrUpdate"

  override val borrowBookDatabaseUpdated: String
    get() = "borrowBookDatabaseUpdated"
}