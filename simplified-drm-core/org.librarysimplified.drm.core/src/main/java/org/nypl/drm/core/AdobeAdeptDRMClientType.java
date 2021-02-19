package org.nypl.drm.core;

/**
 * <p> Callback functions for the DRM client. </p>
 */

public interface AdobeAdeptDRMClientType
{
  /**
   * <p> A particular call has completely finished executing. This method will
   * <i>always</i> be called at the end of an API call. </p>
   *
   * <p> This method exists because when using the Adobe API asynchronously (as
   * is practically required on Android), there is no documented way to
   * determine if the Adobe code has actually finished execution. The {@link
   * #onWorkflowsDone(int, byte[])} and {@link #onWorkflowError(int, String)}
   * methods will be called repeatedly with mostly undocumented values. </p>
   */

  void onCompletelyFinished();

  /**
   * A workflow has completed. This method may be called many times during a
   * single operation.
   *
   * @param w         The identifier of the workflow
   * @param remaining Any remaining data
   */

  void onWorkflowsDone(
    int w,
    byte[] remaining);

  /**
   * The progress of a workflow has changed.
   *
   * @param w        The identifier of the workflow
   * @param title    The title of the workflow
   * @param progress The current progress
   */

  void onWorkflowProgress(
    int w,
    String title,
    double progress);

  /**
   * An error has occurred during a workflow. Typically, this indicates that the
   * operation that triggered the workflow has failed.
   *
   * @param w    The identifier of the workflow
   * @param code The error message
   */

  void onWorkflowError(
    int w,
    String code);

  /**
   * Report a URL that should be opened to follow up the workflow.
   *
   * @param w   The identifier of the workflow
   * @param url The URL
   */

  void onFollowupURL(
    int w,
    String url);

  /**
   * Report that a new item has been fulfilled and downloaded to <tt>file</tt>.
   * Rights for the given file are given as the opaque bytes <tt>rights</tt>.
   *
   * @param file       The destination file
   * @param rights     Serialized rights for the downloaded item
   * @param returnable {@code true} iff the loan is returnable
   * @param loan_id    The loan ID
   */

  void onDownloadCompleted(
    String file,
    byte[] rights,
    boolean returnable,
    String loan_id);
}
