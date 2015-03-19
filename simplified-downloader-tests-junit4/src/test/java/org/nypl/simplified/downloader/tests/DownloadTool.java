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

    final DownloaderConfiguration c = cb.build();
    final DownloaderType d = Downloader.newDownloader(exec, h, c);

    final BufferedReader reader =
      new BufferedReader(new InputStreamReader(System.in));

    final DownloadListenerType listener = new DownloadListenerType() {
      @Override public void downloadStarted(
        final DownloadSnapshot snap)
      {
        System.out.println(snap);
      }

      @Override public void downloadResumed(
        final DownloadSnapshot snap)
      {
        System.out.println(snap);
      }

      @Override public void downloadPaused(
        final DownloadSnapshot snap)
      {
        System.out.println(snap);
      }

      @Override public void downloadFailed(
        final DownloadSnapshot snap,
        final Throwable e)
      {
        System.out.println(snap);
      }

      @Override public void downloadCompleted(
        final DownloadSnapshot snap)
      {
        System.out.println(snap);
      }

      @Override public void downloadCancelled(
        final DownloadSnapshot snap)
      {
        System.out.println(snap);
      }

      @Override public void downloadCleanedUp(
        final DownloadSnapshot snap)
      {

      }

      @Override public void downloadStartedReceivingData(
        final DownloadSnapshot snap)
      {

      }
    };

    final Thread t = new Thread(new Runnable() {
      @Override public void run()
      {
        for (;;) {
          final Map<Long, DownloadSnapshot> s = d.downloadStatusSnapshotAll();
          for (final Long lid : s.keySet()) {
            final DownloadSnapshot snap = s.get(lid);
            switch (snap.statusGet()) {
              case STATUS_CANCELLED:
              case STATUS_COMPLETED:
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

        if ("d".equals(command)) {
          if (segments.length < 2) {
            System.err.println("error: usage: d uri");
            continue;
          }
          final URI uri = new URI(segments[1]);
          final OptionType<HTTPAuthType> none = Option.none();
          final long id = d.downloadEnqueue(none, uri, "Title", listener);
          System.out.println("info: download queued " + id);
        }

        if ("c".equals(command)) {
          if (segments.length < 2) {
            System.err.println("error: usage: c id");
            continue;
          }
          final long id = Long.valueOf(segments[1]);
          d.downloadCancel(id);
          System.out.println("info: download cancelled " + id);
        }

        if ("p".equals(command)) {
          if (segments.length < 2) {
            System.err.println("error: usage: p id");
            continue;
          }
          final long id = Long.valueOf(segments[1]);
          d.downloadPause(id);
          System.out.println("info: download paused " + id);
        }

        if ("r".equals(command)) {
          if (segments.length < 2) {
            System.err.println("error: usage: r id");
            continue;
          }
          final long id = Long.valueOf(segments[1]);
          d.downloadResume(id);
          System.out.println("info: download resumed " + id);
        }

        if ("s".equals(command)) {
          DownloadTool.showStatus(d);
        }

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
