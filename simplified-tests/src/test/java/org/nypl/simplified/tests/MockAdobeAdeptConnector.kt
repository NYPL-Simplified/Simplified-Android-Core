package org.nypl.simplified.tests

import org.nypl.drm.core.AdobeAdeptActivationReceiverType
import org.nypl.drm.core.AdobeAdeptConnectorType
import org.nypl.drm.core.AdobeAdeptDeactivationReceiverType
import org.nypl.drm.core.AdobeAdeptFulfillmentListenerType
import org.nypl.drm.core.AdobeAdeptJoinAccountListenerType
import org.nypl.drm.core.AdobeAdeptLoanReturnListenerType
import org.nypl.drm.core.AdobeAdeptNetProviderType
import org.nypl.drm.core.AdobeAdeptResourceProviderType
import org.nypl.drm.core.AdobeLoanID
import org.nypl.drm.core.AdobeUserID
import org.nypl.drm.core.AdobeVendorID

class MockAdobeAdeptConnector(
  val netProvider: MockAdobeAdeptNetProvider,
  val resourceProvider: MockAdobeAdeptResourceProvider
) : AdobeAdeptConnectorType {

  var onFulfill: (
    listener: AdobeAdeptFulfillmentListenerType,
    acsm: ByteArray,
    user: AdobeUserID
  ) -> Unit = {
    _, _, _ ->
  }

  override fun activateDeviceToken(
    client: AdobeAdeptActivationReceiverType,
    vendor: AdobeVendorID,
    user_name: String,
    token: String
  ) {
  }

  override fun deactivateDevice(
    client: AdobeAdeptDeactivationReceiverType,
    vendor: AdobeVendorID,
    user: AdobeUserID,
    user_name: String,
    password: String
  ) {
  }

  override fun getDeviceActivations(client: AdobeAdeptActivationReceiverType) {
  }

  override fun joinAccount(
    listener: AdobeAdeptJoinAccountListenerType,
    user: AdobeUserID?
  ) {
  }

  override fun fulfillACSM(
    listener: AdobeAdeptFulfillmentListenerType,
    acsm: ByteArray,
    user: AdobeUserID
  ) {
    this.onFulfill.invoke(listener, acsm, user)
  }

  override fun getNetProvider(): AdobeAdeptNetProviderType {
    return this.netProvider
  }

  override fun discardDeviceActivations() {
  }

  override fun getResourceProvider(): AdobeAdeptResourceProviderType {
    return this.resourceProvider
  }

  override fun loanReturn(
    listener: AdobeAdeptLoanReturnListenerType,
    loan_id: AdobeLoanID,
    user: AdobeUserID
  ) {
  }

  override fun activateDevice(
    client: AdobeAdeptActivationReceiverType,
    vendor: AdobeVendorID,
    user_name: String,
    password: String
  ) {
  }
}
