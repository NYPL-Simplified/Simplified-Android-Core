package org.nypl.simplified.viewer.epub.readium1;

/**
 * Functions called via the {@code host_app_feedback.js} file using the {@code
 * readium} URI scheme.
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
   * Called on receipt of a {@code readium:initialize} request.
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
   * Called on receipt of a {@code readium:content-document-loaded} request.
   */

  void onReadiumContentDocumentLoaded();

  /**
   * Called when {@link #onReadiumContentDocumentLoaded()} raises an exception.
   *
   * @param e The raised exception
   */

  void onReadiumContentDocumentLoadedError(
    Throwable e);

  /**
   * Called on receipt of a {@code readium:pagination-changed} request.
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
   * Called on receipt of a {@code readium:settings-applied} request.
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
