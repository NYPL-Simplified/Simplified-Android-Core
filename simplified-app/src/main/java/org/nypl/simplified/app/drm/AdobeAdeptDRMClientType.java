package org.nypl.simplified.app.drm;

/**
 * Callback functions for the DRM client.
 */

public interface AdobeAdeptDRMClientType
{
  void onWorkflowsDone(
    int w,
    String remaining);

  void onWorkflowProgress(
    int w,
    String title,
    double progress);

  void onWorkflowError(
    int w,
    String code);

  void onFollowupURL(
    int w,
    String url);

  void onDownloadCompleted(
    String url);
}
