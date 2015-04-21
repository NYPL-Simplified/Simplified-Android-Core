package org.nypl.simplified.app.reader;

/**
 * Functions called via the <tt>host_app_feedback.js</tt> file using the
 * <tt>simplified</tt> URI scheme.
 */

public interface ReaderSimplifiedFeedbackListenerType
{
  /**
   * Called when an exception is raised when trying to dispatch a function.
   */

  void onSimplifiedFunctionDispatchError(
    Throwable x);

  /**
   * Called when an unknown request is made.
   */

  void onSimplifiedFunctionUnknown(
    String text);

  /**
   * Called upon receipt of a leftwards gesture.
   */

  void onSimplifiedGestureLeft();

  /**
   * Called if {@link #onSimplifiedGestureLeft()} raises an exception.
   */

  void onSimplifiedGestureLeftError(
    Throwable x);

  /**
   * Called upon receipt of a rightwards gesture.
   */

  void onSimplifiedGestureRight();

  /**
   * Called if {@link #onSimplifiedGestureRight()} raises an exception.
   */

  void onSimplifiedGestureRightError(
    Throwable x);
}
