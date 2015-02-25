package org.nypl.simplified.app;

import android.app.Fragment;

/**
 * <p>
 * The type of all fragments used in the app.
 * </p>
 * <p>
 * This type solely exists to ensure that all of the app fragments implement
 * common interfaces to facilitate, for example, "up" button handling.
 * </p>
 */

public abstract class SimplifiedFragment extends Fragment implements
  UpButtonListenerType
{
  @Override public void onResume()
  {
    super.onResume();
    this.onUpButtonConfigure();
  }
}
