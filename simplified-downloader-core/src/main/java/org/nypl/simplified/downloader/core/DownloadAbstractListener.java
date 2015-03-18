package org.nypl.simplified.downloader.core;

public abstract class DownloadAbstractListener implements
  DownloadListenerType
{
  public DownloadAbstractListener()
  {
    // Nothing
  }

  @Override public void downloadCancelled(
    final DownloadSnapshot snap)
  {
    // Nothing
  }

  @Override public void downloadCleanedUp(
    final DownloadSnapshot snap)
  {
    // Nothing
  }

  @Override public void downloadCompleted(
    final DownloadSnapshot snap)
  {
    // Nothing
  }

  @Override public void downloadFailed(
    final DownloadSnapshot snap,
    final Throwable e)
  {
    // Nothing
  }

  @Override public void downloadPaused(
    final DownloadSnapshot snap)
  {
    // Nothing
  }

  @Override public void downloadResumed(
    final DownloadSnapshot snap)
  {
    // Nothing
  }

  @Override public void downloadStarted(
    final DownloadSnapshot snap)
  {
    // Nothing
  }

  @Override public void downloadStartedReceivingData(
    final DownloadSnapshot snap)
  {
    // Nothing
  }
}
