package org.nypl.simplified.tests

import org.nypl.drm.core.AxisNowFulfillment
import org.nypl.drm.core.AxisNowServiceType
import java.io.File

class MockAxisNowService : AxisNowServiceType {
  override fun fulfill(token: ByteArray, tempFactory: () -> File): AxisNowFulfillment {
    TODO()
  }
}
