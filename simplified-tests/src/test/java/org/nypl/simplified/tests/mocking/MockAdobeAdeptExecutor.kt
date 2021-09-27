package org.nypl.simplified.tests.mocking

import org.nypl.drm.core.AdobeAdeptExecutorType
import org.nypl.drm.core.AdobeAdeptProcedureType
import java.util.concurrent.ExecutorService

class MockAdobeAdeptExecutor(
  val executor: ExecutorService,
  val connector: MockAdobeAdeptConnector
) : AdobeAdeptExecutorType {
  override fun execute(p: AdobeAdeptProcedureType) {
    this.executor.execute {
      p.executeWith(this.connector)
    }
  }
}
