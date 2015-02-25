package org.nypl.simplified.app;

import android.app.DialogFragment;

import com.io7m.jfunctional.OptionType;

/**
 * The type of the fragment controller.
 */

public interface FragmentControllerType
{
  /**
   * @return The current fragment in the content frame, if any.
   */

  OptionType<SimplifiedFragment> fragControllerGetContentFragmentCurrent();

  /**
   * Display the given dialog.
   *
   * @param f
   *          The dialog
   */

  void fragControllerSetAndShowDialog(
    final DialogFragment f);

  /**
   * Iff <code>return_to.isNone()</code>, this is equivalent to
   * {@link #fragControllerSetContentFragmentWithoutBack(SimplifiedFragment)}.
   * Otherwise, this function is equivalent to
   * {@link #fragControllerSetContentFragmentWithBackReturn(SimplifiedFragment, SimplifiedFragment)}
   * .
   *
   * @param return_to
   *          The return fragment
   * @param f
   *          The new fragment
   */

  void fragControllerSetContentFragmentWithBackOptionalReturn(
    final OptionType<SimplifiedFragment> return_to,
    final SimplifiedFragment f);

  /**
   * Set the given fragment <code>f</code> as the current fragment in the
   * content frame, adding <code>return_to</code> to the back stack so that
   * when the user presses the <i>back</i> button, the <code>return_to</code>
   * fragment becomes the current fragment in the content frame again.
   *
   * @param return_to
   *          The return fragment
   * @param f
   *          The new fragment
   */

  void fragControllerSetContentFragmentWithBackReturn(
    final SimplifiedFragment return_to,
    final SimplifiedFragment f);

  /**
   * Set the given fragment <code>f</code> as the current fragment in the
   * content frame.
   * 
   * @param f
   *          The new fragment
   */

  void fragControllerSetContentFragmentWithoutBack(
    final SimplifiedFragment f);
}
