package org.nypl.simplified.tests.mocking

import org.nypl.drm.core.AdobeAdeptResourceProviderType

class MockAdobeAdeptResourceProvider : AdobeAdeptResourceProviderType {
  override fun getResourceAsBytes(name: String): ByteArray {
    TODO()
  }
}
