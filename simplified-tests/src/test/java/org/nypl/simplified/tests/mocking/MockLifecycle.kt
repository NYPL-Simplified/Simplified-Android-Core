package org.nypl.simplified.tests.mocking

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver

class MockLifecycle : Lifecycle() {

  var state: State = State.INITIALIZED

  override fun addObserver(observer: LifecycleObserver) {
  }

  override fun removeObserver(observer: LifecycleObserver) {
  }

  override fun getCurrentState(): State {
    return this.state
  }
}
