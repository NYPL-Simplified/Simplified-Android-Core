package org.nypl.simplified.app.reader;

/**
 * Functions called via the {@code host_app_feedback.js} file using the
 * {@code simplified} URI scheme.
 */

public interface ReaderSimplifiedFeedbackListenerType
{
  /**
   * Called when an exception is raised when trying to dispatch a function.
   *
   * @param x The exception
   */

  void onSimplifiedFunctionDispatchError(
    Throwable x);

  /**
   * Called when an unknown request is made.
   *
   * @param text The text of the request
   */

  void onSimplifiedFunctionUnknown(
    String text);

  /**
   * Called upon receipt of a center gesture.
   */

  void onSimplifiedGestureCenter();

  /**
   * Called if {@link #onSimplifiedGestureCenter()} raises an exception.
   *
   * @param x The exception raised
   */

  void onSimplifiedGestureCenterError(
    Throwable x);

  /**
   * Called upon receipt of a leftwards gesture.
   */

  void onSimplifiedGestureLeft();

  /**
   * Called if {@link #onSimplifiedGestureLeft()} raises an exception.
   *
   * @param x The exception raised
   */

  void onSimplifiedGestureLeftError(
    Throwable x);

  /**
   * Called upon receipt of a rightwards gesture.
   */

  void onSimplifiedGestureRight();

  /**
   * Called if {@link #onSimplifiedGestureRight()} raises an exception.
   *
   * @param x The exception raised
   */

  void onSimplifiedGestureRightError(
    Throwable x);
}
