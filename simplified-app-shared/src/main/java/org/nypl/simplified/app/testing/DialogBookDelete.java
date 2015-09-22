package org.nypl.simplified.app.testing;

import android.app.Activity;
import android.app.FragmentManager;
import android.os.Bundle;
import com.io7m.jnull.Nullable;
import org.nypl.simplified.app.catalog.CatalogBookDeleteDialog;

/**
 * A book deletion activity.
 */

public final class DialogBookDelete extends Activity
{
  /**
   * Construct an activity.
   */

  public DialogBookDelete()
  {

  }

  @Override protected void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);

    final CatalogBookDeleteDialog d = CatalogBookDeleteDialog.newDialog();
    final FragmentManager fm = this.getFragmentManager();
    d.show(fm, "dialog");
  }
}
