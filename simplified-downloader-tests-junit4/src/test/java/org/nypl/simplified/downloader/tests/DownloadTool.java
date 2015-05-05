package org.nypl.simplified.downloader.tests;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.nypl.simplified.downloader.core.DownloadListenerType;
import org.nypl.simplified.downloader.core.DownloadSnapshot;
import org.nypl.simplified.downloader.core.Downloader;
import org.nypl.simplified.downloader.core.DownloaderConfiguration;
import org.nypl.simplified.downloader.core.DownloaderConfigurationBuilderType;
import org.nypl.simplified.downloader.core.DownloaderType;
import org.nypl.simplified.http.core.HTTP;
import org.nypl.simplified.http.core.HTTPAuthBasic;
import org.nypl.simplified.http.core.HTTPAuthType;
import org.nypl.simplified.http.core.HTTPType;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;

public final class DownloadTool
{
  public static void main(
    final String[] args)
    throws IOException
  {
    if (args.length < 1) {
      System.err.println("usage: download-dir");
      System.exit(1);
    }

    final File dir = new File(args[0]);
    final ExecutorService exec = Executors.newFixedThreadPool(4);
    final HTTPType h = HTTP.newHTTP();

    final DownloaderConfigurationBuilderType cb =
      DownloaderConfiguration.newBuilder(dir);
    cb.setBufferSize(2 << 4);

    final DownloadListenerType listener = new DownloadListenerType() {
      @Override public void downloadCancelled(
        final DownloadSnapshot snap)
      {
        System.out.println(snap);
      }

      @Override public void downloadCleanedUp(
        final DownloadSnapshot snap)
      {
        System.out.println(snap);
      }

      @Override public void downloadCompleted(
        final DownloadSnapshot snap)
      {
        System.out.println(snap);
      }

      @Override public void downloadCompletedTake(
        final DownloadSnapshot snap,
        final File file_data)
      {
        System.out.println("info: "
          + snap.statusGetID()
          + " ready for taking: "
          + file_data);

        final File completed = new File("/tmp/completed");
        completed.mkdirs();
        if (completed.isDirectory() == false) {
          throw new IllegalStateException(completed + ": Not a directory");
        }

        final File target = new File(completed, file_data.getName());
        final boolean ok = file_data.renameTo(target);
        if (ok == false) {
          throw new IllegalStateException(target + ": Could not rename file");
        }
      }

      @Override public void downloadCompletedTakeFailed(
        final DownloadSnapshot snap,
        final Throwable x)
      {
        x.printStackTrace();
      }

      @Override public void downloadFailed(
        final DownloadSnapshot snap,
        final Throwable e)
      {
        System.out.println(snap);
        e.printStackTrace();
      }

      @Override public void downloadPaused(
        final DownloadSnapshot snap)
      {
        System.out.println(snap);
      }

      @Override public void downloadReceivedData(
        final DownloadSnapshot snap)
      {

      }

      @Override public void downloadResumed(
        final DownloadSnapshot snap)
      {
        System.out.println(snap);
      }

      @Override public void downloadStarted(
        final DownloadSnapshot snap)
      {
        System.out.println(snap);
      }

      @Override public void downloadStartedReceivingData(
        final DownloadSnapshot snap)
      {

      }

      @Override public void downloadCompletedTaken(
        final DownloadSnapshot snap)
      {

      }
    };

    final DownloaderConfiguration c = cb.build();
    final DownloaderType d =
      Downloader.newDownloaderWithListener(exec, h, c, listener);

    final BufferedReader reader =
      new BufferedReader(new InputStreamReader(System.in));

    final Thread t = new Thread(new Runnable() {
      @Override public void run()
      {
        for (;;) {
          final Map<Long, DownloadSnapshot> s = d.downloadStatusSnapshotAll();
          for (final Long lid : s.keySet()) {
            final DownloadSnapshot snap = s.get(lid);
            switch (snap.statusGet()) {
              case STATUS_CANCELLED:
              case STATUS_COMPLETED_NOT_TAKEN:
              case STATUS_COMPLETED_TAKEN:
              case STATUS_FAILED:
              case STATUS_PAUSED:
              {
                break;
              }
              case STATUS_IN_PROGRESS:
              case STATUS_IN_PROGRESS_RESUMED:
              {
                System.out.println(snap);
                break;
              }
            }
          }

          try {
            Thread.sleep(1000);
          } catch (final InterruptedException e) {
            // Ignore
          }
        }
      }
    });

    t.start();

    OptionType<HTTPAuthType> auth = Option.none();

    for (;;) {
      final String line = reader.readLine();
      if (line == null) {
        break;
      }

      final String[] segments = line.split("\\s+");
      if (segments.length < 1) {
        continue;
      }

      try {
        final String command = segments[0];

        if ("download".equals(command)) {
          if (segments.length < 2) {
            System.err.println("error: usage: download uri");
            continue;
          }
          final URI uri = new URI(segments[1]);
          final long id = d.downloadEnqueue(auth, uri, "Title", listener);
          System.out.println("info: download queued " + id);
          continue;
        }

        if ("cancel".equals(command)) {
          if (segments.length < 2) {
            System.err.println("error: usage: cancel id");
            continue;
          }
          final long id = Long.valueOf(segments[1]);
          d.downloadCancel(id);
          System.out.println("info: download cancelled " + id);
          continue;
        }

        if ("pause".equals(command)) {
          if (segments.length < 2) {
            System.err.println("error: usage: pause id");
            continue;
          }
          final long id = Long.valueOf(segments[1]);
          d.downloadPause(id);
          System.out.println("info: download paused " + id);
          continue;
        }

        if ("resume".equals(command)) {
          if (segments.length < 2) {
            System.err.println("error: usage: resume id");
            continue;
          }
          final long id = Long.valueOf(segments[1]);
          d.downloadResume(id);
          System.out.println("info: download resumed " + id);
          continue;
        }

        if ("status".equals(command)) {
          DownloadTool.showStatus(d);
          continue;
        }

        if ("auth-basic".equals(command)) {
          if (segments.length < 3) {
            System.err.println("error: usage: auth-basic user pass");
            continue;
          }
          final String name = segments[1];
          final String pass = segments[2];
          auth = Option.some((HTTPAuthType) new HTTPAuthBasic(name, pass));
          continue;
        }

        if ("auth-none".equals(command)) {
          auth = Option.none();
          continue;
        }

        System.err.println("error: unknown command: " + command);

      } catch (final URISyntaxException e) {
        System.err.println("error: invalid uri: " + e.getMessage());
      }
    }
  }

  private static void showStatus(
    final DownloaderType d)
  {
    final Map<Long, DownloadSnapshot> s = d.downloadStatusSnapshotAll();
    for (final Long lid : s.keySet()) {
      final DownloadSnapshot snap = s.get(lid);
      System.out.println(snap);
    }
  }
}
