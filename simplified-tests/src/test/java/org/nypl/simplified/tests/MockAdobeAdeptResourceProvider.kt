package org.nypl.simplified.tests

import org.nypl.drm.core.AdobeAdeptResourceProviderType

class MockAdobeAdeptResourceProvider : AdobeAdeptResourceProviderType {
  override fun getResourceAsBytes(name: String): ByteArray {
    TODO()
  }
}
