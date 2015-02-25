package org.nypl.simplified.app;

import android.app.DialogFragment;

import com.io7m.jfunctional.OptionType;

public interface FragmentControllerType
{
  OptionType<SimplifiedFragment> fragControllerGetContentFragmentCurrent();

  void fragControllerSetAndShowDialog(
    final DialogFragment f);

  void fragControllerSetContentFragmentWithBackOptionalReturn(
    final OptionType<SimplifiedFragment> return_to,
    final SimplifiedFragment f);

  void fragControllerSetContentFragmentWithBackReturn(
    final SimplifiedFragment return_to,
    final SimplifiedFragment f);

  void fragControllerSetContentFragmentWithoutBack(
    final SimplifiedFragment f);
}
