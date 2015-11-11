package org.nypl.simplified.bugsnag;

import android.content.Context;

import com.bugsnag.android.Bugsnag;
import com.io7m.jnull.NullCheck;

/**
 *
 */
public final class IfBugsnag
{
  private static BugsnagType INSTANCE;

  private IfBugsnag()
  {
  }

  /**
   * Initialize the no-op Bugsnag dummy
   */
  public static void init()
  {
    if (INSTANCE == null) {
      INSTANCE = new BugsnagDummy();
    }
  }

  /**
   * Initialize the wrapped Bugsnag client
   *
   * @param context an Android context, usually <code>this</code>
   * @param api_key your Bugsnag API key from your Bugsnag dashboard
   */
  public static void init(final Context context, final String api_key)
  {
    NullCheck.notNull(context);
    NullCheck.notNull(api_key);

    if (INSTANCE == null) {
      INSTANCE = new BugsnagWrapper(Bugsnag.init(context, api_key));
    }
  }

  /**
   * @return Bugsnag instance
   */
  public static BugsnagType get()
  {
    if (INSTANCE == null) {
      throw new IllegalStateException("You must call IfBugsnag.init before getting an instance");
    }

    return INSTANCE;
  }
}
