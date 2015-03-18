package org.nypl.simplified.downloader.tests;

import java.io.File;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.nypl.simplified.downloader.core.DownloadSnapshot;
import org.nypl.simplified.downloader.core.DownloadStatus;
import org.nypl.simplified.downloader.core.Downloader;
import org.nypl.simplified.downloader.core.DownloaderConfiguration;
import org.nypl.simplified.downloader.core.DownloaderConfigurationBuilderType;
import org.nypl.simplified.downloader.core.DownloaderType;

import com.google.common.io.Files;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NonNull;
import com.io7m.jnull.NullCheck;

/**
 * Note: These tests are unfortunately time-dependent. The right way to fix
 * this is to hide all HTTP communication behind an interface so that tests
 * can hijack communication.
 */

@SuppressWarnings("static-method") public final class DownloaderTest
{
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
      final DownloaderType d = Downloader.newDownloader(e, c);

      final long id =
        d
          .downloadEnqueue(
            this
              .serverAddress("/org/nypl/simplified/downloader/tests/hello.txt"),
            "Hello");

      {
        final Some<DownloadSnapshot> some_snapshot =
          (Some<DownloadSnapshot>) d.downloadStatusSnapshot(id);
        final DownloadSnapshot snapshot = some_snapshot.get();
        Assert.assertEquals(
          DownloadStatus.STATUS_IN_PROGRESS,
          snapshot.statusGet());
      }

      d.downloadCancel(id);

      Thread.sleep(1000);

      {
        final Some<DownloadSnapshot> some_snapshot =
          (Some<DownloadSnapshot>) d.downloadStatusSnapshot(id);
        final DownloadSnapshot snapshot = some_snapshot.get();
        Assert.assertEquals(
          DownloadStatus.STATUS_CANCELLED,
          snapshot.statusGet());
      }

    } finally {
      e.shutdown();
      s.stop();
    }
  }

  @Test public void testDownloadNonexistent()
    throws Exception
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

    final File tmp = this.makeTempDir();
    final DownloaderConfigurationBuilderType cb =
      DownloaderConfiguration.newBuilder(tmp);
    final DownloaderConfiguration c = cb.build();
    final DownloaderType d = Downloader.newDownloader(e, c);

    final long id =
      d.downloadEnqueue(this.serverAddress("/nonexistent"), "Nonexistent");

    final Long lid = Long.valueOf(id);
    DownloadStatus status;

    for (;;) {
      final Map<Long, DownloadSnapshot> stats = d.downloadStatusSnapshotAll();
      Assert.assertTrue(stats.containsKey(lid));
      final DownloadSnapshot snap = stats.get(lid);
      status = snap.statusGet();
      System.out.println(snap);

      if (status != DownloadStatus.STATUS_IN_PROGRESS) {
        break;
      }
      Thread.sleep(100);
    }

    Assert.assertEquals(DownloadStatus.STATUS_FAILED, status);

    e.shutdown();
    s.stop();
  }

  @Test(expected = IllegalArgumentException.class) public
    void
    testDownloadNotHTTP()
      throws Exception
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

      final File tmp = this.makeTempDir();
      final DownloaderConfigurationBuilderType cb =
        DownloaderConfiguration.newBuilder(tmp);
      final DownloaderConfiguration c = cb.build();
      final DownloaderType d = Downloader.newDownloader(e, c);

      d.downloadEnqueue(
        URI.create("nothttp://example.com/nonexistent"),
        "Something");
    } finally {
      e.shutdown();
      s.stop();
    }
  }

  @Test public void testDownloadOK()
    throws Exception
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

    final File tmp = this.makeTempDir();
    final DownloaderConfigurationBuilderType cb =
      DownloaderConfiguration.newBuilder(tmp);
    final DownloaderConfiguration c = cb.build();
    final DownloaderType d = Downloader.newDownloader(e, c);

    final long id =
      d
        .downloadEnqueue(
          this
            .serverAddress("/org/nypl/simplified/downloader/tests/hello.txt"),
          "Hello");

    Assert.assertEquals(1L, id);

    final Long lid = Long.valueOf(id);
    DownloadStatus status;

    for (;;) {
      final Map<Long, DownloadSnapshot> stats = d.downloadStatusSnapshotAll();
      Assert.assertTrue(stats.containsKey(lid));
      final DownloadSnapshot snap = stats.get(lid);
      status = snap.statusGet();
      System.out.println(snap);

      if (status != DownloadStatus.STATUS_IN_PROGRESS) {
        break;
      }
      Thread.sleep(100);
    }

    Assert.assertEquals(DownloadStatus.STATUS_COMPLETED, status);

    final File file = new File(tmp, "1.data");
    final String text = Files.toString(file, Charset.forName("UTF-8"));
    Assert.assertEquals("Hello.", text);

    e.shutdown();
    s.stop();
  }

  @Test public void testDownloadPauseOK()
    throws Exception
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

    final File tmp = this.makeTempDir();
    final DownloaderConfigurationBuilderType cb =
      DownloaderConfiguration.newBuilder(tmp);
    final DownloaderConfiguration c = cb.build();
    final DownloaderType d = Downloader.newDownloader(e, c);

    final long id =
      d
        .downloadEnqueue(
          this
            .serverAddress("/org/nypl/simplified/downloader/tests/hello.txt"),
          "Hello");
    d.downloadPause(id);

    Assert.assertEquals(1L, id);

    Thread.sleep(1000);

    {
      final Some<DownloadSnapshot> some_snapshot =
        (Some<DownloadSnapshot>) d.downloadStatusSnapshot(id);
      final DownloadSnapshot snapshot = some_snapshot.get();
      Assert.assertEquals(DownloadStatus.STATUS_PAUSED, snapshot.statusGet());
    }

    d.downloadResume(id);

    {
      final Some<DownloadSnapshot> some_snapshot =
        (Some<DownloadSnapshot>) d.downloadStatusSnapshot(id);
      final DownloadSnapshot snapshot = some_snapshot.get();
      Assert.assertEquals(
        DownloadStatus.STATUS_IN_PROGRESS,
        snapshot.statusGet());
    }

    Thread.sleep(1000);

    {
      final Some<DownloadSnapshot> some_snapshot =
        (Some<DownloadSnapshot>) d.downloadStatusSnapshot(id);
      final DownloadSnapshot snapshot = some_snapshot.get();
      Assert.assertEquals(
        DownloadStatus.STATUS_COMPLETED,
        snapshot.statusGet());
    }

    final File file = new File(tmp, "1.data");
    final String text = Files.toString(file, Charset.forName("UTF-8"));
    Assert.assertEquals("Hello.", text);

    e.shutdown();
    s.stop();
  }

  @Test public void testDownloadSerialized()
    throws Exception
  {
    final File tmp = this.makeTempDir();
    final DownloaderConfigurationBuilderType cb =
      DownloaderConfiguration.newBuilder(tmp);
    final DownloaderConfiguration c = cb.build();

    long id;

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
        final DownloaderType d = Downloader.newDownloader(e, c);
        id =
          d
            .downloadEnqueue(
              this
                .serverAddress("/org/nypl/simplified/downloader/tests/hello.txt"),
              "Hello");

        Thread.sleep(1000);

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
        final DownloaderType d = Downloader.newDownloader(e, c);

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
    final DownloaderType d = Downloader.newDownloader(e, c);

    Assert.assertTrue(d.downloadStatusSnapshot(0).isNone());

    e.shutdown();
    s.stop();
  }

  @Test public void testDownloadDestroyAll()
    throws Exception
  {
    final File tmp = this.makeTempDir();
    final DownloaderConfigurationBuilderType cb =
      DownloaderConfiguration.newBuilder(tmp);
    final DownloaderConfiguration c = cb.build();

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
      final DownloaderType d = Downloader.newDownloader(e, c);
      final long id_0 =
        d
          .downloadEnqueue(
            this
              .serverAddress("/org/nypl/simplified/downloader/tests/hello.txt"),
            "Hello");
      final long id_1 =
        d
          .downloadEnqueue(
            this
              .serverAddress("/org/nypl/simplified/downloader/tests/hello.txt"),
            "Hello");
      final long id_2 =
        d
          .downloadEnqueue(
            this
              .serverAddress("/org/nypl/simplified/downloader/tests/hello.txt"),
            "Hello");

      Thread.sleep(1000);

      final Some<DownloadSnapshot> some_0 =
        (Some<DownloadSnapshot>) d.downloadStatusSnapshot(id_0);
      final Some<DownloadSnapshot> some_1 =
        (Some<DownloadSnapshot>) d.downloadStatusSnapshot(id_1);
      final Some<DownloadSnapshot> some_2 =
        (Some<DownloadSnapshot>) d.downloadStatusSnapshot(id_2);

      Assert.assertEquals(
        some_0.get().statusGet(),
        DownloadStatus.STATUS_COMPLETED);
      Assert.assertEquals(
        some_1.get().statusGet(),
        DownloadStatus.STATUS_COMPLETED);
      Assert.assertEquals(
        some_2.get().statusGet(),
        DownloadStatus.STATUS_COMPLETED);

      Assert.assertTrue(new File(tmp, "1.data").exists());
      Assert.assertTrue(new File(tmp, "2.data").exists());
      Assert.assertTrue(new File(tmp, "3.data").exists());

      d.downloadDestroyAll();

      Thread.sleep(1000);

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
      e.shutdown();
      s.stop();
    }

    try {
      final DownloaderType d = Downloader.newDownloader(e, c);
      final Map<Long, DownloadSnapshot> all = d.downloadStatusSnapshotAll();
      Assert.assertTrue(all.isEmpty());

    } finally {
      e.shutdown();
      s.stop();
    }
  }
}
