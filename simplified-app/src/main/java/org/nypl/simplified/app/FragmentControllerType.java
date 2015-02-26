package org.nypl.simplified.app;

import android.app.DialogFragment;
import android.os.Bundle;

import com.io7m.jfunctional.OptionType;

/**
 * The type of the fragment controller.
 */

public interface FragmentControllerType
{
  /**
   * Pop the back stack, calling <code>on_empty</code> if the back stack is
   * actually empty.
   *
   * @param on_empty
   *          The procedure to be called on an empty stack
   */

  void fragControllerPopBackStack(
    final Runnable on_empty);

  /**
   * Deserialize the state of all fragments from the given bundle, which was
   * expected to be populated by {@link #fragControllerSerialize(Bundle)}.
   *
   * @param b
   *          The bundle
   */

  void fragControllerDeserialize(
    final Bundle b);

  /**
   * @return The current fragment in the content frame, if any.
   */

  OptionType<SimplifiedFragment> fragControllerGetContentFragmentCurrent();

  /**
   * Serialize the state of all fragments into the given bundle, for later
   * deserialization with {@link #fragControllerDeserialize(Bundle)}.
   *
   * @param b
   *          The bundle
   */

  void fragControllerSerialize(
    final Bundle b);

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
