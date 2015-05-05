package org.nypl.simplified.downloader.core;

import java.io.File;

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

  @Override public void downloadCompletedTaken(
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

  @Override public void downloadCompletedTake(
    final DownloadSnapshot snap,
    final File file_data)
  {
    // Nothing
  }

  @Override public void downloadCompletedTakeFailed(
    final DownloadSnapshot snap,
    final Throwable x)
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

  @Override public void downloadReceivedData(
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
