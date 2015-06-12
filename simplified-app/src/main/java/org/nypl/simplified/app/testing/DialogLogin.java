package org.nypl.simplified.app.testing;

import org.nypl.simplified.app.LoginDialog;
import org.nypl.simplified.books.core.AccountBarcode;
import org.nypl.simplified.books.core.AccountPIN;

import android.app.Activity;
import android.app.FragmentManager;
import android.os.Bundle;

import com.io7m.jnull.Nullable;

public final class DialogLogin extends Activity
{
  @Override protected void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);

    final LoginDialog d =
      LoginDialog.newDialog(
        "Something here",
        new AccountBarcode(""),
        new AccountPIN(""));
    final FragmentManager fm = this.getFragmentManager();
    d.show(fm, "dialog");
  }
}
