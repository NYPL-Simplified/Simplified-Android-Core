package org.nypl.simplified.webview

import android.content.res.Configuration
import android.webkit.CookieManager
import android.webkit.WebSettings
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebSettingsCompat.FORCE_DARK_ON
import androidx.webkit.WebSettingsCompat.FORCE_DARK_OFF
import androidx.webkit.WebViewFeature
import org.nypl.simplified.accounts.api.AccountCookie
import org.nypl.simplified.android.ktx.isNightModeYes
import org.slf4j.LoggerFactory
import java.io.File
import java.text.SimpleDateFormat
import java.util.TimeZone

/**
 * Web view utility functions.
 */

object WebViewUtilities {
  private val logger = LoggerFactory.getLogger(WebViewUtilities::class.java)

  private val httpDateFormatter =
    SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z").apply {
      timeZone = TimeZone.getTimeZone("GMT")
    }

  /**
   * Dump cookies from the Android web view as a list of account cookies.
   */

  fun dumpCookiesAsAccountCookies(
    cookieManager: CookieManager,
    dataDir: File
  ): List<AccountCookie> {
    cookieManager.flush()

    return try {
      WebViewCookieDatabase.open(dataDir).use {
        it.getAll().map { webViewCookie ->
          AccountCookie(
            url = webViewCookie.sourceURL,
            value = webViewCookie.toSetCookieString()
          )
        }
      }
    } catch (e: Exception) {
      logger.error("could not dump cookies from {}:", dataDir, e)

      listOf<AccountCookie>()
    }
  }

  /**
   * Convert a webkit timestamp (microseconds since 1 Jan 1601) to a unix timestamp (milliseconds
   * since 1 Jan 1970).
   */

  private fun webkitTimeToUnixTime(
    time: Long
  ): Long {
    return (time / 1000L - 11644473600000L)
  }

  /**
   * Format a webkit timestamp as a string suitable for sending in an HTTP header.
   */

  fun formatWebKitTimestampForHTTP(
    timestamp: Long
  ): String {
    return httpDateFormatter.format(
      webkitTimeToUnixTime(timestamp)
    )
  }

  fun setForcedDark(settings: WebSettings, configuration: Configuration) {
    if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
      val forceDarkMode = if (configuration.isNightModeYes) FORCE_DARK_ON else FORCE_DARK_OFF
      WebSettingsCompat.setForceDark(settings, forceDarkMode)
    }
  }
}
