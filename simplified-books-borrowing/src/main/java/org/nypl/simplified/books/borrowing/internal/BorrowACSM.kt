package org.nypl.simplified.books.borrowing.internal

import com.io7m.junreachable.UnreachableCodeException
import one.irradia.mime.api.MIMECompatibility
import one.irradia.mime.api.MIMEType
import org.librarysimplified.http.downloads.LSHTTPDownloadState.LSHTTPDownloadResult.DownloadCancelled
import org.librarysimplified.http.downloads.LSHTTPDownloadState.LSHTTPDownloadResult.DownloadCompletedSuccessfully
import org.librarysimplified.http.downloads.LSHTTPDownloadState.LSHTTPDownloadResult.DownloadFailed.DownloadFailedExceptionally
import org.librarysimplified.http.downloads.LSHTTPDownloadState.LSHTTPDownloadResult.DownloadFailed.DownloadFailedServer
import org.librarysimplified.http.downloads.LSHTTPDownloadState.LSHTTPDownloadResult.DownloadFailed.DownloadFailedUnacceptableMIME
import org.librarysimplified.http.downloads.LSHTTPDownloads
import org.nypl.drm.core.AdobeAdeptFulfillmentToken
import org.nypl.drm.core.AdobeAdeptNetProviderType
import org.nypl.simplified.accounts.api.AccountAuthenticationAdobePostActivationCredentials
import org.nypl.simplified.accounts.api.AccountAuthenticationAdobePreActivationCredentials
import org.nypl.simplified.adobe.extensions.AdobeDRMExtensions
import org.nypl.simplified.adobe.extensions.AdobeDRMExtensions.AdobeDRMFulfillmentException
import org.nypl.simplified.books.api.BookDRMKind.ACS
import org.nypl.simplified.books.book_database.api.BookDRMInformationHandle.ACSHandle
import org.nypl.simplified.books.book_database.api.BookDRMInformationHandle.LCPHandle
import org.nypl.simplified.books.book_database.api.BookDRMInformationHandle.NoneHandle
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleAudioBook
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleEPUB
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandlePDF
import org.nypl.simplified.books.borrowing.BorrowContextType
import org.nypl.simplified.books.borrowing.internal.BorrowErrorCodes.accountCredentialsRequired
import org.nypl.simplified.books.borrowing.internal.BorrowErrorCodes.acsNoCredentialsPost
import org.nypl.simplified.books.borrowing.internal.BorrowErrorCodes.acsNoCredentialsPre
import org.nypl.simplified.books.borrowing.internal.BorrowErrorCodes.acsNotSupported
import org.nypl.simplified.books.borrowing.internal.BorrowErrorCodes.acsTimedOut
import org.nypl.simplified.books.borrowing.internal.BorrowErrorCodes.acsUnparseableACSM
import org.nypl.simplified.books.borrowing.internal.BorrowErrorCodes.noFormatHandle
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskException.BorrowSubtaskCancelled
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskException.BorrowSubtaskFailed
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskException.BorrowSubtaskHaltedEarly
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskFactoryType
import org.nypl.simplified.books.borrowing.subtasks.BorrowSubtaskType
import org.nypl.simplified.books.formats.api.StandardFormatNames.adobeACSMFiles
import java.io.File
import java.net.URI
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeoutException

/**
 * A task that downloads an ACSM file and then fulfills a book using it and the Adobe DRM
 * API.
 */

class BorrowACSM private constructor() : BorrowSubtaskType {

  companion object : BorrowSubtaskFactoryType {
    override val name: String
      get() = "ACSM Download"

    override fun createSubtask(): BorrowSubtaskType {
      return BorrowACSM()
    }

    override fun isApplicableFor(
      type: MIMEType,
      target: URI?
    ): Boolean {
      return MIMECompatibility.isCompatibleStrictWithoutAttributes(type, adobeACSMFiles)
    }
  }

  private data class ProcessedACSM(
    val acsmBytes: ByteArray,
    val acsmToken: AdobeAdeptFulfillmentToken,
    val acsmFile: File
  )

  private data class RequiredCredentials(
    val preActivation: AccountAuthenticationAdobePreActivationCredentials,
    val postActivation: AccountAuthenticationAdobePostActivationCredentials
  )

  override fun execute(context: BorrowContextType) {
    try {
      context.bookDownloadIsRunning(100L, 0L, 1L, "Downloading...")

      this.checkDRMSupport(context)
      val credentials = this.checkRequiredCredentials(context)
      context.taskRecorder.beginNewStep("Downloading ACSM file...")

      val currentURI = context.currentURICheck()
      context.logDebug("downloading {}", currentURI)
      context.taskRecorder.beginNewStep("Downloading $currentURI...")
      context.taskRecorder.addAttribute("URI", currentURI.toString())
      context.checkCancelled()

      val temporaryFile = context.temporaryFile()

      try {
        val downloadRequest =
          BorrowHTTP.createDownloadRequest(
            context = context,
            target = currentURI,
            outputFile = temporaryFile
          )

        when (val result = LSHTTPDownloads.download(downloadRequest)) {
          DownloadCancelled ->
            throw BorrowSubtaskCancelled()
          is DownloadFailedServer ->
            throw BorrowHTTP.onDownloadFailedServer(context, result)
          is DownloadFailedUnacceptableMIME ->
            throw BorrowSubtaskFailed()
          is DownloadFailedExceptionally ->
            throw BorrowHTTP.onDownloadFailedExceptionally(context, result)
          is DownloadCompletedSuccessfully -> {
            this.fulfillACSMFile(
              context = context,
              credentials = credentials,
              acsm = this.processACSMFile(context, temporaryFile)
            )
          }
        }
      } finally {
        temporaryFile.delete()
      }
    } catch (e: BorrowSubtaskFailed) {
      context.bookDownloadFailed()
      throw e
    }
  }

  /**
   * Check that we have a fully activated ACS device.
   */

  private fun checkRequiredCredentials(
    context: BorrowContextType
  ): RequiredCredentials {
    context.taskRecorder.beginNewStep("Checking for Adobe ACS credentials...")

    val credentials = context.account.loginState.credentials
    if (credentials == null) {
      context.taskRecorder.currentStepFailed(
        message = "The account has no credentials.",
        errorCode = accountCredentialsRequired
      )
      throw BorrowSubtaskFailed()
    }

    val preActivation = credentials.adobeCredentials
    if (preActivation == null) {
      context.taskRecorder.currentStepFailed(
        message = "The account has no pre-activation ACS credentials.",
        errorCode = acsNoCredentialsPre
      )
      throw BorrowSubtaskFailed()
    }

    val postActivation = preActivation.postActivationCredentials
    if (postActivation == null) {
      context.taskRecorder.currentStepFailed(
        message = "The account's ACS device is not activated.",
        errorCode = acsNoCredentialsPost
      )
      throw BorrowSubtaskFailed()
    }

    context.taskRecorder.beginNewStep("ACS device is activated.")
    return RequiredCredentials(preActivation, postActivation)
  }

  /**
   * Check that we actually have the required DRM support.
   */

  private fun checkDRMSupport(
    context: BorrowContextType
  ) {
    context.taskRecorder.beginNewStep("Checking for Adobe ACS support...")
    if (context.adobeExecutor == null) {
      context.taskRecorder.currentStepFailed(
        message = "This build of the application does not support Adobe ACS.",
        errorCode = acsNotSupported
      )
      throw BorrowSubtaskFailed()
    }
  }

  /**
   * Process an ACSM file. This means parsing it and saving it.
   */

  private fun processACSMFile(
    context: BorrowContextType,
    temporaryFile: File
  ): ProcessedACSM {
    context.taskRecorder.beginNewStep("Reading ACSM file...")
    val bytes = temporaryFile.readBytes()
    val acsm = this.parseACSMFile(context, bytes)
    return this.saveACSMFile(
      context = context,
      acsmBytes = bytes,
      acsmToken = acsm,
      temporaryFile = temporaryFile
    )
  }

  /**
   * Save an ACSM file to the database.
   */

  private fun saveACSMFile(
    context: BorrowContextType,
    acsmBytes: ByteArray,
    acsmToken: AdobeAdeptFulfillmentToken,
    temporaryFile: File
  ): ProcessedACSM {
    context.taskRecorder.beginNewStep("Saving ACSM file...")

    val formatHandle = this.findFormatHandle(context)

    /*
     * Set the book's DRM kind to ACS, and save the ACSM file.
     */

    formatHandle.setDRMKind(ACS)
    return when (val drmHandle = formatHandle.drmInformationHandle) {
      is ACSHandle -> {
        drmHandle.setACSMFile(temporaryFile)
        context.taskRecorder.currentStepSucceeded("Saved ACSM file.")
        ProcessedACSM(
          acsmBytes = acsmBytes,
          acsmToken = acsmToken,
          acsmFile = drmHandle.info.acsmFile!!
        )
      }
      is LCPHandle,
      is NoneHandle ->
        throw UnreachableCodeException()
    }
  }

  /**
   * Parse a series of bytes that are expected to comprise an ACSM file.
   */

  private fun parseACSMFile(
    context: BorrowContextType,
    acsmBytes: ByteArray
  ): AdobeAdeptFulfillmentToken {
    context.taskRecorder.beginNewStep("Parsing ACSM file...")
    return try {
      AdobeAdeptFulfillmentToken.parseFromBytes(acsmBytes)
    } catch (e: Exception) {
      context.taskRecorder.currentStepFailed(
        message = "Unparseable ACSM file: ${e.message}",
        errorCode = acsUnparseableACSM,
        exception = e
      )
      throw BorrowSubtaskFailed()
    }
  }

  /**
   * Use an ACSM file to download a book.
   */

  private fun fulfillACSMFile(
    context: BorrowContextType,
    credentials: RequiredCredentials,
    acsm: ProcessedACSM
  ) {
    context.taskRecorder.beginNewStep("Fulfilling ACSM file...")

    val temporaryFile = context.temporaryFile()
    try {
      val executor = context.adobeExecutor!!
      var netProvider: AdobeAdeptNetProviderType? = null
      val unitsPerSecond = BorrowUnitsPerSecond(context.clock)

      val future =
        AdobeDRMExtensions.fulfill(
          executor = executor,
          error = { message ->
            this.onAdobeError(context, message)
          },
          debug = { message ->
            this.onAdobeDebug(context, message)
          },
          onStart = {
            netProvider = it.netProvider

            context.bookDownloadIsRunning(
              expectedSize = 100L,
              receivedSize = 0L,
              bytesPerSecond = 1L,
              message = BorrowHTTP.downloadingMessage(
                expectedSize = 100,
                currentSize = 0L,
                perSecond = 1L
              )
            )
          },
          progress = { progress ->
            if (context.isCancelled) {
              netProvider?.cancel()
            }

            if (unitsPerSecond.update(progress.toLong())) {
              context.bookDownloadIsRunning(
                expectedSize = 100L,
                receivedSize = progress.toLong(),
                bytesPerSecond = 1L,
                message = BorrowHTTP.downloadingMessage(
                  expectedSize = 100,
                  currentSize = progress.toLong(),
                  perSecond = 1L
                )
              )
            }
          },
          outputFile = temporaryFile,
          data = acsm.acsmBytes,
          userId = credentials.postActivation.userID
        )

      val fulfillment = try {
        future.get(context.adobeExecutorTimeout.time, context.adobeExecutorTimeout.timeUnit)
      } catch (e: TimeoutException) {
        context.taskRecorder.currentStepFailed(
          message = "Adobe ACS fulfillment timed out.",
          errorCode = acsTimedOut,
          exception = e
        )
        throw BorrowSubtaskFailed()
      } catch (e: ExecutionException) {
        throw when (val cause = e.cause!!) {
          is CancellationException -> {
            BorrowSubtaskCancelled()
          }
          is AdobeDRMFulfillmentException -> {
            context.taskRecorder.currentStepFailed(
              message = "Adobe ACS fulfillment failed (${cause.errorCode})",
              errorCode = "ACS: ${cause.errorCode}",
              exception = cause
            )
            BorrowSubtaskFailed()
          }
          else -> {
            context.taskRecorder.currentStepFailed(
              message = "Adobe ACS fulfillment failed (${cause.javaClass})",
              errorCode = "ACS: ${cause.javaClass}",
              exception = cause
            )
            BorrowSubtaskFailed()
          }
        }
      }

      this.saveFulfilledBook(context, fulfillment)

      /*
       * Adobe ACS is a special case in the sense that it supersedes any acquisition
       * path elements that might follow this one. We mark this subtask as having halted
       * early.
       */

      throw BorrowSubtaskHaltedEarly()
    } finally {
      temporaryFile.delete()
    }
  }

  /**
   * Save the fulfilled book to the database.
   */

  private fun saveFulfilledBook(
    context: BorrowContextType,
    fulfillment: AdobeDRMExtensions.Fulfillment
  ) {
    context.taskRecorder.beginNewStep("Saving fulfilled book...")
    val formatHandle = this.findFormatHandle(context)

    check(formatHandle.drmInformationHandle.info.kind == ACS) {
      "DRM information handle must be set to ACS!"
    }

    return when (val drmHandle = formatHandle.drmInformationHandle) {
      is ACSHandle -> {
        drmHandle.setAdobeRightsInformation(fulfillment.loan)
        when (formatHandle) {
          is BookDatabaseEntryFormatHandleEPUB -> {
            formatHandle.copyInBook(fulfillment.file)
            context.taskRecorder.currentStepSucceeded("Saved book.")
            context.bookDownloadSucceeded()
          }
          is BookDatabaseEntryFormatHandlePDF,
          is BookDatabaseEntryFormatHandleAudioBook ->
            throw UnreachableCodeException()
        }
      }
      is LCPHandle,
      is NoneHandle ->
        throw UnreachableCodeException()
    }
  }

  /**
   * Determine the actual book format we're aiming for at the end of the acquisition path.
   */

  private fun findFormatHandle(
    context: BorrowContextType
  ): BookDatabaseEntryFormatHandle {
    val eventualType = context.opdsAcquisitionPath.asMIMETypes().last()
    val formatHandle = context.bookDatabaseEntry.findFormatHandleForContentType(eventualType)
    if (formatHandle == null) {
      context.taskRecorder.currentStepFailed(
        message = "No format handle available for ${eventualType.fullType}",
        errorCode = noFormatHandle
      )
      throw BorrowSubtaskFailed()
    }
    return formatHandle
  }

  private fun onAdobeDebug(
    context: BorrowContextType,
    message: String
  ) {
    context.logDebug("{}", message)
  }

  private fun onAdobeError(
    context: BorrowContextType,
    message: String
  ) {
    context.logError("{}", message)
  }
}
