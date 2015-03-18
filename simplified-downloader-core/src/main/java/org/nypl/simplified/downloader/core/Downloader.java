package org.nypl.simplified.downloader.core;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

/**
 * <p>
 * Default implementation of the {@link DownloaderType} interface.
 * </p>
 * <p>
 * Note: This implementation assumes that file renames within the same
 * directory are atomic. This is true of just about anything POSIX-like.
 * </p>
 */

@SuppressWarnings("synthetic-access") public final class Downloader implements
  DownloaderType
{
  private final static class DownloadInfo implements Serializable
  {
    private static final long serialVersionUID = 2L;

    public static OptionType<DownloadInfo> loadFromFile(
      final File file)
    {
      ObjectInputStream is = null;
      try {
        is = new ObjectInputStream(new FileInputStream(file));
        final DownloadInfo state =
          NullCheck.notNull((DownloadInfo) is.readObject());
        return Option.some(state);
      } catch (final Throwable e) {
        return Option.none();
      } finally {
        try {
          if (is != null) {
            is.close();
          }
        } catch (final IOException e) {
          // Don't care
        }
      }
    }

    private final long           bytes_max;
    private final long           id;
    private final DownloadStatus status;
    private final String         title;
    private final URI            uri;

    public DownloadInfo(
      final long in_id,
      final String in_title,
      final URI in_uri,
      final long in_bytes_max,
      final DownloadStatus in_status)
    {
      this.id = in_id;
      this.title = NullCheck.notNull(in_title);
      this.uri = NullCheck.notNull(in_uri);
      this.bytes_max = in_bytes_max;
      this.status = NullCheck.notNull(in_status);
    }

    public void save(
      final File directory)
      throws IOException
    {
      final File file_info = Downloader.makeFilenameInfo(directory, this.id);
      final File file_info_tmp = new File(directory, this.id + ".info_tmp");

      final ObjectOutputStream os =
        new ObjectOutputStream(new FileOutputStream(file_info_tmp, false));
      os.writeObject(this);
      os.flush();
      os.close();

      if (file_info_tmp.renameTo(file_info) == false) {
        file_info.delete();
        file_info_tmp.delete();
        throw new IOException("Could not save download state");
      }
    }
  }

  private final static class DownloadTask implements Runnable
  {
    private final byte[]                  buffer;
    private volatile long                 bytes_cur;
    private volatile long                 bytes_max;
    private final DownloaderConfiguration config;
    private volatile @Nullable Throwable  error;
    private final File                    file_data;
    private final File                    file_data_tmp;
    private final File                    file_info;
    private final File                    file_info_tmp;
    private final long                    id;
    private volatile DownloadStatus       status;
    private final String                  title;
    private final URI                     uri;
    private final AtomicBoolean           want_cancel;
    private final AtomicBoolean           want_pause;

    public DownloadTask(
      final long in_id,
      final URI in_uri,
      final String in_title,
      final DownloadStatus in_status,
      final DownloaderConfiguration in_config)
    {
      this.id = in_id;
      this.uri = NullCheck.notNull(in_uri);
      this.title = NullCheck.notNull(in_title);
      this.config = NullCheck.notNull(in_config);
      this.status = NullCheck.notNull(in_status);
      this.buffer = new byte[this.config.getBufferSize()];
      this.bytes_max = -1;
      this.bytes_cur = 0;
      this.want_cancel = new AtomicBoolean(false);
      this.want_pause = new AtomicBoolean(false);

      this.file_data_tmp =
        Downloader.makeFilenameDataTemporary(in_config.getDirectory(), in_id);
      this.file_data =
        Downloader.makeFilenameData(in_config.getDirectory(), in_id);
      this.file_info =
        Downloader.makeFilenameInfo(in_config.getDirectory(), in_id);
      this.file_info_tmp =
        Downloader.makeFilenameInfoTemporary(in_config.getDirectory(), in_id);
    }

    public void cancel()
    {
      this.want_cancel.set(true);
    }

    private void downloadFile()
      throws Exception
    {
      final FileOutputStream fs =
        new FileOutputStream(this.file_data_tmp, true);
      final FileChannel channel = fs.getChannel();
      this.bytes_cur = channel.size();

      final BufferedOutputStream out =
        new BufferedOutputStream(fs, this.config.getBufferSize());

      try {
        this.downloadToStream(out);
      } finally {
        out.flush();
        out.close();
      }

      if (this.wantPauseOrCancel()) {
        return;
      }

      if (this.file_data_tmp.renameTo(this.file_data) == false) {
        this.file_data_tmp.delete();
        this.file_data.delete();
        final String m =
          String.format(
            "Could not rename %s to %s",
            this.file_data_tmp,
            this.file_data);
        throw new IOException(NullCheck.notNull(m));
      }
    }

    private void downloadGetRemoteSize()
      throws Exception
    {
      final URL url = NullCheck.notNull(this.uri.toURL());
      final HttpURLConnection conn =
        NullCheck.notNull((HttpURLConnection) url.openConnection());

      try {
        conn.setRequestMethod("HEAD");
        conn.setReadTimeout(10000);
        conn.connect();

        if (conn.getResponseCode() >= 400) {
          final String m =
            String.format(
              "Error fetching file: %s",
              conn.getResponseMessage());
          throw new IOException(NullCheck.notNull(m));
        }

        this.bytes_max = conn.getContentLength();
        this.downloadSaveState();
      } finally {
        conn.disconnect();
      }
    }

    private void downloadLoadSize()
    {
      final OptionType<DownloadInfo> iopt =
        DownloadInfo.loadFromFile(this.file_info);

      if (iopt.isSome()) {
        final Some<DownloadInfo> some = (Some<DownloadInfo>) iopt;
        final DownloadInfo i = some.get();
        this.bytes_max = i.bytes_max;

        if (this.file_data.exists()) {
          this.bytes_cur = this.file_data.length();
        }
        if (this.file_data_tmp.exists()) {
          this.bytes_cur = this.file_data_tmp.length();
        }
      }
    }

    private void downloadMakeDirectory()
      throws Exception
    {
      final File directory = this.config.getDirectory();
      directory.mkdirs();
      if (directory.isDirectory() == false) {
        throw new IOException("Not a directory: " + directory);
      }
    }

    private void downloadSaveState()
      throws IOException
    {
      final DownloadInfo state =
        new DownloadInfo(
          this.id,
          this.title,
          this.uri,
          this.bytes_max,
          this.status);
      state.save(this.config.getDirectory());
    }

    private void downloadToStream(
      final BufferedOutputStream out)
      throws Exception
    {
      final URL url = NullCheck.notNull(this.uri.toURL());
      final HttpURLConnection conn =
        NullCheck.notNull((HttpURLConnection) url.openConnection());

      try {
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("Range", "bytes=" + this.bytes_cur + "-");
        conn.connect();

        if (conn.getResponseCode() >= 400) {
          final String m =
            String.format(
              "Error fetching file: %s",
              conn.getResponseMessage());
          throw new IOException(NullCheck.notNull(m));
        }

        this.downloadSaveState();

        final InputStream s = conn.getInputStream();
        try {
          for (;;) {
            if (this.wantPauseOrCancel()) {
              return;
            }

            final int r = s.read(this.buffer);
            if (r == -1) {
              break;
            }
            this.bytes_cur += r;
            out.write(this.buffer, 0, r);
          }
        } finally {
          s.close();
        }
      } finally {
        conn.disconnect();
      }
    }

    public DownloadSnapshot getStatus()
    {
      return new DownloadSnapshot(
        this.bytes_max,
        this.bytes_cur,
        this.id,
        this.title,
        this.uri,
        this.status,
        Option.of(this.error));
    }

    public void pause()
    {
      this.want_pause.set(true);
    }

    @Override public void run()
    {
      /**
       * Attempt to load the size of the data on disk, if any.
       */

      this.downloadLoadSize();

      /**
       * If there's actually something to do, do it.
       */

      switch (this.status) {
        case STATUS_IN_PROGRESS:
        {
          try {
            this.downloadMakeDirectory();
            this.downloadGetRemoteSize();
            this.downloadFile();

            if (this.want_cancel.get()) {
              this.status = DownloadStatus.STATUS_CANCELLED;
            } else if (this.want_pause.get()) {
              this.status = DownloadStatus.STATUS_PAUSED;
            } else {
              this.status = DownloadStatus.STATUS_COMPLETED;
            }

          } catch (final Throwable e) {
            this.status = DownloadStatus.STATUS_FAILED;
            this.error = e;
          } finally {
            try {
              this.downloadSaveState();
            } catch (final IOException x) {
              // Nothing can be done about it...
            }
          }
          break;
        }
        case STATUS_CANCELLED:
        case STATUS_COMPLETED:
        case STATUS_FAILED:
        case STATUS_PAUSED:
        {
          break;
        }
      }

      /**
       * Clean up any temporary files based on the final task status. If the
       * task was cancelled or failed, delete everything.
       */

      switch (this.status) {
        case STATUS_CANCELLED:
        case STATUS_FAILED:
        {
          this.file_data.delete();
          this.file_data_tmp.delete();
          this.file_info.delete();
          this.file_info_tmp.delete();
          return;
        }
        case STATUS_COMPLETED:
        {
          this.file_data_tmp.delete();
          return;
        }
        case STATUS_IN_PROGRESS:
        case STATUS_PAUSED:
        {
          break;
        }
      }
    }

    private boolean wantPauseOrCancel()
    {
      return this.want_cancel.get() || this.want_pause.get();
    }
  }

  private static File makeFilenameData(
    final File in_directory,
    final long in_id)
  {
    return new File(in_directory, in_id + ".data");
  }

  private static File makeFilenameDataTemporary(
    final File in_directory,
    final long in_id)
  {
    return new File(in_directory, in_id + ".tmp");
  }

  private static File makeFilenameInfo(
    final File in_directory,
    final long in_id)
  {
    return new File(in_directory, in_id + ".info");
  }

  private static File makeFilenameInfoTemporary(
    final File in_directory,
    final long in_id)
  {
    return new File(in_directory, in_id + ".info_tmp");
  }

  public static DownloaderType newDownloader(
    final ExecutorService in_exec,
    final DownloaderConfiguration in_config)
  {
    return new Downloader(in_exec, in_config);
  }

  private final DownloaderConfiguration config;
  private final ExecutorService         exec;
  private final Map<Long, Future<?>>    futures;
  private final AtomicLong              id_pool;
  private final Map<Long, DownloadTask> tasks;

  private Downloader(
    final ExecutorService in_exec,
    final DownloaderConfiguration in_config)
  {
    this.config = NullCheck.notNull(in_config);
    this.exec = NullCheck.notNull(in_exec);
    this.id_pool = new AtomicLong(0);
    this.tasks = new HashMap<Long, DownloadTask>();
    this.futures = new HashMap<Long, Future<?>>();

    final File[] files =
      this.config.getDirectory().listFiles(new FileFilter() {
        @Override public boolean accept(
          final @Nullable File f)
        {
          assert f != null;
          return f.getName().endsWith(".info");
        }
      });

    for (final File f : files) {
      assert f != null;
      final OptionType<DownloadInfo> iopt = DownloadInfo.loadFromFile(f);
      if (iopt.isSome()) {
        final Some<DownloadInfo> some = (Some<DownloadInfo>) iopt;
        final DownloadInfo i = some.get();
        this.id_pool.set(Long.max(this.id_pool.get(), i.id));
        this.downloadEnqueueWithID(i.id, i.uri, i.title, i.status);
      }
    }
  }

  @Override public void downloadAcknowledge(
    final long id)
  {
    synchronized (this.tasks) {
      final Long lid = Long.valueOf(id);
      if (this.tasks.containsKey(lid)) {
        final DownloadTask t = NullCheck.notNull(this.tasks.get(lid));
        final Future<?> f = NullCheck.notNull(this.futures.get(lid));
        if (t.status.isComplete()) {
          this.tasks.remove(lid);
          this.futures.remove(lid);
          f.cancel(true);
        }
      }
    }
  }

  @Override public void downloadCancel(
    final long id)
  {
    synchronized (this.tasks) {
      final Long lid = Long.valueOf(id);
      if (this.tasks.containsKey(lid)) {
        final DownloadTask t = NullCheck.notNull(this.tasks.get(lid));
        t.cancel();
      }
    }
  }

  @Override public void downloadDestroyAll()
  {
    synchronized (this.tasks) {
      final Iterator<Long> iter = this.tasks.keySet().iterator();
      while (iter.hasNext()) {
        final Long id = iter.next();
        final Future<?> f = this.futures.get(id);
        final DownloadTask task = this.tasks.get(id);
        f.cancel(true);
        this.downloadEnqueueWithID(
          task.id,
          task.uri,
          task.title,
          DownloadStatus.STATUS_CANCELLED);
      }
    }
  }

  @Override public long downloadEnqueue(
    final URI uri,
    final String title)
  {
    final String scheme = uri.getScheme();
    if ("http".equals(scheme) || "https".equals(scheme)) {
      final long id = this.id_pool.incrementAndGet();
      this.downloadEnqueueWithID(
        id,
        uri,
        title,
        DownloadStatus.STATUS_IN_PROGRESS);
      return id;
    }

    final StringBuilder m = new StringBuilder();
    m.append("Unsupported URI scheme.\n");
    m.append("  URI scheme: ");
    m.append(scheme);
    m.append("\n");
    m.append("  Supported schemes: http https\n");
    throw new IllegalArgumentException(m.toString());
  }

  private void downloadEnqueueWithID(
    final long id,
    final URI uri,
    final String title,
    final DownloadStatus status)
  {
    synchronized (this.tasks) {
      final DownloadTask d =
        new DownloadTask(id, uri, title, status, this.config);
      this.tasks.put(Long.valueOf(id), d);
      final Future<?> f = this.exec.submit(d);
      this.futures.put(Long.valueOf(id), f);
    }
  }

  @Override public void downloadPause(
    final long id)
  {
    synchronized (this.tasks) {
      final Long lid = Long.valueOf(id);
      if (this.tasks.containsKey(lid)) {
        final DownloadTask t = NullCheck.notNull(this.tasks.get(lid));
        t.pause();
      }
    }
  }

  @Override public void downloadResume(
    final long id)
  {
    synchronized (this.tasks) {
      final Long lid = Long.valueOf(id);
      if (this.tasks.containsKey(lid)) {
        final DownloadTask t = NullCheck.notNull(this.tasks.get(lid));
        if (t.status == DownloadStatus.STATUS_PAUSED) {
          this.downloadEnqueueWithID(
            t.id,
            t.uri,
            t.title,
            DownloadStatus.STATUS_IN_PROGRESS);
        }
      }
    }
  }

  @Override public OptionType<DownloadSnapshot> downloadStatusSnapshot(
    final long id)
  {
    synchronized (this.tasks) {
      final Long lid = Long.valueOf(id);
      if (this.tasks.containsKey(lid)) {
        final DownloadTask task = this.tasks.get(id);
        final DownloadSnapshot status = task.getStatus();
        return Option.some(status);
      }
      return Option.none();
    }
  }

  @Override public Map<Long, DownloadSnapshot> downloadStatusSnapshotAll()
  {
    final Map<Long, DownloadSnapshot> r =
      new HashMap<Long, DownloadSnapshot>();

    synchronized (this.tasks) {
      final Iterator<Long> iter = this.tasks.keySet().iterator();
      while (iter.hasNext()) {
        final Long id = iter.next();
        final DownloadTask task = this.tasks.get(id);
        final DownloadSnapshot status = task.getStatus();
        r.put(id, status);
      }
    }

    return NullCheck.notNull(Collections.unmodifiableMap(r));
  }
}
