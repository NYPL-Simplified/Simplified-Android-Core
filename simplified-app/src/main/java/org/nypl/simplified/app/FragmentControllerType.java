package org.nypl.simplified.app;

import android.app.DialogFragment;
import android.app.Fragment;

public interface FragmentControllerType
{
  void setAndShowDialog(
    final DialogFragment f);

  void setContentFragmentWithBackReturn(
    final Fragment return_to,
    final Fragment f);

  void setContentFragmentWithoutBack(
    final Fragment f);
}
