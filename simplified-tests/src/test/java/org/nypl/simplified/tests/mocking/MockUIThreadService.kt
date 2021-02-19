package org.nypl.simplified.tests.mocking

import org.nypl.simplified.ui.thread.api.UIThreadServiceType

class MockUIThreadService : UIThreadServiceType {
  override fun checkIsUIThread() {
  }

  override fun isUIThread(): Boolean {
    return false
  }

  override fun runOnUIThread(r: Runnable) {
    r.run()
  }

  override fun runOnUIThread(f: () -> Unit) {
    f.invoke()
  }

  override fun runOnUIThreadDelayed(r: Runnable, ms: Long) {
    r.run()
  }

  override fun runOnUIThreadDelayed(f: () -> Unit, ms: Long) {
    f.invoke()
  }
}
