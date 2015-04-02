package org.nypl.simplified.downloader.core;

public interface DownloaderConfigurationBuilderType
{
  DownloaderConfiguration build();

  void setBufferSize(
    int s);

  void setReadSleepTime(
    long ms);
}
