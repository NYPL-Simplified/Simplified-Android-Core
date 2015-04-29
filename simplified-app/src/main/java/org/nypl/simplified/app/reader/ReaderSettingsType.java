package org.nypl.simplified.app.reader;

public interface ReaderSettingsType
{
  void addListener(
    ReaderSettingsListenerType l);

  ReaderColorScheme getColorScheme();

  float getFontScale();

  void removeListener(
    ReaderSettingsListenerType l);

  void setColorScheme(
    ReaderColorScheme c);

  void setFontScale(
    float s);
}
