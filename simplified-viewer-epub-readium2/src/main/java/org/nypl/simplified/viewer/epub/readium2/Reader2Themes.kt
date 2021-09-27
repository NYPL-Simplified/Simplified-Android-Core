package org.nypl.simplified.viewer.epub.readium2

import org.librarysimplified.r2.api.SR2ColorScheme
import org.librarysimplified.r2.api.SR2Font
import org.librarysimplified.r2.api.SR2PublisherCSS
import org.librarysimplified.r2.api.SR2PublisherCSS.SR2_PUBLISHER_DEFAULT_CSS_DISABLED
import org.librarysimplified.r2.api.SR2PublisherCSS.SR2_PUBLISHER_DEFAULT_CSS_ENABLED
import org.librarysimplified.r2.api.SR2Theme
import org.nypl.simplified.reader.api.ReaderColorScheme
import org.nypl.simplified.reader.api.ReaderFontSelection
import org.nypl.simplified.reader.api.ReaderPreferences
import org.nypl.simplified.reader.api.ReaderPublisherCSS
import org.nypl.simplified.reader.api.ReaderPublisherCSS.PUBLISHER_DEFAULT_CSS_DISABLED
import org.nypl.simplified.reader.api.ReaderPublisherCSS.PUBLISHER_DEFAULT_CSS_ENABLED

object Reader2Themes {

  /**
   * Convert an SR2 theme to SimplyE reader preferences.
   */

  fun fromSR2(theme: SR2Theme): ReaderPreferences {
    return ReaderPreferences.builder()
      .setBrightness(1.0)
      .setColorScheme(fromSR2Color(theme.colorScheme))
      .setFontFamily(fromSR2Font(theme.font))
      .setFontScale(fromSR2Size(theme.textSize))
      .setPublisherCSS(fromSR2PublisherCSS(theme.publisherCSS))
      .build()
  }

  private fun fromSR2PublisherCSS(
    publisherCSS: SR2PublisherCSS
  ): ReaderPublisherCSS {
    return when (publisherCSS) {
      SR2_PUBLISHER_DEFAULT_CSS_ENABLED -> PUBLISHER_DEFAULT_CSS_ENABLED
      SR2_PUBLISHER_DEFAULT_CSS_DISABLED -> PUBLISHER_DEFAULT_CSS_DISABLED
    }
  }

  private fun fromSR2Size(
    textSize: Double
  ): Double {
    return textSize * 100.0
  }

  private fun fromSR2Font(
    font: SR2Font
  ): ReaderFontSelection {
    return when (font) {
      SR2Font.FONT_SANS ->
        ReaderFontSelection.READER_FONT_SANS_SERIF
      SR2Font.FONT_SERIF ->
        ReaderFontSelection.READER_FONT_SERIF
      SR2Font.FONT_OPEN_DYSLEXIC ->
        ReaderFontSelection.READER_FONT_OPEN_DYSLEXIC
    }
  }

  private fun fromSR2Color(
    colorScheme: SR2ColorScheme
  ): ReaderColorScheme {
    return when (colorScheme) {
      SR2ColorScheme.DARK_TEXT_LIGHT_BACKGROUND ->
        ReaderColorScheme.SCHEME_BLACK_ON_WHITE
      SR2ColorScheme.LIGHT_TEXT_DARK_BACKGROUND ->
        ReaderColorScheme.SCHEME_WHITE_ON_BLACK
      SR2ColorScheme.DARK_TEXT_ON_SEPIA ->
        ReaderColorScheme.SCHEME_BLACK_ON_BEIGE
    }
  }

  /**
   * Convert SimplyE reader preferences to an SR2 theme.
   */

  fun toSR2(
    readerPreferences: ReaderPreferences
  ): SR2Theme {
    return SR2Theme(
      colorScheme = toSR2Color(readerPreferences.colorScheme()),
      font = toSR2Font(readerPreferences.fontFamily()),
      textSize = toSR2Size(readerPreferences.fontScale()),
      publisherCSS = toSR2PublisherCSS(readerPreferences.publisherCSS())
    )
  }

  private fun toSR2PublisherCSS(
    publisherCSS: ReaderPublisherCSS
  ): SR2PublisherCSS {
    return when (publisherCSS) {
      PUBLISHER_DEFAULT_CSS_ENABLED -> SR2_PUBLISHER_DEFAULT_CSS_ENABLED
      PUBLISHER_DEFAULT_CSS_DISABLED -> SR2_PUBLISHER_DEFAULT_CSS_DISABLED
    }
  }

  private fun toSR2Size(
    fontScale: Double
  ): Double {
    return SR2Theme.sizeConstrain(fontScale / 100.0)
  }

  private fun toSR2Font(fontFamily: ReaderFontSelection): SR2Font {
    return when (fontFamily) {
      ReaderFontSelection.READER_FONT_SANS_SERIF ->
        SR2Font.FONT_SANS
      ReaderFontSelection.READER_FONT_OPEN_DYSLEXIC ->
        SR2Font.FONT_OPEN_DYSLEXIC
      ReaderFontSelection.READER_FONT_SERIF ->
        SR2Font.FONT_SERIF
    }
  }

  private fun toSR2Color(colorScheme: ReaderColorScheme): SR2ColorScheme {
    return when (colorScheme) {
      ReaderColorScheme.SCHEME_BLACK_ON_BEIGE ->
        SR2ColorScheme.DARK_TEXT_ON_SEPIA
      ReaderColorScheme.SCHEME_BLACK_ON_WHITE ->
        SR2ColorScheme.DARK_TEXT_LIGHT_BACKGROUND
      ReaderColorScheme.SCHEME_WHITE_ON_BLACK ->
        SR2ColorScheme.LIGHT_TEXT_DARK_BACKGROUND
    }
  }
}
