package org.nypl.simplified.app.reader;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.nypl.simplified.app.utilities.LogUtilities;
import org.slf4j.Logger;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;

/**
 * User-configurable reader settings.
 */

public final class ReaderSettings implements ReaderSettingsType
{
  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(ReaderSettings.class);
  }

  public static ReaderSettingsType openSettings(
    final Context cc)
  {
    return new ReaderSettings(cc);
  }

  private final Map<ReaderSettingsListenerType, Unit> listeners;
  private final SharedPreferences                     settings;

  private ReaderSettings(
    final Context cc)
  {
    NullCheck.notNull(cc);
    this.settings = NullCheck.notNull(cc.getSharedPreferences("reader", 0));
    this.listeners =
      new ConcurrentHashMap<ReaderSettingsListenerType, Unit>();
  }

  @Override public void addListener(
    final ReaderSettingsListenerType l)
  {
    NullCheck.notNull(l);
    ReaderSettings.LOG.debug("adding listener: {}", l);
    this.listeners.put(l, Unit.unit());
  }

  private void broadcastChanges()
  {
    for (final ReaderSettingsListenerType l : this.listeners.keySet()) {
      try {
        l.onReaderSettingsChanged(this);
      } catch (final Throwable x) {
        ReaderSettings.LOG.error(
          "listener raised exception: {}",
          l,
          x.getMessage(),
          x);
      }
    }
  }

  @Override public ReaderColorScheme getColorScheme()
  {
    try {
      final String raw =
        NullCheck.notNull(this.settings.getString(
          "color_scheme",
          ReaderColorScheme.SCHEME_BLACK_ON_BEIGE.toString()));
      return NullCheck.notNull(ReaderColorScheme.valueOf(raw));
    } catch (final Throwable x) {
      return ReaderColorScheme.SCHEME_BLACK_ON_BEIGE;
    }
  }

  @Override public void removeListener(
    final ReaderSettingsListenerType l)
  {
    NullCheck.notNull(l);
    ReaderSettings.LOG.debug("removing listener: {}", l);
    this.listeners.remove(l);
  }

  @Override public void setColorScheme(
    final ReaderColorScheme c)
  {
    final Editor e = this.settings.edit();
    e.putString("color_scheme", NullCheck.notNull(c).toString());
    e.apply();
    this.broadcastChanges();
  }
}
