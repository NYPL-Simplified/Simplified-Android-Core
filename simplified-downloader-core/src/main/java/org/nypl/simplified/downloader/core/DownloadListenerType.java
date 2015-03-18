package org.nypl.simplified.downloader.core;

public interface DownloadListenerType
{
  void downloadStarted(
    final DownloadSnapshot snap);

  void downloadStartedReceivingData(
    final DownloadSnapshot snap);

  void downloadCompleted(
    final DownloadSnapshot snap);

  void downloadCancelled(
    final DownloadSnapshot snap);

  void downloadPaused(
    final DownloadSnapshot snap);

  void downloadResumed(
    final DownloadSnapshot snap);

  void downloadFailed(
    final DownloadSnapshot snap,
    final Throwable e);

  void downloadCleanedUp(
    final DownloadSnapshot snap);
}
