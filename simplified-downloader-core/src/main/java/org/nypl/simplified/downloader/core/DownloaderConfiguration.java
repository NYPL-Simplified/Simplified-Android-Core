package org.nypl.simplified.downloader.core;

import java.io.File;

import com.io7m.jnull.NullCheck;

@SuppressWarnings("synthetic-access") public final class DownloaderConfiguration
{
  private static final class Builder implements
    DownloaderConfigurationBuilderType
  {
    private int        buffer_size;
    private final File directory;
    private long       sleep_ms;

    Builder(
      final File in_directory)
    {
      this.directory = NullCheck.notNull(in_directory);
      this.buffer_size = 2 << 14;
      this.sleep_ms = 0;
    }

    @Override public DownloaderConfiguration build()
    {
      return new DownloaderConfiguration(
        this.directory,
        this.buffer_size,
        this.sleep_ms);
    }

    @Override public void setBufferSize(
      final int s)
    {
      if (s < 1) {
        throw new IllegalArgumentException("Buffer size < 1");
      }

      this.buffer_size = s;
    }

    @Override public void setReadSleepTime(
      final long ms)
    {
      this.sleep_ms = ms;
    }
  }

  public static DownloaderConfigurationBuilderType newBuilder(
    final File in_directory)
  {
    return new Builder(in_directory);
  }

  private final int  buffer_size;
  private final File directory;
  private final long sleep_ms;

  private DownloaderConfiguration(
    final File in_directory,
    final int in_buffer_size,
    final long in_sleep_ms)
  {
    if (in_buffer_size < 1) {
      throw new IllegalArgumentException("Buffer size < 1");
    }
    if (in_sleep_ms < 0) {
      throw new IllegalArgumentException("Sleep time < 0");
    }

    this.directory = NullCheck.notNull(in_directory);
    this.buffer_size = in_buffer_size;
    this.sleep_ms = in_sleep_ms;
  }

  public int getBufferSize()
  {
    return this.buffer_size;
  }

  public File getDirectory()
  {
    return this.directory;
  }

  public long getSleepTime()
  {
    return this.sleep_ms;
  }
}
