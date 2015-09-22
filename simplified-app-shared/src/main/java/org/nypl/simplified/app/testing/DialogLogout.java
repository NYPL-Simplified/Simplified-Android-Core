package org.nypl.simplified.app.testing;

import android.app.Activity;
import android.app.FragmentManager;
import android.os.Bundle;
import com.io7m.jnull.Nullable;
import org.nypl.simplified.app.LogoutDialog;

/**
 * A dialog activity requesting logout confirmation.
 */

public final class DialogLogout extends Activity
{
  /**
   * Construct an activity.
   */

  public DialogLogout()
  {

  }

  @Override protected void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);

    final LogoutDialog d = LogoutDialog.newDialog();
    final FragmentManager fm = this.getFragmentManager();
    d.show(fm, "dialog");
  }
}
