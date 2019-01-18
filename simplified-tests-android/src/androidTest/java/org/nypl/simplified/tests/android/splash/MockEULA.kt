package org.nypl.simplified.tests.android.splash

import org.nypl.simplified.books.eula.EULAType
import java.net.URL

class MockEULA : EULAType {
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