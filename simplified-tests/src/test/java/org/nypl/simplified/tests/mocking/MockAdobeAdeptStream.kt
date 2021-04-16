package org.nypl.simplified.tests.mocking

import org.nypl.drm.core.AdobeAdeptStreamClientType
import org.nypl.drm.core.AdobeAdeptStreamType

class MockAdobeAdeptStream : AdobeAdeptStreamType {
  override fun onRelease() {
  }

  override fun onSetStreamClient(c: AdobeAdeptStreamClientType) {
  }

  override fun onRequestInfo() {
  }

  override fun onError(message: String) {
  }

  override fun onRequestBytes(
    offset: Long,
    size: Long
  ) {
  }
}
