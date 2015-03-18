package org.nypl.simplified.downloader.core;

import java.io.Serializable;
import java.net.URI;

import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.io7m.junreachable.UnreachableCodeException;

public final class DownloadSnapshot implements Serializable
{
  private static final long           serialVersionUID = 1L;

  private final long                  cur_bytes;
  private final OptionType<Throwable> error;
  private final long                  id;
  private final long                  max_bytes;
  private final DownloadStatus        status;
  private final String                title;
  private final URI                   uri;

  public DownloadSnapshot(
    final long in_max_bytes,
    final long in_cur_bytes,
    final long in_id,
    final String in_title,
    final URI in_uri,
    final DownloadStatus in_status,
    final OptionType<Throwable> in_error)
  {
    this.max_bytes = in_max_bytes;
    this.cur_bytes = in_cur_bytes;
    this.id = in_id;
    this.title = NullCheck.notNull(in_title);
    this.uri = NullCheck.notNull(in_uri);
    this.status = NullCheck.notNull(in_status);
    this.error = NullCheck.notNull(in_error);
  }

  @Override public boolean equals(
    final @Nullable Object obj)
  {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (this.getClass() != obj.getClass()) {
      return false;
    }
    final DownloadSnapshot other = (DownloadSnapshot) obj;
    return (this.cur_bytes == other.cur_bytes)
      && (this.id == other.id)
      && (this.max_bytes == other.max_bytes)
      && this.title.equals(other.title)
      && this.uri.equals(other.uri)
      && this.status.equals(other.status)
      && this.error.equals(other.error);
  }

  public OptionType<Throwable> getError()
  {
    return this.error;
  }

  @Override public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result =
      (prime * result) + (int) (this.cur_bytes ^ (this.cur_bytes >>> 32));
    result = (prime * result) + (int) (this.id ^ (this.id >>> 32));
    result =
      (prime * result) + (int) (this.max_bytes ^ (this.max_bytes >>> 32));
    result = (prime * result) + this.title.hashCode();
    result = (prime * result) + this.uri.hashCode();
    result = (prime * result) + this.status.hashCode();
    result = (prime * result) + this.error.hashCode();
    return result;
  }

  public DownloadStatus statusGet()
  {
    return this.status;
  }

  public long statusGetCurrentBytes()
  {
    return this.cur_bytes;
  }

  public long statusGetID()
  {
    return this.id;
  }

  public long statusGetMaximumBytes()
  {
    return this.max_bytes;
  }

  public String statusGetTitle()
  {
    return this.title;
  }

  public URI statusGetURI()
  {
    return this.uri;
  }

  @Override public String toString()
  {
    switch (this.status) {
      case STATUS_CANCELLED:
      case STATUS_COMPLETED:
      case STATUS_IN_PROGRESS:
      case STATUS_PAUSED:
      {
        final String m =
          String.format(
            "[%d] (%d/%d) %s: %s",
            this.id,
            this.statusGetCurrentBytes(),
            this.statusGetMaximumBytes(),
            this.statusGet(),
            this.statusGetURI());
        return NullCheck.notNull(m);
      }
      case STATUS_FAILED:
      {
        final Some<Throwable> some_error = (Some<Throwable>) this.error;
        final Throwable e = some_error.get();

        final String m =
          String.format(
            "[%d] (%d/%d) %s: %s (%s - %s)",
            this.id,
            this.statusGetCurrentBytes(),
            this.statusGetMaximumBytes(),
            this.statusGet(),
            this.statusGetURI(),
            e.getClass().getName(),
            e.getMessage());
        return NullCheck.notNull(m);
      }
    }

    throw new UnreachableCodeException();
  }
}
