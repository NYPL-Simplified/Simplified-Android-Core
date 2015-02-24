package org.nypl.simplified.app;

import android.app.DialogFragment;
import android.app.Fragment;

import com.io7m.jfunctional.OptionType;

public interface FragmentControllerType
{
  void fragControllerSetAndShowDialog(
    final DialogFragment f);

  OptionType<Fragment> fragControllerGetContentFragmentCurrent();

  void fragControllerSetContentFragmentWithBackReturn(
    final Fragment return_to,
    final Fragment f);

  void fragControllerSetContentFragmentWithBackOptionalReturn(
    final OptionType<Fragment> return_to,
    final Fragment f);

  void fragControllerSetContentFragmentWithoutBack(
    final Fragment f);
}
