package org.nypl.simplified.crashlytics

import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.nypl.simplified.crashlytics.api.CrashlyticsServiceType

class CrashlyticsService : CrashlyticsServiceType {
  private val instance by lazy {
    FirebaseCrashlytics.getInstance()
  }

  override fun log(message: String) {
    this.instance.log(message)
  }

  override fun recordException(throwable: Throwable) {
    this.instance.recordException(throwable)
  }

  override fun setCollectionEnabled(enabled: Boolean) {
    this.instance.setCrashlyticsCollectionEnabled(enabled)
  }

  override fun setUserId(identifier: String) {
    this.instance.setUserId(identifier)
  }

  override fun setCustomKey(key: String, value: Int) {
    this.instance.setCustomKey(key, value)
  }

  override fun setCustomKey(key: String, value: Long) {
    this.instance.setCustomKey(key, value)
  }

  override fun setCustomKey(key: String, value: Float) {
    this.instance.setCustomKey(key, value)
  }

  override fun setCustomKey(key: String, value: Double) {
    this.instance.setCustomKey(key, value)
  }

  override fun setCustomKey(key: String, value: String) {
    this.instance.setCustomKey(key, value)
  }

  override fun setCustomKey(key: String, value: Boolean) {
    this.instance.setCustomKey(key, value)
  }
}
