package org.nypl.simplified.tests.mocking

import org.nypl.drm.core.AdobeAdeptNetProviderType
import org.nypl.drm.core.AdobeAdeptStreamClientType
import org.nypl.drm.core.AdobeAdeptStreamType

class MockAdobeAdeptNetProvider : AdobeAdeptNetProviderType {

  override fun newStream(
    method: String,
    url: String,
    client: AdobeAdeptStreamClientType,
    post_data_content_type: String,
    post_data: ByteArray
  ): AdobeAdeptStreamType {
    TODO()
  }

  override fun cancel() {
  }
}
