package org.nypl.simplified.app.testing;

import org.nypl.simplified.app.catalog.CatalogBookDeleteDialog;

import android.app.Activity;
import android.app.FragmentManager;
import android.os.Bundle;

import com.io7m.jnull.Nullable;

public final class DialogBookDelete extends Activity
{
  @Override protected void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);

    final CatalogBookDeleteDialog d = CatalogBookDeleteDialog.newDialog();
    final FragmentManager fm = this.getFragmentManager();
    d.show(fm, "dialog");
  }
}
