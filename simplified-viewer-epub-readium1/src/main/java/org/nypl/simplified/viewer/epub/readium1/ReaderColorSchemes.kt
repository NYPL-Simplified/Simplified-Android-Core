package org.nypl.simplified.app.reader

import android.graphics.Color
import androidx.annotation.ColorInt
import org.nypl.simplified.reader.api.ReaderColorScheme

/**
 * Color schemes.
 */

object ReaderColorSchemes {

  /**
   * Retrieve the background color for the given scheme as an Android ARGB color int.
   */

  @JvmStatic
  @ColorInt
  fun backgroundAsAndroidColor(scheme: ReaderColorScheme): Int =
    when (scheme) {
      ReaderColorScheme.SCHEME_BLACK_ON_BEIGE ->
        Color.argb(0xff, 0xf2, 0xe4, 0xcb)
      ReaderColorScheme.SCHEME_BLACK_ON_WHITE ->
        Color.argb(0xff, 0xff, 0xff, 0xff)
      ReaderColorScheme.SCHEME_WHITE_ON_BLACK ->
        Color.argb(0xff, 0x00, 0x00, 0x00)
    }

  /**
   * Retrieve the foreground color for the given scheme as an Android ARGB color int.
   */

  @JvmStatic
  @ColorInt
  fun foregroundAsAndroidColor(scheme: ReaderColorScheme): Int =
    when (scheme) {
      ReaderColorScheme.SCHEME_BLACK_ON_BEIGE ->
        Color.argb(0xff, 0x00, 0x00, 0x00)
      ReaderColorScheme.SCHEME_BLACK_ON_WHITE ->
        Color.argb(0xff, 0x00, 0x00, 0x00)
      ReaderColorScheme.SCHEME_WHITE_ON_BLACK ->
        Color.argb(0xff, 0xff, 0xff, 0xff)
    }

  /**
   * Retrieve the foreground color for the given scheme as a web '#rrggbb' string.
   */

  @JvmStatic
  fun backgroundAsBrowserHex(scheme: ReaderColorScheme): String =
    when (scheme) {
      ReaderColorScheme.SCHEME_BLACK_ON_BEIGE -> "#f2e4cb"
      ReaderColorScheme.SCHEME_BLACK_ON_WHITE -> "#ffffff"
      ReaderColorScheme.SCHEME_WHITE_ON_BLACK -> "#000000"
    }

  /**
   * Retrieve the foreground color for the given scheme as a web '#rrggbb' string.
   */

  @JvmStatic
  fun foregroundAsBrowserHex(scheme: ReaderColorScheme): String =
    when (scheme) {
      ReaderColorScheme.SCHEME_BLACK_ON_BEIGE -> "#000000"
      ReaderColorScheme.SCHEME_BLACK_ON_WHITE -> "#000000"
      ReaderColorScheme.SCHEME_WHITE_ON_BLACK -> "#ffffff"
    }
}
