package org.nypl.simplified.app.reader;

/**
 * Functions called via the <tt>host_app_feedback.js</tt> file using the
 * <tt>readium</tt> URI scheme.
 */

public interface ReaderReadiumFeedbackListenerType
{
  /**
   * Called when an exception is raised when trying to dispatch a function.
   *
   * @param x The raised exception
   */

  void onReadiumFunctionDispatchError(
    Throwable x);

  /**
   * Called on receipt of a <tt>readium:initialize</tt> request.
   */

  void onReadiumFunctionInitialize();

  /**
   * Called when {@link #onReadiumFunctionInitialize()} raises an exception.
   *
   * @param e The raised exception
   */

  void onReadiumFunctionInitializeError(
    Throwable e);

  /**
   * Called on receipt of a <tt>readium:pagination-changed</tt> request.
   *
   * @param e The pagination event
   */

  void onReadiumFunctionPaginationChanged(
    ReaderPaginationChangedEvent e);

  /**
   * Called when {@link #onReadiumFunctionPaginationChanged
   * (ReaderPaginationChangedEvent)} raises an exception.
   *
   * @param e The raised exception
   */

  void onReadiumFunctionPaginationChangedError(
    Throwable e);

  /**
   * Called on receipt of a <tt>readium:settings-applied</tt> request.
   */

  void onReadiumFunctionSettingsApplied();

  /**
   * Called when {@link #onReadiumFunctionSettingsApplied()} raises an
   * exception.
   *
   * @param e The raised exception
   */

  void onReadiumFunctionSettingsAppliedError(
    Throwable e);

  /**
   * Called when an unknown request is made.
   *
   * @param text The text of the request
   */

  void onReadiumFunctionUnknown(
    String text);

  /**
   * The status of the media overlay has changed; media is/is not playing.
   *
   * @param playing {@code true} if the media is playing
   */

  void onReadiumMediaOverlayStatusChangedIsPlaying(
    boolean playing);

  /**
   * Called when {@link #onReadiumMediaOverlayStatusChangedIsPlaying(boolean)}
   * raises an exception.
   *
   * @param e The raised exception
   */

  void onReadiumMediaOverlayStatusError(
    Throwable e);
}
