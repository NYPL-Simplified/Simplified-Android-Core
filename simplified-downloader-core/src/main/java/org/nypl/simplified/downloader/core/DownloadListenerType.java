package org.nypl.simplified.downloader.core;

public interface DownloadListenerType
{
  void downloadCancelled(
    final DownloadSnapshot snap);

  void downloadCleanedUp(
    final DownloadSnapshot snap);

  void downloadCompleted(
    final DownloadSnapshot snap);

  void downloadFailed(
    final DownloadSnapshot snap,
    final Throwable e);

  void downloadPaused(
    final DownloadSnapshot snap);

  void downloadResumed(
    final DownloadSnapshot snap);

  void downloadStarted(
    final DownloadSnapshot snap);

  void downloadStartedReceivingData(
    final DownloadSnapshot snap);
}
