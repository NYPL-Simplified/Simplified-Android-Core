package org.nypl.simplified.crashlytics.api

/**
 * This interface provides an API for wrapping Firebase Crashlytics or
 * a similar service provider.
 *
 * Implementations SHOULD match the behavior of the Crashlytics API.
 *
 * See [https://firebase.google.com/docs/crashlytics](https://firebase.google.com/docs/crashlytics).
 */

interface CrashlyticsServiceType {

  /** Logs a message with the next fatal or non-fatal report. */
  fun log(message: String)

  /** Records a non-fatal report to Crashlytics. */
  fun recordException(throwable: Throwable)

  /** Enables or disables data collection by Crashlytics. */
  fun setCollectionEnabled(enabled: Boolean)

  /** Records an identifier that's associated with subsequent fatal and non-fatal reports. */
  fun setUserId(identifier: String)

  /** Set a custom key and value that are associated with subsequent fatal and non-fatal reports. */
  fun setCustomKey(key: String, value: Int)

  /** Set a custom key and value that are associated with subsequent fatal and non-fatal reports. */
  fun setCustomKey(key: String, value: Long)

  /** Set a custom key and value that are associated with subsequent fatal and non-fatal reports. */
  fun setCustomKey(key: String, value: Float)

  /** Set a custom key and value that are associated with subsequent fatal and non-fatal reports. */
  fun setCustomKey(key: String, value: Double)

  /** Set a custom key and value that are associated with subsequent fatal and non-fatal reports. */
  fun setCustomKey(key: String, value: String)

  /** Set a custom key and value that are associated with subsequent fatal and non-fatal reports. */
  fun setCustomKey(key: String, value: Boolean)
}
