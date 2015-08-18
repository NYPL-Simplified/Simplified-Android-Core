package org.nypl.simplified.app.reader;

/**
 * The settings interface for the reader.
 */

public interface ReaderSettingsType
{
  /**
   * Add a listener that will be notified of settings changes.
   *
   * @param l The listener
   */

  void addListener(
    ReaderSettingsListenerType l);

  /**
   * @return The current color scheme
   */

  ReaderColorScheme getColorScheme();

  /**
   * Set the current color scheme.
   *
   * @param c The color scheme
   */

  void setColorScheme(
    ReaderColorScheme c);

  /**
   * @return The current font scale
   */

  float getFontScale();

  /**
   * Set the current font scale.
   *
   * @param s The font scale
   */

  void setFontScale(
    float s);

  /**
   * Remove the given listener from the settings. Has no effect if the listener
   * has not been added previously.
   *
   * @param l The listener
   */

  void removeListener(
    ReaderSettingsListenerType l);
}
