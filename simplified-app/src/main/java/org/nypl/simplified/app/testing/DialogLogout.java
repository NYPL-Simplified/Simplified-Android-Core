package org.nypl.simplified.app.testing;

import org.nypl.simplified.app.LogoutDialog;

import android.app.Activity;
import android.app.FragmentManager;
import android.os.Bundle;

import com.io7m.jnull.Nullable;

public final class DialogLogout extends Activity
{
  @Override protected void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);

    final LogoutDialog d = LogoutDialog.newDialog();
    final FragmentManager fm = this.getFragmentManager();
    d.show(fm, "dialog");
  }
}
