package org.nypl.simplified.app.reader;

/**
 * The type of listeners that receive settings changes.
 */

public interface ReaderSettingsListenerType
{
  /**
   * The current settings have changed.
   *
   * @param s The current settings
   */

  void onReaderSettingsChanged(
    ReaderSettingsType s);
}
