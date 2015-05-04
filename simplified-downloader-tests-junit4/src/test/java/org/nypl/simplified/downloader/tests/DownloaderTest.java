package org.nypl.simplified.downloader.tests;

import java.io.File;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.nypl.simplified.downloader.core.DownloadAbstractListener;
import org.nypl.simplified.downloader.core.DownloadListenerType;
import org.nypl.simplified.downloader.core.DownloadSnapshot;
import org.nypl.simplified.downloader.core.DownloadStatus;
import org.nypl.simplified.downloader.core.Downloader;
import org.nypl.simplified.downloader.core.DownloaderConfiguration;
import org.nypl.simplified.downloader.core.DownloaderConfigurationBuilderType;
import org.nypl.simplified.downloader.core.DownloaderType;
import org.nypl.simplified.http.core.HTTP;
import org.nypl.simplified.http.core.HTTPAuthType;
import org.nypl.simplified.http.core.HTTPType;

import com.google.common.io.Files;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NonNull;
import com.io7m.jnull.NullCheck;

@SuppressWarnings("static-method") public final class DownloaderTest
{
  private static abstract class LoggingListener implements
    DownloadListenerType
  {
    @Override public void downloadCancelled(
      final DownloadSnapshot snap)
    {
      System.out.println("CANCELLED: " + snap);
    }

    @Override public void downloadCleanedUp(
      final DownloadSnapshot snap)
    {
      System.out.println("CLEANUP: " + snap);
    }

    @Override public void downloadCompleted(
      final DownloadSnapshot snap)
    {
      System.out.println("COMPLETED: " + snap);
    }

    @Override public void downloadCompletedTake(
      final DownloadSnapshot snap,
      final File file_data)
    {
      System.out.println("DOWNLOAD TAKE: " + snap + " " + file_data);
    }

    @Override public void downloadCompletedTakeFailed(
      final DownloadSnapshot snap,
      final Throwable x)
    {
      System.out.println("DOWNLOAD TAKE FAILED: " + snap + " " + x);
    }

    @Override public void downloadCompletedTaken(
      final DownloadSnapshot snap)
    {
      System.out.println("COMPLETED TAKEN: " + snap);
    }

    @Override public void downloadFailed(
      final DownloadSnapshot snap,
      final Throwable e)
    {
      System.out.println("FAILED: " + snap);
    }

    @Override public void downloadPaused(
      final DownloadSnapshot snap)
    {
      System.out.println("PAUSED: " + snap);
    }

    @Override public void downloadReceivedData(
      final DownloadSnapshot snap)
    {
      System.out.println("RECEIVED DATA: " + snap);
    }

    @Override public void downloadResumed(
      final DownloadSnapshot snap)
    {
      System.out.println("RESUMED: " + snap);
    }

    @Override public void downloadStarted(
      final DownloadSnapshot snap)
    {
      System.out.println("STARTED: " + snap);
    }

    @Override public void downloadStartedReceivingData(
      final DownloadSnapshot snap)
    {
      System.out.println("STARTED DATA: " + snap);
    }
  }

  private static int   SERVER_PORT;

  @Rule public Timeout globalTimeout = new Timeout(10000);

  private @NonNull File makeTempDir()
  {
    final File dir = Files.createTempDir();
    System.out.printf("temporary directory: %s\n", dir);
    return NullCheck.notNull(dir);
  }

  private @NonNull URI serverAddress(
    final String path)
  {
    final StringBuilder b = new StringBuilder();
    b.append("http://127.0.0.1:");
    b.append(DownloaderTest.SERVER_PORT);
    b.append(path);
    return NullCheck.notNull(URI.create(b.toString()));
  }

  @Before public void setup()
  {
    final String s =
      System.getProperty("org.nypl.simplified.tests.http_server_port");
    if (s == null) {
      throw new IllegalStateException(
        "Property 'org.nypl.simplified.tests.http_server_port' is not set");
    }

    DownloaderTest.SERVER_PORT = Integer.valueOf(s).intValue();
  }

  @Test public void testDownloadCancel()
    throws Exception
  {
    final HTTPType h = HTTP.newHTTP();
    final ExecutorService exec = Executors.newFixedThreadPool(8);
    final CountDownLatch latch = new CountDownLatch(1);
    final FileServer s = new FileServer(DownloaderTest.SERVER_PORT);
    exec.submit(new Runnable() {
      @Override public void run()
      {
        try {
          s.call();
          latch.countDown();
        } catch (final Exception x) {
          x.printStackTrace();
        }
      }
    });
    latch.await();

    try {
      final File tmp = this.makeTempDir();
      final DownloaderConfigurationBuilderType cb =
        DownloaderConfiguration.newBuilder(tmp);
      final DownloaderConfiguration c = cb.build();
      final DownloaderType d = Downloader.newDownloader(exec, h, c);

      final CountDownLatch cancel_latch = new CountDownLatch(1);
      final DownloadListenerType listener = new LoggingListener() {
        @Override public void downloadCancelled(
          final DownloadSnapshot snap)
        {
          super.downloadCancelled(snap);
          cancel_latch.countDown();
        }
      };

      final OptionType<HTTPAuthType> none = Option.none();
      final long id0 =
        d
          .downloadEnqueue(
            none,
            this
              .serverAddress("/org/nypl/simplified/downloader/tests/hello.txt"),
            "Hello",
            listener);

      {
        final Some<DownloadSnapshot> some_snapshot =
          (Some<DownloadSnapshot>) d.downloadStatusSnapshot(id0);
        final DownloadSnapshot snapshot = some_snapshot.get();
        Assert.assertEquals(
          DownloadStatus.STATUS_IN_PROGRESS,
          snapshot.statusGet());
      }

      d.downloadCancel(id0);
      cancel_latch.await();

      {
        final Some<DownloadSnapshot> some_snapshot =
          (Some<DownloadSnapshot>) d.downloadStatusSnapshot(id0);
        final DownloadSnapshot snapshot = some_snapshot.get();
        Assert.assertEquals(
          DownloadStatus.STATUS_CANCELLED,
          snapshot.statusGet());
      }

      /**
       * Check that cancelling does not cause IDs to be re-used, and that the
       * previously cancelled task stays cancelled.
       */

      final long id1 =
        d
          .downloadEnqueue(
            none,
            this
              .serverAddress("/org/nypl/simplified/downloader/tests/hello.txt"),
            "Hello",
            listener);
      Assert.assertNotEquals(id0, id1);

      {
        final Some<DownloadSnapshot> some_snapshot =
          (Some<DownloadSnapshot>) d.downloadStatusSnapshot(id0);
        final DownloadSnapshot snapshot = some_snapshot.get();
        Assert.assertEquals(
          DownloadStatus.STATUS_CANCELLED,
          snapshot.statusGet());
      }

    } finally {
      exec.shutdown();
      s.stop();
    }
  }

  @Test public void testDownloadDestroyAll()
    throws Exception
  {
    final HTTPType h = HTTP.newHTTP();
    final File tmp = this.makeTempDir();
    final DownloaderConfigurationBuilderType cb =
      DownloaderConfiguration.newBuilder(tmp);
    final DownloaderConfiguration c = cb.build();

    final ExecutorService exec = Executors.newFixedThreadPool(8);
    final CountDownLatch latch = new CountDownLatch(1);
    final FileServer s = new FileServer(DownloaderTest.SERVER_PORT);
    exec.submit(new Runnable() {
      @Override public void run()
      {
        try {
          s.call();
          latch.countDown();
        } catch (final Exception x) {
          x.printStackTrace();
        }
      }
    });
    latch.await();

    final CountDownLatch complete_latch = new CountDownLatch(3);
    final CountDownLatch cleanup_latch = new CountDownLatch(3);
    final DownloadListenerType listener = new LoggingListener() {
      @Override public void downloadCleanedUp(
        final DownloadSnapshot snap)
      {
        super.downloadCleanedUp(snap);
        cleanup_latch.countDown();
      }

      @Override public void downloadCompleted(
        final DownloadSnapshot snap)
      {
        super.downloadCompleted(snap);
        complete_latch.countDown();
      }
    };

    try {
      final DownloaderType d = Downloader.newDownloader(exec, h, c);

      final OptionType<HTTPAuthType> none = Option.none();
      final long id_0 =
        d
          .downloadEnqueue(
            none,
            this
              .serverAddress("/org/nypl/simplified/downloader/tests/hello-0.txt"),
            "Hello",
            listener);
      final long id_1 =
        d
          .downloadEnqueue(
            none,
            this
              .serverAddress("/org/nypl/simplified/downloader/tests/hello-1.txt"),
            "Hello",
            listener);
      final long id_2 =
        d
          .downloadEnqueue(
            none,
            this
              .serverAddress("/org/nypl/simplified/downloader/tests/hello-2.txt"),
            "Hello",
            listener);

      complete_latch.await();

      final Some<DownloadSnapshot> some_0 =
        (Some<DownloadSnapshot>) d.downloadStatusSnapshot(id_0);
      final Some<DownloadSnapshot> some_1 =
        (Some<DownloadSnapshot>) d.downloadStatusSnapshot(id_1);
      final Some<DownloadSnapshot> some_2 =
        (Some<DownloadSnapshot>) d.downloadStatusSnapshot(id_2);

      Assert.assertEquals(
        some_0.get().statusGet(),
        DownloadStatus.STATUS_COMPLETED_NOT_TAKEN);
      Assert.assertEquals(
        some_1.get().statusGet(),
        DownloadStatus.STATUS_COMPLETED_NOT_TAKEN);
      Assert.assertEquals(
        some_2.get().statusGet(),
        DownloadStatus.STATUS_COMPLETED_NOT_TAKEN);

      Assert.assertTrue(new File(tmp, "1.data").exists());
      Assert.assertTrue(new File(tmp, "2.data").exists());
      Assert.assertTrue(new File(tmp, "3.data").exists());

      d.downloadDestroyAll();

      cleanup_latch.await();

      Assert.assertFalse(new File(tmp, "1.data").exists());
      Assert.assertFalse(new File(tmp, "2.data").exists());
      Assert.assertFalse(new File(tmp, "3.data").exists());
      Assert.assertTrue(tmp.list().length == 0);

      d.downloadAcknowledge(id_0);
      d.downloadAcknowledge(id_1);
      d.downloadAcknowledge(id_2);

      final Map<Long, DownloadSnapshot> all = d.downloadStatusSnapshotAll();
      Assert.assertTrue(all.isEmpty());

    } finally {
      exec.shutdown();
      s.stop();
    }

    try {
      final DownloaderType d = Downloader.newDownloader(exec, h, c);
      final Map<Long, DownloadSnapshot> all = d.downloadStatusSnapshotAll();
      Assert.assertTrue(all.isEmpty());

    } finally {
      exec.shutdown();
      s.stop();
    }
  }

  @Test public void testDownloadNonexistent()
    throws Exception
  {
    final HTTPType h = HTTP.newHTTP();
    final ExecutorService e = Executors.newFixedThreadPool(8);
    final CountDownLatch latch = new CountDownLatch(1);
    final FileServer s = new FileServer(DownloaderTest.SERVER_PORT);
    e.submit(new Runnable() {
      @Override public void run()
      {
        try {
          s.call();
          latch.countDown();
        } catch (final Exception x) {
          x.printStackTrace();
        }
      }
    });
    latch.await();

    final File tmp = this.makeTempDir();
    final DownloaderConfigurationBuilderType cb =
      DownloaderConfiguration.newBuilder(tmp);
    final DownloaderConfiguration c = cb.build();
    final DownloaderType d = Downloader.newDownloader(e, h, c);

    final CountDownLatch fail_latch = new CountDownLatch(1);
    final DownloadListenerType listener = new LoggingListener() {
      @Override public void downloadFailed(
        final DownloadSnapshot snap,
        final Throwable x)
      {
        super.downloadFailed(snap, x);
        fail_latch.countDown();
      }
    };

    final OptionType<HTTPAuthType> none = Option.none();
    final long id =
      d.downloadEnqueue(
        none,
        this.serverAddress("/nonexistent"),
        "Nonexistent",
        listener);

    fail_latch.await();

    final Some<DownloadSnapshot> some_snap =
      (Some<DownloadSnapshot>) d.downloadStatusSnapshot(id);
    Assert.assertEquals(DownloadStatus.STATUS_FAILED, some_snap
      .get()
      .statusGet());

    e.shutdown();
    s.stop();
  }

  @Test public void testDownloadNotHTTP()
    throws Exception
  {
    final HTTPType h = HTTP.newHTTP();
    final ExecutorService e = Executors.newFixedThreadPool(8);
    final CountDownLatch latch = new CountDownLatch(1);
    final FileServer s = new FileServer(DownloaderTest.SERVER_PORT);
    e.submit(new Runnable() {
      @Override public void run()
      {
        try {
          s.call();
          latch.countDown();
        } catch (final Exception x) {
          x.printStackTrace();
        }
      }
    });
    latch.await();

    try {
      final File tmp = this.makeTempDir();
      final DownloaderConfigurationBuilderType cb =
        DownloaderConfiguration.newBuilder(tmp);
      final DownloaderConfiguration c = cb.build();
      final DownloaderType d = Downloader.newDownloader(e, h, c);

      final CountDownLatch error_latch = new CountDownLatch(1);
      final AtomicReference<Throwable> e_ref =
        new AtomicReference<Throwable>();
      final DownloadAbstractListener listener =
        new DownloadAbstractListener() {
          @Override public void downloadFailed(
            final DownloadSnapshot snap,
            final Throwable ex)
          {
            e_ref.set(ex);
            error_latch.countDown();
          }
        };

      final OptionType<HTTPAuthType> none = Option.none();
      d.downloadEnqueue(
        none,
        URI.create("nothttp://example.com/nonexistent"),
        "Something",
        listener);

      error_latch.await();

      final Throwable ex = e_ref.get();
      Assert.assertTrue(ex instanceof IllegalArgumentException);

    } finally {
      e.shutdown();
      s.stop();
    }
  }

  @Test public void testDownloadOK()
    throws Exception
  {
    final HTTPType h = HTTP.newHTTP();
    final ExecutorService e = Executors.newFixedThreadPool(8);
    final CountDownLatch latch = new CountDownLatch(1);
    final FileServer s = new FileServer(DownloaderTest.SERVER_PORT);
    e.submit(new Runnable() {
      @Override public void run()
      {
        try {
          s.call();
          latch.countDown();
        } catch (final Exception x) {
          x.printStackTrace();
        }
      }
    });
    latch.await();

    final File tmp = this.makeTempDir();
    final DownloaderConfigurationBuilderType cb =
      DownloaderConfiguration.newBuilder(tmp);
    final DownloaderConfiguration c = cb.build();
    final DownloaderType d = Downloader.newDownloader(e, h, c);

    final CountDownLatch success_latch = new CountDownLatch(1);
    final DownloadListenerType listener = new LoggingListener() {
      @Override public void downloadCompleted(
        final DownloadSnapshot snap)
      {
        super.downloadCompleted(snap);
        success_latch.countDown();
      }
    };

    final OptionType<HTTPAuthType> none = Option.none();
    final URI uri =
      this.serverAddress("/org/nypl/simplified/downloader/tests/hello.txt");
    final long id = d.downloadEnqueue(none, uri, "Hello", listener);
    final long id2 = d.downloadEnqueue(none, uri, "Hello", listener);
    Assert.assertEquals(id, id2);

    success_latch.await();

    final Some<DownloadSnapshot> some_snap =
      (Some<DownloadSnapshot>) d.downloadStatusSnapshot(id);
    Assert.assertEquals(DownloadStatus.STATUS_COMPLETED_NOT_TAKEN, some_snap
      .get()
      .statusGet());

    final File file = new File(tmp, "1.data");
    final String text = Files.toString(file, Charset.forName("UTF-8"));
    Assert.assertEquals("Hello.", text);

    e.shutdown();
    s.stop();
  }

  @Test public void testDownloadPauseOK()
    throws Exception
  {
    final HTTPType h = HTTP.newHTTP();
    final ExecutorService e = Executors.newFixedThreadPool(8);
    final CountDownLatch latch = new CountDownLatch(1);
    final FileServer s = new FileServer(DownloaderTest.SERVER_PORT);
    e.submit(new Runnable() {
      @Override public void run()
      {
        try {
          s.call();
          latch.countDown();
        } catch (final Exception x) {
          x.printStackTrace();
        }
      }
    });
    latch.await();

    final File tmp = this.makeTempDir();
    final DownloaderConfigurationBuilderType cb =
      DownloaderConfiguration.newBuilder(tmp);
    final DownloaderConfiguration c = cb.build();
    final DownloaderType d = Downloader.newDownloader(e, h, c);

    final CountDownLatch pause_latch = new CountDownLatch(1);
    final CountDownLatch resume_latch = new CountDownLatch(1);
    final CountDownLatch complete_latch = new CountDownLatch(1);

    final DownloadListenerType listener = new LoggingListener() {
      @Override public void downloadCompleted(
        final DownloadSnapshot snap)
      {
        super.downloadCompleted(snap);
        complete_latch.countDown();
      }

      @Override public void downloadPaused(
        final DownloadSnapshot snap)
      {
        super.downloadPaused(snap);
        pause_latch.countDown();
      }

      @Override public void downloadResumed(
        final DownloadSnapshot snap)
      {
        super.downloadResumed(snap);
        resume_latch.countDown();
      }
    };

    final OptionType<HTTPAuthType> none = Option.none();
    final long id =
      d
        .downloadEnqueue(
          none,
          this
            .serverAddress("/org/nypl/simplified/downloader/tests/hello.txt"),
          "Hello",
          listener);

    d.downloadPause(id);
    pause_latch.await();

    {
      /**
       * Check that pausing reuses IDs instead of creating new downloads.
       */

      final long id2 =
        d
          .downloadEnqueue(
            none,
            this
              .serverAddress("/org/nypl/simplified/downloader/tests/hello.txt"),
            "Hello",
            listener);
      Assert.assertEquals(id, id2);
    }

    {
      final Some<DownloadSnapshot> some_snapshot =
        (Some<DownloadSnapshot>) d.downloadStatusSnapshot(id);
      final DownloadSnapshot snapshot = some_snapshot.get();
      Assert.assertEquals(DownloadStatus.STATUS_PAUSED, snapshot.statusGet());
    }

    d.downloadResume(id);
    resume_latch.await();

    {
      /**
       * Check that resuming reuses IDs instead of creating new downloads.
       */

      final long id2 =
        d
          .downloadEnqueue(
            none,
            this
              .serverAddress("/org/nypl/simplified/downloader/tests/hello.txt"),
            "Hello",
            listener);
      Assert.assertEquals(id, id2);
    }

    {
      final Some<DownloadSnapshot> some_snapshot =
        (Some<DownloadSnapshot>) d.downloadStatusSnapshot(id);
      final DownloadSnapshot snapshot = some_snapshot.get();
      Assert.assertEquals(
        DownloadStatus.STATUS_IN_PROGRESS_RESUMED,
        snapshot.statusGet());
    }

    complete_latch.await();

    {
      final Some<DownloadSnapshot> some_snapshot =
        (Some<DownloadSnapshot>) d.downloadStatusSnapshot(id);
      final DownloadSnapshot snapshot = some_snapshot.get();
      Assert.assertEquals(
        DownloadStatus.STATUS_COMPLETED_NOT_TAKEN,
        snapshot.statusGet());
    }

    final File file = new File(tmp, "1.data");
    final String text = Files.toString(file, Charset.forName("UTF-8"));
    Assert.assertEquals("Hello.", text);

    {
      /**
       * Check that completed downloads reuse IDs instead of creating new
       * downloads.
       */

      final long id2 =
        d
          .downloadEnqueue(
            none,
            this
              .serverAddress("/org/nypl/simplified/downloader/tests/hello.txt"),
            "Hello",
            listener);
      Assert.assertEquals(id, id2);
    }

    e.shutdown();
    s.stop();
  }

  @Test public void testDownloadSerialized()
    throws Exception
  {
    final HTTPType h = HTTP.newHTTP();
    final File tmp = this.makeTempDir();
    final DownloaderConfigurationBuilderType cb =
      DownloaderConfiguration.newBuilder(tmp);
    final DownloaderConfiguration c = cb.build();

    long id;

    final CountDownLatch start_latch = new CountDownLatch(1);
    final CountDownLatch pause_latch = new CountDownLatch(1);
    final CountDownLatch resume_latch = new CountDownLatch(1);
    final DownloadListenerType listener = new LoggingListener() {
      @Override public void downloadPaused(
        final DownloadSnapshot snap)
      {
        super.downloadPaused(snap);
        pause_latch.countDown();
      }

      @Override public void downloadResumed(
        final DownloadSnapshot snap)
      {
        super.downloadResumed(snap);
        resume_latch.countDown();
      }
    };

    {
      final ExecutorService e = Executors.newFixedThreadPool(8);
      final CountDownLatch latch = new CountDownLatch(1);
      final FileServer s = new FileServer(DownloaderTest.SERVER_PORT);
      e.submit(new Runnable() {
        @Override public void run()
        {
          try {
            s.call();
            latch.countDown();
          } catch (final Exception x) {
            x.printStackTrace();
          }
        }
      });
      latch.await();

      try {
        final OptionType<HTTPAuthType> none = Option.none();
        final DownloaderType d = Downloader.newDownloader(e, h, c);
        id =
          d
            .downloadEnqueue(
              none,
              this
                .serverAddress("/org/nypl/simplified/downloader/tests/hello.txt"),
              "Hello",
              listener);
        d.downloadPause(id);
        pause_latch.await();

      } finally {
        e.shutdown();
        s.stop();
      }
    }

    {
      final ExecutorService e = Executors.newFixedThreadPool(8);
      final CountDownLatch latch = new CountDownLatch(1);
      final FileServer s = new FileServer(DownloaderTest.SERVER_PORT);
      e.submit(new Runnable() {
        @Override public void run()
        {
          try {
            s.call();
            latch.countDown();
          } catch (final Exception x) {
            x.printStackTrace();
          }
        }
      });
      latch.await();

      try {
        final DownloaderType d =
          Downloader.newDownloaderWithListener(e, h, c, listener);
        d.downloadResume(id);
        resume_latch.await();

        final Some<DownloadSnapshot> some_snap =
          (Some<DownloadSnapshot>) d.downloadStatusSnapshot(id);
        final DownloadSnapshot snap = some_snap.get();

        Assert.assertEquals(snap.statusGetID(), id);

      } finally {
        e.shutdown();
        s.stop();
      }
    }
  }

  @Test public void testDownloadStatusNonexistent()
    throws Exception
  {
    final HTTPType h = HTTP.newHTTP();
    final ExecutorService e = Executors.newFixedThreadPool(8);
    final CountDownLatch latch = new CountDownLatch(1);
    final FileServer s = new FileServer(DownloaderTest.SERVER_PORT);
    e.submit(new Runnable() {
      @Override public void run()
      {
        try {
          s.call();
          latch.countDown();
        } catch (final Exception x) {
          x.printStackTrace();
        }
      }
    });
    latch.await();

    final File tmp = this.makeTempDir();
    final DownloaderConfigurationBuilderType cb =
      DownloaderConfiguration.newBuilder(tmp);
    final DownloaderConfiguration c = cb.build();
    final DownloaderType d = Downloader.newDownloader(e, h, c);

    Assert.assertTrue(d.downloadStatusSnapshot(0).isNone());

    e.shutdown();
    s.stop();
  }

  @Test public void testDownloadTaken()
    throws Exception
  {
    final HTTPType h = HTTP.newHTTP();
    final ExecutorService e = Executors.newFixedThreadPool(8);
    final CountDownLatch latch = new CountDownLatch(1);
    final FileServer s = new FileServer(DownloaderTest.SERVER_PORT);
    e.submit(new Runnable() {
      @Override public void run()
      {
        try {
          s.call();
          latch.countDown();
        } catch (final Exception x) {
          x.printStackTrace();
        }
      }
    });
    latch.await();

    final File tmp = this.makeTempDir();
    final DownloaderConfigurationBuilderType cb =
      DownloaderConfiguration.newBuilder(tmp);
    final DownloaderConfiguration c = cb.build();
    final DownloaderType d = Downloader.newDownloader(e, h, c);

    final OptionType<HTTPAuthType> none = Option.none();
    final URI uri =
      this.serverAddress("/org/nypl/simplified/downloader/tests/hello.txt");

    final long id0;
    final long id1;
    final long id2;

    /**
     * Schedule a download, and then don't take the file when given the
     * opportunity.
     */

    {
      final CountDownLatch success_latch = new CountDownLatch(1);
      final DownloadListenerType listener = new LoggingListener() {
        @Override public void downloadCompleted(
          final DownloadSnapshot snap)
        {
          super.downloadCompleted(snap);
          success_latch.countDown();
        }
      };

      id0 = d.downloadEnqueue(none, uri, "Hello", listener);
      success_latch.await();

      final Some<DownloadSnapshot> some_snap =
        (Some<DownloadSnapshot>) d.downloadStatusSnapshot(id0);
      Assert.assertEquals(
        DownloadStatus.STATUS_COMPLETED_NOT_TAKEN,
        some_snap.get().statusGet());
    }

    /**
     * Try to download the same URI again: The task returned will be the
     * previously completed (but not taken) task.
     */

    {
      final CountDownLatch success_latch = new CountDownLatch(2);
      final DownloadListenerType listener = new LoggingListener() {
        @Override public void downloadCompletedTake(
          final DownloadSnapshot snap,
          final File file_data)
        {
          super.downloadCompletedTake(snap, file_data);
          file_data.delete();
          success_latch.countDown();
        }

        @Override public void downloadCompletedTaken(
          final DownloadSnapshot snap)
        {
          super.downloadCompletedTaken(snap);
          success_latch.countDown();
        }
      };

      id1 = d.downloadEnqueue(none, uri, "Hello", listener);
      success_latch.await();
    }

    Assert.assertEquals(id0, id1);

    /**
     * Schedule another download of the same URI. Because the data was taken
     * last time, an entirely new task will be created.
     */

    {
      final CountDownLatch success_latch = new CountDownLatch(1);
      final DownloadListenerType listener = new LoggingListener() {
        @Override public void downloadCompleted(
          final DownloadSnapshot snap)
        {
          super.downloadCompleted(snap);
          success_latch.countDown();
        }
      };

      id2 = d.downloadEnqueue(none, uri, "Hello", listener);
      success_latch.await();

      final Some<DownloadSnapshot> some_snap =
        (Some<DownloadSnapshot>) d.downloadStatusSnapshot(id2);
      Assert.assertEquals(
        DownloadStatus.STATUS_COMPLETED_NOT_TAKEN,
        some_snap.get().statusGet());

      Assert.assertNotEquals(id0, id2);
      Assert.assertNotEquals(id1, id2);
    }

    e.shutdown();
    s.stop();
  }

}
