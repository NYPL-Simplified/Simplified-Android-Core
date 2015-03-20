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
import java.nio.channels.FileChannel;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.nypl.simplified.http.core.HTTPAuthType;
import org.nypl.simplified.http.core.HTTPResultError;
import org.nypl.simplified.http.core.HTTPResultException;
import org.nypl.simplified.http.core.HTTPResultMatcherType;
import org.nypl.simplified.http.core.HTTPResultOKType;
import org.nypl.simplified.http.core.HTTPResultType;
import org.nypl.simplified.http.core.HTTPType;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jfunctional.Unit;
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

@SuppressWarnings({ "boxing", "synthetic-access" }) public final class Downloader implements
  DownloaderType
{
  private abstract static class DownloadErrorFlattener<A, B> implements
    HTTPResultMatcherType<A, B, Exception>
  {
    public DownloadErrorFlattener()
    {

    }

    @Override public final B onHTTPError(
      final HTTPResultError<A> e)
      throws Exception
    {
      final String m = String.format("%d: %s", e.getStatus(), e.getMessage());
      throw new IOException(NullCheck.notNull(m));
    }

    @Override public final B onHTTPException(
      final HTTPResultException<A> e)
      throws Exception
    {
      throw e.getError();
    }
  }

  private final static class DownloadInfo implements Serializable
  {
    private static final long serialVersionUID = 3L;

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

    private final OptionType<HTTPAuthType> auth;
    private final long                     bytes_max;
    private final long                     id;
    private final DownloadStatus           status;
    private final String                   title;
    private final URI                      uri;

    public DownloadInfo(
      final long in_id,
      final OptionType<HTTPAuthType> in_auth,
      final String in_title,
      final URI in_uri,
      final long in_bytes_max,
      final DownloadStatus in_status)
    {
      this.id = in_id;
      this.auth = NullCheck.notNull(in_auth);
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
    @SuppressWarnings("resource") private static long fileStreamSize(
      final FileOutputStream fs)
      throws IOException
    {
      final FileChannel channel = fs.getChannel();
      return channel.size();
    }

    private final OptionType<HTTPAuthType> auth;
    private final byte[]                   buffer;
    private volatile long                  bytes_cur;
    private volatile long                  bytes_max;
    private final DownloaderConfiguration  config;
    private volatile @Nullable Throwable   error;
    private final File                     file_data;
    private final File                     file_data_tmp;
    private final File                     file_info;
    private final File                     file_info_tmp;
    private final HTTPType                 http;
    private final long                     id;
    private final DownloadListenerType     listener;
    private volatile DownloadStatus        status;
    private final String                   title;
    private final URI                      uri;
    private final AtomicBoolean            want_cancel;
    private final AtomicBoolean            want_pause;

    public DownloadTask(
      final HTTPType in_http,
      final long in_id,
      final OptionType<HTTPAuthType> in_auth,
      final URI in_uri,
      final String in_title,
      final DownloadStatus in_status,
      final DownloaderConfiguration in_config,
      final DownloadListenerType in_listener)
    {
      this.id = in_id;
      this.auth = NullCheck.notNull(in_auth);
      this.http = NullCheck.notNull(in_http);
      this.uri = NullCheck.notNull(in_uri);
      this.title = NullCheck.notNull(in_title);
      this.config = NullCheck.notNull(in_config);
      this.status = NullCheck.notNull(in_status);
      this.listener = NullCheck.notNull(in_listener);

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
      final HTTPResultOKType<InputStream> result =
        this.downloadFileFollowingRedirects();
      try {

        final InputStream is = result.getValue();

        try {
          final FileOutputStream fs =
            new FileOutputStream(this.file_data_tmp, true);

          try {
            this.bytes_cur = DownloadTask.fileStreamSize(fs);

            final BufferedOutputStream out =
              new BufferedOutputStream(fs, this.config.getBufferSize());

            try {
              this.listener.downloadStartedReceivingData(this.getStatus());
            } catch (final Throwable x) {
              // Ignore
            }

            try {
              for (;;) {
                if (this.wantPauseOrCancel()) {
                  return;
                }

                final int r = is.read(this.buffer);
                if (r == -1) {
                  break;
                }
                this.bytes_cur += r;
                out.write(this.buffer, 0, r);
              }
            } finally {
              out.flush();
              out.close();
            }

          } finally {
            fs.flush();
            fs.close();
          }

        } finally {
          is.close();
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

      } finally {
        result.close();
      }
    }

    private HTTPResultOKType<InputStream> downloadFileFollowingRedirects()
      throws Exception
    {
      final HTTPResultOKType<InputStream> result =
        new LinkFollower(this.http, this.auth, 5, this.uri, this.bytes_cur)
          .call();

      this.bytes_max = result.getContentLength();
      this.downloadSaveState();
      return result;
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
          this.auth,
          this.title,
          this.uri,
          this.bytes_max,
          this.status);
      state.save(this.config.getDirectory());
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
        case STATUS_IN_PROGRESS_RESUMED:
        {
          try {
            switch (this.status) {
              case STATUS_IN_PROGRESS:
              {
                try {
                  this.listener.downloadStarted(this.getStatus());
                } catch (final Throwable e) {
                  // Ignored
                }
                break;
              }
              case STATUS_IN_PROGRESS_RESUMED:
              {
                try {
                  this.listener.downloadResumed(this.getStatus());
                } catch (final Throwable e) {
                  // Ignored
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

            this.downloadMakeDirectory();
            this.downloadFile();

            if (this.want_cancel.get()) {
              this.status = DownloadStatus.STATUS_CANCELLED;
              try {
                this.listener.downloadCancelled(this.getStatus());
              } catch (final Throwable e) {
                // Ignored
              }
            } else if (this.want_pause.get()) {
              this.status = DownloadStatus.STATUS_PAUSED;
              try {
                this.listener.downloadPaused(this.getStatus());
              } catch (final Throwable e) {
                // Ignored
              }
            } else {
              this.status = DownloadStatus.STATUS_COMPLETED;
              try {
                this.listener.downloadCompleted(this.getStatus());
              } catch (final Throwable e) {
                // Ignored
              }
            }

          } catch (final Throwable e) {
            this.status = DownloadStatus.STATUS_FAILED;
            this.error = e;
            try {
              this.listener.downloadFailed(this.getStatus(), e);
            } catch (final Throwable x) {
              // Ignored
            }
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

          try {
            this.listener.downloadCleanedUp(this.getStatus());
          } catch (final Throwable x) {
            // Ignore
          }

          return;
        }
        case STATUS_COMPLETED:
        {
          this.file_data_tmp.delete();
          return;
        }
        case STATUS_IN_PROGRESS:
        case STATUS_IN_PROGRESS_RESUMED:
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

  private final static class LinkFollower implements
    Callable<HTTPResultOKType<InputStream>>,
    HTTPResultMatcherType<Unit, Unit, Exception>
  {
    private final long                     byte_offset;
    private int                            cur_redirects;
    private OptionType<HTTPAuthType>       current_auth;
    private URI                            current_uri;
    private final HTTPType                 http;
    private final int                      max_redirects;
    private final OptionType<HTTPAuthType> target_auth;
    private final Set<URI>                 tried_auth;

    public LinkFollower(
      final HTTPType in_http,
      final OptionType<HTTPAuthType> in_auth,
      final int in_max_redirects,
      final URI in_uri,
      final long in_byte_offset)
    {
      this.http = NullCheck.notNull(in_http);
      this.target_auth = NullCheck.notNull(in_auth);
      this.current_auth = Option.none();
      this.current_uri = NullCheck.notNull(in_uri);
      this.max_redirects = in_max_redirects;
      this.cur_redirects = 0;
      this.byte_offset = in_byte_offset;
      this.tried_auth = new HashSet<URI>();
    }

    @Override public HTTPResultOKType<InputStream> call()
      throws Exception
    {
      this.processURI();

      final HTTPResultType<InputStream> r =
        this.http.get(this.current_auth, this.current_uri, this.byte_offset);

      return r
        .matchResult(new DownloadErrorFlattener<InputStream, HTTPResultOKType<InputStream>>() {
          @Override public HTTPResultOKType<InputStream> onHTTPOK(
            final HTTPResultOKType<InputStream> e)
            throws Exception
          {
            return e;
          }
        });
    }

    @Override public Unit onHTTPError(
      final HTTPResultError<Unit> e)
      throws Exception
    {
      switch (e.getStatus()) {
        case HttpURLConnection.HTTP_UNAUTHORIZED:
        {
          if (this.tried_auth.contains(this.current_uri)) {
            final String m =
              String.format("%d: %s", e.getStatus(), e.getMessage());
            throw new DownloadAuthenticationError(NullCheck.notNull(m));
          }

          this.current_auth = this.target_auth;
          this.tried_auth.add(this.current_uri);
          this.processURI();
          return Unit.unit();
        }
      }

      final String m = String.format("%d: %s", e.getStatus(), e.getMessage());
      throw new IOException(NullCheck.notNull(m));
    }

    @Override public Unit onHTTPException(
      final HTTPResultException<Unit> e)
      throws Exception
    {
      throw e.getError();
    }

    @Override public Unit onHTTPOK(
      final HTTPResultOKType<Unit> e)
      throws Exception
    {
      switch (e.getStatus()) {
        case HttpURLConnection.HTTP_OK:
        {
          return Unit.unit();
        }

        case HttpURLConnection.HTTP_MOVED_PERM:
        case HttpURLConnection.HTTP_MOVED_TEMP:
        case 307:
        case 308:
        {
          this.current_auth = Option.none();

          final Map<String, List<String>> headers =
            NullCheck.notNull(e.getResponseHeaders());
          final List<String> locations =
            NullCheck.notNull(headers.get("Location"));

          if (locations.size() != 1) {
            throw new IOException(
              "Malformed server response: Expected exactly one Location header");
          }

          final String location = NullCheck.notNull(locations.get(0));
          this.cur_redirects = this.cur_redirects + 1;
          this.current_uri = NullCheck.notNull(URI.create(location));
          this.processURI();
          return Unit.unit();
        }
      }

      throw new IOException(String.format(
        "Unhandled http code (%d: %s)",
        e.getStatus(),
        e.getMessage()));
    }

    private void processURI()
      throws IOException,
        Exception
    {
      if (this.cur_redirects >= this.max_redirects) {
        throw new IOException("Reached redirect limit");
      }

      final HTTPResultType<Unit> r =
        this.http.head(this.current_auth, this.current_uri);
      r.matchResult(this);
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
    final HTTPType in_http,
    final DownloaderConfiguration in_config)
  {
    return new Downloader(
      in_exec,
      in_http,
      in_config,
      new DownloadEmptyListener());
  }

  public static DownloaderType newDownloaderWithListener(
    final ExecutorService in_exec,
    final HTTPType in_http,
    final DownloaderConfiguration in_config,
    final DownloadListenerType in_default_listener)
  {
    return new Downloader(in_exec, in_http, in_config, in_default_listener);
  }

  private final DownloaderConfiguration config;
  private final DownloadListenerType    default_listener;
  private final ExecutorService         exec;
  private final Map<Long, Future<?>>    futures;
  private final HTTPType                http;
  private final AtomicLong              id_pool;
  private final Map<Long, DownloadTask> tasks;

  private Downloader(
    final ExecutorService in_exec,
    final HTTPType in_http,
    final DownloaderConfiguration in_config,
    final DownloadListenerType in_default_listener)
  {
    this.config = NullCheck.notNull(in_config);
    this.http = NullCheck.notNull(in_http);
    this.exec = NullCheck.notNull(in_exec);
    this.default_listener = NullCheck.notNull(in_default_listener);

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

        DownloadStatus s = null;
        switch (i.status) {
          case STATUS_CANCELLED:
          case STATUS_COMPLETED:
          case STATUS_FAILED:
          case STATUS_PAUSED:
          {
            s = i.status;
            break;
          }
          case STATUS_IN_PROGRESS:
          case STATUS_IN_PROGRESS_RESUMED:
          {
            s = DownloadStatus.STATUS_IN_PROGRESS_RESUMED;
            break;
          }
        }

        this.downloadEnqueueWithID(
          i.id,
          i.auth,
          i.uri,
          i.title,
          NullCheck.notNull(s),
          in_default_listener);
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
          task.auth,
          task.uri,
          task.title,
          DownloadStatus.STATUS_CANCELLED,
          task.listener);
      }
    }
  }

  @Override public long downloadEnqueue(
    final OptionType<HTTPAuthType> auth,
    final URI uri,
    final String title,
    final DownloadListenerType listener)
  {
    NullCheck.notNull(auth);
    NullCheck.notNull(uri);
    NullCheck.notNull(title);
    NullCheck.notNull(listener);

    final long id = this.id_pool.incrementAndGet();
    this.downloadEnqueueWithID(
      id,
      auth,
      uri,
      title,
      DownloadStatus.STATUS_IN_PROGRESS,
      listener);
    return id;
  }

  private void downloadEnqueueWithID(
    final long id,
    final OptionType<HTTPAuthType> auth,
    final URI uri,
    final String title,
    final DownloadStatus status,
    final DownloadListenerType listener)
  {
    synchronized (this.tasks) {
      final DownloadTask d =
        new DownloadTask(
          this.http,
          id,
          auth,
          uri,
          title,
          status,
          this.config,
          listener);
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
            t.auth,
            t.uri,
            t.title,
            DownloadStatus.STATUS_IN_PROGRESS_RESUMED,
            t.listener);
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
