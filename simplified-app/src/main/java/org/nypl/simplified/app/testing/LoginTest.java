package org.nypl.simplified.app.testing;

import org.nypl.simplified.app.LoginDialog;
import org.nypl.simplified.books.core.AccountBarcode;
import org.nypl.simplified.books.core.AccountPIN;

import android.app.Activity;
import android.app.FragmentManager;
import android.os.Bundle;

import com.io7m.jnull.Nullable;

public final class LoginTest extends Activity
{
  @Override protected void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);

    final LoginDialog d =
      LoginDialog.newDialog(
        "Login Required",
        new AccountBarcode("77777"),
        new AccountPIN("4444"));
    final FragmentManager fm = this.getFragmentManager();
    d.show(fm, "dialog");
  }
}
