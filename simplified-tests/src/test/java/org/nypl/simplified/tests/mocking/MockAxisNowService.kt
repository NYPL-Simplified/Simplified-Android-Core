package org.nypl.simplified.tests.mocking

import org.nypl.drm.core.AxisNowFulfillment
import org.nypl.drm.core.AxisNowServiceType
import java.io.File

class MockAxisNowService : AxisNowServiceType {

  lateinit var onFulfill: (
    token: ByteArray,
    tempFactory: () -> File
  ) -> AxisNowFulfillment

  override fun fulfill(token: ByteArray, tempFactory: () -> File): AxisNowFulfillment {
    return onFulfill(token, tempFactory)
  }
}
