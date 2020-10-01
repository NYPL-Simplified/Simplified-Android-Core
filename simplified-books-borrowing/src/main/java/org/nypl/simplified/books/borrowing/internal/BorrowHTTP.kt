package org.nypl.simplified.books.borrowing.internal

import one.irradia.mime.api.MIMECompatibility
import one.irradia.mime.api.MIMEType
import org.librarysimplified.http.api.LSHTTPAuthorizationBasic
import org.librarysimplified.http.api.LSHTTPAuthorizationBearerToken
import org.librarysimplified.http.api.LSHTTPAuthorizationType
import org.librarysimplified.http.api.LSHTTPProblemReport
import org.librarysimplified.http.downloads.LSHTTPDownloadRequest
import org.librarysimplified.http.downloads.LSHTTPDownloadState
import org.librarysimplified.http.downloads.LSHTTPDownloadState.DownloadReceiving
import org.librarysimplified.http.downloads.LSHTTPDownloadState.DownloadStarted
import org.librarysimplified.http.downloads.LSHTTPDownloadState.LSHTTPDownloadResult.DownloadCancelled
import org.librarysimplified.http.downloads.LSHTTPDownloadState.LSHTTPDownloadResult.DownloadCompletedSuccessfully
import org.librarysimplified.http.downloads.LSHTTPDownloadState.LSHTTPDownloadResult.DownloadFailed.DownloadFailedExceptionally
import org.librarysimplified.http.downloads.LSHTTPDownloadState.LSHTTPDownloadResult.DownloadFailed.DownloadFailedServer
import org.librarysimplified.http.downloads.LSHTTPDownloadState.LSHTTPDownloadResult.DownloadFailed.DownloadFailedUnacceptableMIME
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoggedIn
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoggingIn
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoggingInWaitingForExternalAuthentication
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoggingOut
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoginFailed
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLogoutFailed
import org.nypl.simplified.accounts.api.AccountLoginState.AccountNotLoggedIn
import org.nypl.simplified.accounts.api.AccountReadableType
import org.nypl.simplified.books.borrowing.BorrowContextType
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskException.BorrowSubtaskFailed
import java.io.File
import java.net.URI

/**
 * Convenience functions over HTTP.
 */

object BorrowHTTP {

  /**
   * Encode the given problem report as a set of presentable attributes.
   */

  fun problemReportAsAttributes(
    problemReport: LSHTTPProblemReport?
  ): Map<String, String> {
    return when (problemReport) {
      null -> mapOf()
      else -> {
        val attributes = mutableMapOf<String, String>()
        attributes["HTTP problem detail"] = problemReport.detail ?: ""
        attributes["HTTP problem status"] = problemReport.status.toString()
        attributes["HTTP problem title"] = problemReport.title ?: ""
        attributes["HTTP problem type"] = problemReport.type.toString()
        attributes.toMap()
      }
    }
  }

  /**
   * Create a download request for the given URI, downloading content to the given output file.
   * Events will be delivered to the given borrow context.
   */

  fun createDownloadRequest(
    context: BorrowContextType,
    target: URI,
    outputFile: File
  ): LSHTTPDownloadRequest {
    val request =
      context.httpClient.newRequest(target)
        .setAuthorization(authorizationOf(context.account))
        .build()

    return LSHTTPDownloadRequest(
      request = request,
      outputFile = outputFile,
      onEvent = {
        this.onDownloadProgressEvent(context, it)
      },
      isMIMETypeAcceptable = {
        this.isMimeTypeAcceptable(context, it)
      },
      isCancelled = {
        context.isCancelled
      },
      clock = context.clock
    )
  }

  /**
   * Create HTTP authorization values for the given account.
   */

  fun authorizationOf(
    account: AccountReadableType
  ): LSHTTPAuthorizationType? {
    return when (val state = account.loginState) {
      is AccountLoggedIn -> {
        when (val creds = state.credentials) {
          is AccountAuthenticationCredentials.Basic ->
            LSHTTPAuthorizationBasic.ofUsernamePassword(
              userName = creds.userName.value,
              password = creds.password.value
            )
          is AccountAuthenticationCredentials.OAuthWithIntermediary ->
            LSHTTPAuthorizationBearerToken.ofToken(creds.accessToken)
        }
      }
      AccountNotLoggedIn,
      is AccountLoggingIn,
      is AccountLoggingInWaitingForExternalAuthentication,
      is AccountLoginFailed,
      is AccountLoggingOut,
      is AccountLogoutFailed ->
        null
    }
  }

  /**
   * Check to see if the given MIME type is "acceptable" according to the current borrowing
   * context. If the type is not acceptable, the current task recorder step will be marked as
   * failed with an appropriate error message.
   *
   * @return `true` if the received MIME type is acceptable
   */

  fun isMimeTypeAcceptable(
    context: BorrowContextType,
    receivedType: MIMEType
  ): Boolean {
    val expectedType = context.currentAcquisitionPathElement.mimeType
    return if (MIMECompatibility.isCompatibleLax(receivedType, expectedType)) {
      true
    } else {
      context.taskRecorder.currentStepFailed(
        message = "The server returned an incompatible context type: We wanted something compatible with ${expectedType.fullType} but received ${receivedType.fullType}.",
        errorCode = BorrowErrorCodes.httpContentTypeIncompatible
      )
      false
    }
  }

  /**
   * Record a server error to the task recorder.
   */

  fun onDownloadFailedServer(
    context: BorrowContextType,
    result: DownloadFailedServer
  ): BorrowSubtaskFailed {
    val status = result.responseStatus
    context.taskRecorder.addAttributes(BorrowHTTP.problemReportAsAttributes(status.problemReport))
    context.taskRecorder.currentStepFailed(
      message = "HTTP request failed: ${status.originalStatus} ${status.message}",
      errorCode = BorrowErrorCodes.httpRequestFailed,
      exception = null
    )
    return BorrowSubtaskFailed()
  }

  /**
   * Record a download exception to the task recorder.
   */

  fun onDownloadFailedExceptionally(
    context: BorrowContextType,
    result: DownloadFailedExceptionally
  ): BorrowSubtaskFailed {
    context.taskRecorder.currentStepFailed(
      message = result.exception.message ?: "Exception raised during connection attempt.",
      errorCode = BorrowErrorCodes.httpConnectionFailed,
      exception = result.exception
    )
    return BorrowSubtaskFailed()
  }

  private fun onDownloadProgressEvent(
    context: BorrowContextType,
    event: LSHTTPDownloadState
  ) {
    when (event) {
      is DownloadReceiving -> {
        context.bookDownloadIsRunning(
          expectedSize = event.expectedSize,
          receivedSize = event.receivedSize,
          bytesPerSecond = event.bytesPerSecond,
          message = this.downloadingMessage(
            expectedSize = event.expectedSize,
            currentSize = event.receivedSize,
            perSecond = event.bytesPerSecond
          )
        )
      }

      DownloadStarted,
      DownloadCancelled,
      is DownloadFailedServer,
      is DownloadFailedUnacceptableMIME,
      is DownloadFailedExceptionally,
      is DownloadCompletedSuccessfully -> {
        // Don't care
      }
    }
  }

  fun downloadingMessage(
    expectedSize: Long?,
    currentSize: Long,
    perSecond: Long
  ): String {
    return if (expectedSize == null) {
      "Downloading..."
    } else {
      "Downloading $currentSize / $expectedSize ($perSecond)..."
    }
  }
}
