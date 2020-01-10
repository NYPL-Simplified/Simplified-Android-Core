package org.nypl.simplified.tests

import org.nypl.simplified.documents.eula.EULAType
import org.slf4j.LoggerFactory
import java.net.URL

class MockEULA : EULAType {

  private val logger = LoggerFactory.getLogger(MockEULA::class.java)

  @Volatile
  private var agreed: Boolean = false

  override fun eulaHasAgreed(): Boolean {
    return this.agreed
  }

  override fun documentSetLatestURL(u: URL?) {
  }

  override fun documentGetReadableURL(): URL {
    return URL("http://www.example.com/eula.txt")
  }

  override fun eulaSetHasAgreed(t: Boolean) {
    this.logger.debug("setHasAgreed: {} -> {}", this.agreed, t)
    this.agreed = t
  }
}
