package org.nypl.simplified.app;

/**
 * <p>
 * The type of listeners called when the <i>up</i> button is pressed.
 * </p>
 * <p>
 * This interface is usually implemented by fragments and will only be called
 * when the fragment in question is the current fragment.
 * </p>
 */

public interface UpButtonListenerType
{
  /**
   * Called when the <i>up</i> button should be configured. This typically
   * means enabling or disabling the <i>up</i> button based on a
   * fragment-specific <i>up stack</i>.
   */

  void onUpButtonConfigure();

  /**
   * Called when the <i>up</i> button has been pressed.
   */

  void onUpButtonPressed();
}
