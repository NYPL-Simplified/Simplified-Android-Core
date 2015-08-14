package org.nypl.simplified.app;

import android.content.SharedPreferences;
import android.content.res.AssetManager;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;
import org.nypl.simplified.app.utilities.LogUtilities;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

/**
 * The default implementation of the {@link EULAType} interface.
 */

public final class EULA implements EULAType
{
  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(EULA.class);
  }

  private final SharedPreferences prefs;
  private final URL               current;

  private EULA(
    final URL in_current,
    final SharedPreferences in_prefs)
  {
    this.current = NullCheck.notNull(in_current);
    this.prefs = NullCheck.notNull(in_prefs);
  }

  /**
   * Retrieve the EULA, if one is defined.
   *
   * @param assets The assets within which the EULA text may be defined in a
   *               file called {@code eula.html}.
   * @param prefs  The shared preferences used to manage state
   *
   * @return An EULA, if any
   */

  public static OptionType<EULAType> getEULA(
    final AssetManager assets,
    final SharedPreferences prefs)
  {
    NullCheck.notNull(assets);

    try {
      final InputStream is = assets.open("eula.html", 0);
      try {
        final URL url = new URL("file:///android_asset/eula.html");
        final EULAType eula = new EULA(url, prefs);
        return Option.some(eula);
      } finally {
        is.close();
      }
    } catch (final IOException e) {
      EULA.LOG.debug("could not open eula.html: ", e);
      return Option.none();
    }
  }

  @Override public boolean eulaHasAgreed()
  {
    return this.prefs.getBoolean("eula-agreed", false);
  }

  @Override public void eulaSetLatestURI(final URI u)
  {
    NullCheck.notNull(u);
    final SharedPreferences.Editor edit = this.prefs.edit();
    edit.putString("eula-uri-latest", u.toString());
    edit.apply();
  }

  @Override public URL eulaGetReadableURL()
  {
    return this.current;
  }

  @Override public void eulaSetHasAgreed(final boolean t)
  {
    final SharedPreferences.Editor edit = this.prefs.edit();
    edit.putBoolean("eula-agreed", t);
    edit.apply();
  }
}
