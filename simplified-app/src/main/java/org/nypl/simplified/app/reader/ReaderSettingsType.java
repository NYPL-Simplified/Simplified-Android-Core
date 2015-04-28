package org.nypl.simplified.app.reader;

public interface ReaderSettingsType
{
  ReaderColorScheme getColorScheme();

  void setColorScheme(
    ReaderColorScheme c);

  void addListener(
    ReaderSettingsListenerType l);

  void removeListener(
    ReaderSettingsListenerType l);
}
