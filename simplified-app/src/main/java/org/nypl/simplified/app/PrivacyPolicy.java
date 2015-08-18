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
 * The default implementation of the {@link PrivacyPolicyType} interface.
 */

public final class PrivacyPolicy extends SyncedDocumentAbstract
  implements PrivacyPolicyType
{
  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(PrivacyPolicy.class);
  }

  protected PrivacyPolicy(
    final Logger in_log,
    final HTTPType in_http,
    final ExecutorService in_exec,
    final SharedPreferences in_prefs,
    final File in_stored)
  {
    super(in_log, in_http, in_exec, in_prefs, in_stored, "privacy");
  }

  /**
   * Retrieve the privacy policy, if one is defined. A privacy policy is assumed
   * to be defined if there is a file called {@code privacy.html} in the root of
   * the Android assets.
   *
   * @param http    An HTTP interface
   * @param exec    An executor that will be used to perform asynchronous
   *                updates of the policy text
   * @param context The application context
   *
   * @return A privacy policy, if any
   */

  public static OptionType<PrivacyPolicyType> getPrivacyPolicy(
    final HTTPType http,
    final ExecutorService exec,
    final Context context)
  {
    NullCheck.notNull(http);
    NullCheck.notNull(exec);
    NullCheck.notNull(context);

    final SharedPreferences prefs =
      SyncedDocumentAbstract.getPreferencesForBaseName(context, "privacy");
    final File stored =
      SyncedDocumentAbstract.getStoredFileForBaseName(context, "privacy");

    if (SyncedDocumentAbstract.baseFileExists(
      PrivacyPolicy.LOG, context, "privacy")) {
      final PrivacyPolicyType pp =
        new PrivacyPolicy(PrivacyPolicy.LOG, http, exec, prefs, stored);
      return Option.some(pp);
    }

    return Option.none();
  }
}
