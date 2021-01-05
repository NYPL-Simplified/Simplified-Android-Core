package org.nypl.simplified.books.borrowing.internal

import com.io7m.junreachable.UnreachableCodeException
import one.irradia.mime.api.MIMECompatibility
import one.irradia.mime.api.MIMEType
import org.librarysimplified.http.api.LSHTTPAuthorizationBasic
import org.librarysimplified.http.api.LSHTTPAuthorizationBearerToken
import org.librarysimplified.http.api.LSHTTPAuthorizationType
import org.librarysimplified.http.api.LSHTTPRequestBuilderType.AllowRedirects.ALLOW_UNSAFE_REDIRECTS
import org.librarysimplified.http.api.LSHTTPRequestProperties
import org.librarysimplified.http.downloads.LSHTTPDownloadRequest
import org.librarysimplified.http.downloads.LSHTTPDownloadState
import org.librarysimplified.http.downloads.LSHTTPDownloadState.DownloadReceiving
import org.librarysimplified.http.downloads.LSHTTPDownloadState.DownloadStarted
import org.librarysimplified.http.downloads.LSHTTPDownloadState.LSHTTPDownloadResult.DownloadCancelled
import org.librarysimplified.http.downloads.LSHTTPDownloadState.LSHTTPDownloadResult.DownloadCompletedSuccessfully
import org.librarysimplified.http.downloads.LSHTTPDownloadState.LSHTTPDownloadResult.DownloadFailed.DownloadFailedExceptionally
import org.librarysimplified.http.downloads.LSHTTPDownloadState.LSHTTPDownloadResult.DownloadFailed.DownloadFailedServer
import org.librarysimplified.http.downloads.LSHTTPDownloadState.LSHTTPDownloadResult.DownloadFailed.DownloadFailedUnacceptableMIME
import org.librarysimplified.http.downloads.LSHTTPDownloads
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoggedIn
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoggingIn
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoggingInWaitingForExternalAuthentication
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoggingOut
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLoginFailed
import org.nypl.simplified.accounts.api.AccountLoginState.AccountLogoutFailed
import org.nypl.simplified.accounts.api.AccountLoginState.AccountNotLoggedIn
import org.nypl.simplified.accounts.api.AccountReadableType
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle
import org.nypl.simplified.books.borrowing.BorrowContextType
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskException
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskException.BorrowSubtaskFailed
import java.io.File
import java.net.URI

/**
 * Convenience functions over HTTP.
 */

object BorrowHTTP {

  /**
   * Create a download request for the given URI, downloading content to the given output file.
   * Events will be delivered to the given borrow context.
   */

  fun createDownloadRequest(
    context: BorrowContextType,
    target: URI,
    outputFile: File,
    requestModifier: ((LSHTTPRequestProperties) -> LSHTTPRequestProperties)? = null
  ): LSHTTPDownloadRequest {
    val request =
      context.httpClient.newRequest(target)
        .setAuthorization(authorizationOf(context.account))
        .allowRedirects(ALLOW_UNSAFE_REDIRECTS)
        .apply {
          if (requestModifier != null) {
            setRequestModifier(requestModifier)
          }
        }
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
          is AccountAuthenticationCredentials.SAML2_0 ->
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
    context.taskRecorder.addAttributes(status.properties.problemReport?.toMap() ?: emptyMap())
    context.taskRecorder.currentStepFailed(
      message = "HTTP request failed: ${status.properties.originalStatus} ${status.properties.message}",
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

  /**
   * A default handler for DownloadFailedUnacceptableMIME that just throws BorrowSubtaskFailed.
   */

  fun onDownloadFailedUnacceptableMimeDefault(
    context: BorrowContextType,
    result: DownloadFailedUnacceptableMIME
  ) {
    throw BorrowSubtaskFailed()
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

  /**
   * Download the file indicated by the given borrowing context.
   *
   * @param context The borrowing context.
   * @param onDownloadFailedUnacceptableMIME A handler to be called if the downloaded file has a
   * content type that is not acceptable according to the borrowing context. If not provided, a
   * BorrowSubtaskFailed exception is thrown.
   */

  fun download(
    context: BorrowContextType,
    onDownloadFailedUnacceptableMIME:
      (BorrowContextType, DownloadFailedUnacceptableMIME) -> Unit =
        this::onDownloadFailedUnacceptableMimeDefault,
    requestModifier:
      ((LSHTTPRequestProperties) -> LSHTTPRequestProperties)? = null
  ) {
    return try {
      val currentURI = context.currentURICheck()
      context.logDebug("downloading {}", currentURI)
      context.taskRecorder.beginNewStep("Downloading $currentURI...")
      context.taskRecorder.addAttribute("URI", currentURI.toString())

      val temporaryFile = context.temporaryFile()

      try {
        val downloadRequest =
          createDownloadRequest(
            context = context,
            target = currentURI,
            outputFile = temporaryFile,
            requestModifier = requestModifier
          )

        when (val result = LSHTTPDownloads.download(downloadRequest)) {
          DownloadCancelled ->
            throw BorrowSubtaskException.BorrowSubtaskCancelled()
          is DownloadFailedServer ->
            throw onDownloadFailedServer(context, result)
          is DownloadFailedUnacceptableMIME ->
            onDownloadFailedUnacceptableMIME(context, result)
          is DownloadFailedExceptionally ->
            throw onDownloadFailedExceptionally(context, result)
          is DownloadCompletedSuccessfully ->
            this.saveDownloadedContent(context, temporaryFile)
        }
      } finally {
        temporaryFile.delete()
      }
    } catch (e: BorrowSubtaskFailed) {
      context.bookDownloadFailed()
      throw e
    }
  }

  private fun saveDownloadedContent(
    context: BorrowContextType,
    temporaryFile: File
  ) {
    context.taskRecorder.beginNewStep("Saving book...")

    val formatHandle =
      context.bookDatabaseEntry.findFormatHandleForContentType(
        context.currentAcquisitionPathElement.mimeType
      )

    return when (formatHandle) {
      is BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleEPUB -> {
        formatHandle.copyInBook(temporaryFile)
        context.bookDownloadSucceeded()
      }
      is BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandlePDF -> {
        formatHandle.copyInBook(temporaryFile)
        context.bookDownloadSucceeded()
      }
      is BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleAudioBook,
      null ->
        throw UnreachableCodeException()
    }
  }
}
