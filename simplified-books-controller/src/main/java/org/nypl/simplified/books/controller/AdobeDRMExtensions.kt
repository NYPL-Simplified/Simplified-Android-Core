package org.nypl.simplified.books.controller

import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import org.nypl.drm.core.AdobeAdeptActivationReceiverType
import org.nypl.drm.core.AdobeAdeptDeactivationReceiverType
import org.nypl.drm.core.AdobeAdeptExecutorType
import org.nypl.drm.core.AdobeDeviceID
import org.nypl.drm.core.AdobeUserID
import org.nypl.drm.core.AdobeVendorID
import org.nypl.simplified.accounts.api.AccountAuthenticationAdobeClientToken
import org.nypl.simplified.accounts.api.AccountAuthenticationAdobePostActivationCredentials

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
    clientToken: AccountAuthenticationAdobeClientToken)
    : ListenableFuture<List<AccountAuthenticationAdobePostActivationCredentials>> {

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
          clientToken.tokenUserName(),
          clientToken.tokenPassword())

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

  class AdobeDRMLoginConnectorException(
    val errorCode: String)
    : Exception(errorCode)

  /**
   * The connector did not return any activations
   */

  class AdobeDRMLoginNoActivationsException
    : Exception()

  private class ActivationReceiver(
    val error: (String) -> Unit,
    val debug: (String) -> Unit,
    val future: SettableFuture<List<AccountAuthenticationAdobePostActivationCredentials>>,
    val results: MutableList<AccountAuthenticationAdobePostActivationCredentials>)
    : AdobeAdeptActivationReceiverType {

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
      expiry: String?) {
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
    clientToken: AccountAuthenticationAdobeClientToken)
    : ListenableFuture<Unit> {

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
          clientToken.tokenUserName(),
          clientToken.tokenPassword())

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

  class AdobeDRMLogoutConnectorException(
    val errorCode: String)
    : Exception(errorCode)

  /**
   * Deactivate a device.
   *
   * @param executor The Adept executor to be used
   * @param error A function to receive error messages
   * @param debug A function to receive debug messages
   * @param vendorID The vendor ID
   * @param clientToken The Adobe short client token
   */

  fun getDeviceActivations(
    executor: AdobeAdeptExecutorType,
    error: (String) -> Unit,
    debug: (String) -> Unit)
    : ListenableFuture<List<Activation>> {

    val adeptFuture = SettableFuture.create<List<Activation>>()
    executor.execute { connector ->
      try {
        val results =
          mutableListOf<Activation>()
        val rawReceiver =
          ActivationRawReceiver(error, debug, adeptFuture, results)

        debug("retrieving device activations")
        connector.getDeviceActivations(rawReceiver)

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
    val expiry: String?)

  private class ActivationRawReceiver(
    val error: (String) -> Unit,
    val debug: (String) -> Unit,
    val future: SettableFuture<List<Activation>>,
    val results: MutableList<Activation>)
    : AdobeAdeptActivationReceiverType {

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
      expiry: String?) {
      this.debug("onActivation: $index")
      this.results.add(Activation(index, vendor, device, userName, userId, expiry))
    }
  }
}