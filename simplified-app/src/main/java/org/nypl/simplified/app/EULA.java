package org.nypl.simplified.app;

import android.content.Context;
import android.content.SharedPreferences;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;
import org.nypl.simplified.app.utilities.LogUtilities;
import org.nypl.simplified.http.core.HTTPType;
import org.slf4j.Logger;

import java.io.File;
import java.util.concurrent.ExecutorService;

/**
 * The default implementation of the {@link EULAType} interface.
 */

public final class EULA extends SyncedDocumentAbstract implements EULAType
{
  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(EULA.class);
  }

  private final SharedPreferences prefs;

  private EULA(
    final HTTPType in_http,
    final ExecutorService in_exec,
    final SharedPreferences in_prefs,
    final File in_stored)
  {
    super(EULA.LOG, in_http, in_exec, in_prefs, in_stored, "eula");
    this.prefs = NullCheck.notNull(in_prefs);
  }

  /**
   * Retrieve the EULA, if one is defined. An EULA is assumed to be defined if
   * there is a file called {@code eula.html} in the root of the Android
   * assets.
   *
   * @param http    An HTTP interface
   * @param exec    An executor that will be used to perform asynchronous
   *                updates of the EULA text
   * @param context The application context
   *
   * @return An EULA, if any
   */

  public static OptionType<EULAType> getEULA(
    final HTTPType http,
    final ExecutorService exec,
    final Context context)
  {
    NullCheck.notNull(http);
    NullCheck.notNull(exec);
    NullCheck.notNull(context);

    final SharedPreferences prefs =
      SyncedDocumentAbstract.getPreferencesForBaseName(context, "eula");
    final File stored =
      SyncedDocumentAbstract.getStoredFileForBaseName(context, "eula");

    if (SyncedDocumentAbstract.baseFileExists(EULA.LOG, context, "eula")) {
      final EULAType eula = new EULA(http, exec, prefs, stored);
      return Option.some(eula);
    }

    return Option.none();
  }

  @Override public boolean eulaHasAgreed()
  {
    return this.prefs.getBoolean("eula-agreed", false);
  }

  @Override public void eulaSetHasAgreed(final boolean t)
  {
    final SharedPreferences.Editor edit = this.prefs.edit();
    edit.putBoolean("eula-agreed", t);
    edit.apply();
  }
}
