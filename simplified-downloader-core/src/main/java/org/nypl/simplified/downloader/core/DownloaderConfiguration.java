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

    Builder(
      final File in_directory)
    {
      this.directory = NullCheck.notNull(in_directory);
      this.buffer_size = 2 << 14;
    }

    @Override public DownloaderConfiguration build()
    {
      return new DownloaderConfiguration(this.directory, this.buffer_size);
    }

    @Override public void setBufferSize(
      final int s)
    {
      if (s < 1) {
        throw new IllegalArgumentException("Buffer size < 1");
      }

      this.buffer_size = s;
    }
  }

  public static DownloaderConfigurationBuilderType newBuilder(
    final File in_directory)
  {
    return new Builder(in_directory);
  }

  private final int  buffer_size;
  private final File directory;

  private DownloaderConfiguration(
    final File in_directory,
    final int in_buffer_size)
  {
    if (in_buffer_size < 1) {
      throw new IllegalArgumentException("Buffer size < 1");
    }

    this.directory = NullCheck.notNull(in_directory);
    this.buffer_size = in_buffer_size;
  }

  public int getBufferSize()
  {
    return this.buffer_size;
  }

  public File getDirectory()
  {
    return this.directory;
  }
}
