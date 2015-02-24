package org.nypl.simplified.app;

import android.app.DialogFragment;
import android.app.Fragment;

public interface FragmentControllerType
{
  void setContentFragmentWithoutBack(
    final Fragment f);

  void setContentFragmentWithBackReturn(
    final Fragment return_to,
    final Fragment f);

  void setAndShowDialog(
    final DialogFragment f);
}
