package org.nypl.simplified.tests.android.splash

import java.net.URL

class MockEULA : org.nypl.simplified.documents.eula.EULAType {
  var agreed = false
  var url : URL? = URL("http://example.com")

  override fun eulaHasAgreed(): Boolean {
    return this.agreed
  }

  override fun documentSetLatestURL(u: URL) {
    this.url = u
  }

  override fun documentGetReadableURL(): URL {
    return this.url!!
  }

  override fun eulaSetHasAgreed(t: Boolean) {
    this.agreed = t
  }
}