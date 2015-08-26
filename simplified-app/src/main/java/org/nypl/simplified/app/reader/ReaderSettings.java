package org.nypl.simplified.app.reader;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import org.nypl.simplified.app.utilities.LogUtilities;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User-configurable reader settings.
 */

public final class ReaderSettings implements ReaderSettingsType
{
  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(ReaderSettings.class);
  }

  private final Map<ReaderSettingsListenerType, Unit> listeners;
  private final SharedPreferences                     settings;

  private ReaderSettings(
    final Context cc)
  {
    NullCheck.notNull(cc);
    this.settings = NullCheck.notNull(cc.getSharedPreferences("reader", 0));
    this.listeners = new ConcurrentHashMap<ReaderSettingsListenerType, Unit>();
  }

  /**
   * Open the reader settings.
   *
   * @param cc The application context
   *
   * @return The reader settings
   */

  public static ReaderSettingsType openSettings(
    final Context cc)
  {
    return new ReaderSettings(cc);
  }

  @Override public void addListener(
    final ReaderSettingsListenerType l)
  {
    NullCheck.notNull(l);
    ReaderSettings.LOG.debug("adding listener: {}", l);
    this.listeners.put(l, Unit.unit());
  }

  @Override public ReaderFontSelection getFontFamily()
  {
    try {
      final String raw = NullCheck.notNull(
        this.settings.getString(
          "font_family", ReaderFontSelection.READER_FONT_SERIF.toString()));
      return NullCheck.notNull(ReaderFontSelection.valueOf(raw));
    } catch (final Throwable x) {
      ReaderSettings.LOG.error(
        "failed to parse color scheme: {}", x.getMessage(), x);
      return ReaderFontSelection.READER_FONT_SERIF;
    }
  }

  @Override public void setFontFamily(final ReaderFontSelection f)
  {
    final Editor e = this.settings.edit();
    e.putString("font_family", NullCheck.notNull(f).toString());
    e.apply();
    this.broadcastChanges();
  }

  private void broadcastChanges()
  {
    for (final ReaderSettingsListenerType l : this.listeners.keySet()) {
      try {
        l.onReaderSettingsChanged(this);
      } catch (final Throwable x) {
        ReaderSettings.LOG.error(
          "listener raised exception: {}", l, x.getMessage(), x);
      }
    }
  }

  @Override public ReaderColorScheme getColorScheme()
  {
    try {
      final String raw = NullCheck.notNull(
        this.settings.getString(
          "color_scheme", ReaderColorScheme.SCHEME_BLACK_ON_BEIGE.toString()));
      return NullCheck.notNull(ReaderColorScheme.valueOf(raw));
    } catch (final Throwable x) {
      ReaderSettings.LOG.error(
        "failed to parse color scheme: {}", x.getMessage(), x);
      return ReaderColorScheme.SCHEME_BLACK_ON_BEIGE;
    }
  }

  @Override public void setColorScheme(
    final ReaderColorScheme c)
  {
    final Editor e = this.settings.edit();
    e.putString("color_scheme", NullCheck.notNull(c).toString());
    e.apply();
    this.broadcastChanges();
  }

  @Override public float getFontScale()
  {
    try {
      return this.settings.getFloat("font_scale", 100.0f);
    } catch (final Throwable x) {
      ReaderSettings.LOG.error(
        "failed to parse font scale: {}", x.getMessage(), x);
      return 100.0f;
    }
  }

  @Override public void setFontScale(
    final float s)
  {
    final Double x =
      Double.valueOf(Math.max(50.0, Math.min((double) s, 200.0)));
    ReaderSettings.LOG.debug("font size: {}%", x);

    final Editor e = this.settings.edit();
    e.putFloat("font_scale", x.floatValue());
    e.apply();
    this.broadcastChanges();
  }

  @Override public void removeListener(
    final ReaderSettingsListenerType l)
  {
    NullCheck.notNull(l);
    ReaderSettings.LOG.debug("removing listener: {}", l);
    this.listeners.remove(l);
  }
}
