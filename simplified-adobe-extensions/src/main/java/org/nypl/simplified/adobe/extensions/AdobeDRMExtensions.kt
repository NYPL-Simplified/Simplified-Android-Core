package org.nypl.simplified.adobe.extensions

import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import org.nypl.drm.core.AdobeAdeptActivationReceiverType
import org.nypl.drm.core.AdobeAdeptConnectorType
import org.nypl.drm.core.AdobeAdeptDeactivationReceiverType
import org.nypl.drm.core.AdobeAdeptExecutorType
import org.nypl.drm.core.AdobeAdeptFulfillmentListenerType
import org.nypl.drm.core.AdobeAdeptLoan
import org.nypl.drm.core.AdobeAdeptLoanReturnListenerType
import org.nypl.drm.core.AdobeDeviceID
import org.nypl.drm.core.AdobeUserID
import org.nypl.drm.core.AdobeVendorID
import org.nypl.simplified.accounts.api.AccountAuthenticationAdobeClientToken
import org.nypl.simplified.accounts.api.AccountAuthenticationAdobePostActivationCredentials
import org.nypl.simplified.files.FileUtilities
import java.io.File
import java.util.concurrent.CancellationException

/**
 * Extensions to make the ancient Adept interface slightly more pleasant to work with.
 */

object AdobeDRMExtensions {

  /**
   * Activate a device.
   *
   * @param executor The Adept executor to be used
   * @param error A function to receive error messages
   * @param debug A function to receive debug messages
   * @param vendorID The vendor ID
   * @param clientToken The Adobe short client token
   */

  fun activateDevice(
    executor: AdobeAdeptExecutorType,
    error: (String) -> Unit,
    debug: (String) -> Unit,
    vendorID: AdobeVendorID,
    clientToken: AccountAuthenticationAdobeClientToken
  ): ListenableFuture<List<AccountAuthenticationAdobePostActivationCredentials>> {
    val future =
      SettableFuture.create<List<AccountAuthenticationAdobePostActivationCredentials>>()

    executor.execute { connector ->
      try {
        val results =
          mutableListOf<AccountAuthenticationAdobePostActivationCredentials>()
        val receiver =
          ActivationReceiver(error, debug, future, results)

        debug("activating device with token")
        connector.activateDevice(
          receiver,
          vendorID,
          clientToken.userName,
          clientToken.password
        )
        debug("activation finished")

        if (!receiver.failed) {
          if (results.isEmpty()) {
            throw AdobeDRMLoginNoActivationsException()
          }
          future.set(results.toList())
        }
      } catch (e: Throwable) {
        future.setException(e)
      }
    }
    return future
  }

  /**
   * The connector raised some sort of error code.
   */

  data class AdobeDRMLoginConnectorException(
    val errorCode: String
  ) : Exception(errorCode)

  /**
   * The connector did not return any activations
   */

  class AdobeDRMLoginNoActivationsException : Exception()

  private class ActivationReceiver(
    val error: (String) -> Unit,
    val debug: (String) -> Unit,
    val future: SettableFuture<List<AccountAuthenticationAdobePostActivationCredentials>>,
    val results: MutableList<AccountAuthenticationAdobePostActivationCredentials>
  ) : AdobeAdeptActivationReceiverType {

    var failed = false

    override fun onActivationError(error: String) {
      this.error("onActivationError: $error")
      this.failed = true
      this.future.setException(AdobeDRMLoginConnectorException(error))
    }

    override fun onActivationsCount(count: Int) {
      this.debug("onActivationsCount: $count")
    }

    override fun onActivation(
      index: Int,
      vendor: AdobeVendorID,
      device: AdobeDeviceID,
      userName: String,
      userId: AdobeUserID,
      expiry: String?
    ) {
      this.debug("onActivation: $index")
      this.results.add(AccountAuthenticationAdobePostActivationCredentials(device, userId))
    }
  }

  /**
   * Deactivate a device.
   *
   * @param executor The Adept executor to be used
   * @param error A function to receive error messages
   * @param debug A function to receive debug messages
   * @param vendorID The vendor ID
   * @param clientToken The Adobe short client token
   */

  fun deactivateDevice(
    executor: AdobeAdeptExecutorType,
    error: (String) -> Unit,
    debug: (String) -> Unit,
    vendorID: AdobeVendorID,
    userID: AdobeUserID,
    clientToken: AccountAuthenticationAdobeClientToken
  ): ListenableFuture<Unit> {
    val adeptFuture = SettableFuture.create<Unit>()
    executor.execute { connector ->
      try {
        val errors = mutableListOf<Exception>()
        val receiver = object : AdobeAdeptDeactivationReceiverType {
          override fun onDeactivationError(error: String) {
            error("onDeactivationError: $error")
            errors.add(AdobeDRMLogoutConnectorException(error))
          }

          override fun onDeactivationSucceeded() {
            debug("onDeactivationSucceeded")
          }
        }

        debug("deactivating device with token")
        connector.deactivateDevice(
          receiver,
          vendorID,
          userID,
          clientToken.userName,
          clientToken.password
        )
        debug("deactivation finished")

        if (errors.isNotEmpty()) {
          adeptFuture.setException(errors[0])
        } else {
          adeptFuture.set(Unit)
        }
      } catch (e: Throwable) {
        adeptFuture.setException(e)
      }
    }
    return adeptFuture
  }

  /**
   * The connector raised some sort of error code.
   */

  data class AdobeDRMLogoutConnectorException(
    val errorCode: String
  ) : Exception(errorCode)

  /**
   * Retrieve activations for a device.
   *
   * @param executor The Adept executor to be used
   * @param error A function to receive error messages
   * @param debug A function to receive debug messages
   */

  fun getDeviceActivations(
    executor: AdobeAdeptExecutorType,
    error: (String) -> Unit,
    debug: (String) -> Unit
  ): ListenableFuture<List<Activation>> {
    val adeptFuture = SettableFuture.create<List<Activation>>()
    executor.execute { connector ->
      try {
        val results =
          mutableListOf<Activation>()
        val rawReceiver =
          ActivationRawReceiver(error, debug, adeptFuture, results)

        debug("retrieving device activations")
        connector.getDeviceActivations(rawReceiver)
        debug("retrieving device activations ended")

        if (!rawReceiver.failed) {
          adeptFuture.set(results.toList())
        }
      } catch (e: Throwable) {
        adeptFuture.setException(e)
      }
    }
    return adeptFuture
  }

  data class Activation(
    val index: Int,
    val vendor: AdobeVendorID,
    val device: AdobeDeviceID,
    val userName: String,
    val userID: AdobeUserID,
    val expiry: String?
  )

  private class ActivationRawReceiver(
    val error: (String) -> Unit,
    val debug: (String) -> Unit,
    val future: SettableFuture<List<Activation>>,
    val results: MutableList<Activation>
  ) : AdobeAdeptActivationReceiverType {

    var failed = false

    override fun onActivationError(error: String) {
      this.error("onActivationError: $error")
      this.failed = true
      this.future.setException(AdobeDRMLoginConnectorException(error))
    }

    override fun onActivationsCount(count: Int) {
      this.debug("onActivationsCount: $count")
    }

    override fun onActivation(
      index: Int,
      vendor: AdobeVendorID,
      device: AdobeDeviceID,
      userName: String,
      userId: AdobeUserID,
      expiry: String?
    ) {
      this.debug("onActivation: $index")
      this.results.add(Activation(index, vendor, device, userName, userId, expiry))
    }
  }

  /**
   * Fulfill a book.
   *
   * @param executor The Adept executor to be used
   * @param onStart A function evaluated when the download starts
   * @param error A function to receive error messages
   * @param progress A function that will receive progress reports for downloads
   * @param debug A function to receive debug messages
   * @param data The raw bytes of an ACSM file
   * @param userId The user ID performing the fulfillment
   */

  fun fulfill(
    executor: AdobeAdeptExecutorType,
    error: (String) -> Unit,
    debug: (String) -> Unit,
    onStart: (AdobeAdeptConnectorType) -> Unit,
    progress: (Double) -> Unit,
    outputFile: File,
    data: ByteArray,
    userId: AdobeUserID
  ): ListenableFuture<Fulfillment> {
    val adeptFuture = SettableFuture.create<Fulfillment>()
    executor.execute { connector ->
      try {
        onStart.invoke(connector)
        val rawReceiver = FulfillmentReceiver(error, debug, progress, outputFile, adeptFuture)
        debug("fulfilling ACSM")
        connector.fulfillACSM(rawReceiver, data, userId)
        debug("fulfillment ended")

        if (!adeptFuture.isDone) {
          adeptFuture.setException(
            IllegalStateException(
              "Fulfillment receiver failed to report success or failure"
            )
          )
        }
      } catch (e: Throwable) {
        adeptFuture.setException(e)
      }
    }
    return adeptFuture
  }

  data class Fulfillment(
    val file: File,
    val loan: AdobeAdeptLoan
  )

  /**
   * The connector raised some sort of error code.
   */

  data class AdobeDRMFulfillmentException(
    val errorCode: String
  ) : Exception(errorCode)

  private class FulfillmentReceiver(
    private val error: (String) -> Unit,
    private val debug: (String) -> Unit,
    private val progress: (Double) -> Unit,
    private val outputFile: File,
    private val adeptFuture: SettableFuture<Fulfillment>
  ) : AdobeAdeptFulfillmentListenerType {

    override fun onFulfillmentCancelled() {
      this.adeptFuture.setException(CancellationException())
    }

    override fun onFulfillmentProgress(percent: Double) {
      try {
        this.progress.invoke(percent * 100.0)
      } catch (e: Throwable) {
        this.error.invoke("suppressed exception: $e")
      }
    }

    override fun onFulfillmentFailure(errorCode: String) {
      this.adeptFuture.setException(AdobeDRMFulfillmentException(errorCode))
    }

    override fun onFulfillmentSuccess(file: File, loan: AdobeAdeptLoan) {
      try {
        FileUtilities.fileCopy(file, this.outputFile)
      } catch (e: Throwable) {
        this.adeptFuture.setException(e)
      }
      this.adeptFuture.set(Fulfillment(this.outputFile, loan))
    }
  }

  /**
   * Revoke a loan.
   *
   * @param executor The Adept executor to be used
   * @param loan The loan
   * @param userId The user ID
   */

  fun revoke(
    executor: AdobeAdeptExecutorType,
    loan: AdobeAdeptLoan,
    userId: AdobeUserID
  ): ListenableFuture<Unit> {
    val adeptFuture = SettableFuture.create<Unit>()
    executor.execute { connector ->
      try {
        connector.loanReturn(RevokeReceiver(adeptFuture), loan.id, userId)

        if (!adeptFuture.isDone) {
          adeptFuture.setException(
            IllegalStateException(
              "Revoke receiver failed to report success or failure"
            )
          )
        }
      } catch (e: Throwable) {
        adeptFuture.setException(e)
      }
    }
    return adeptFuture
  }

  /**
   * The connector raised some sort of error code.
   */

  data class AdobeDRMRevokeException(
    val errorCode: String
  ) : Exception(errorCode)

  private class RevokeReceiver(
    private val adeptFuture: SettableFuture<Unit>
  ) : AdobeAdeptLoanReturnListenerType {

    override fun onLoanReturnSuccess() {
      this.adeptFuture.set(Unit)
    }

    override fun onLoanReturnFailure(errorCode: String) {
      this.adeptFuture.setException(AdobeDRMRevokeException(errorCode))
    }
  }
}
