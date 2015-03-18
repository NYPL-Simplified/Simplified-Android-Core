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

import org.nypl.simplified.downloader.core.DownloadSnapshot;
import org.nypl.simplified.downloader.core.Downloader;
import org.nypl.simplified.downloader.core.DownloaderConfiguration;
import org.nypl.simplified.downloader.core.DownloaderConfigurationBuilderType;
import org.nypl.simplified.downloader.core.DownloaderType;

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

    final DownloaderConfigurationBuilderType cb =
      DownloaderConfiguration.newBuilder(dir);
    cb.setBufferSize(2 << 4);

    final DownloaderConfiguration c = cb.build();
    final DownloaderType d = Downloader.newDownloader(exec, c);

    final BufferedReader reader =
      new BufferedReader(new InputStreamReader(System.in));

    final Thread t = new Thread(new Runnable() {
      @Override public void run()
      {
        for (;;) {
          final Map<Long, DownloadSnapshot> stats =
            d.downloadStatusSnapshotAll();
          for (final Long id : stats.keySet()) {
            final DownloadSnapshot status = stats.get(id);
            System.out.printf(
              "[%d] (%d/%d) %s: %s\n",
              id,
              status.statusGetCurrentBytes(),
              status.statusGetMaximumBytes(),
              status.statusGet(),
              status.statusGetURI());

            if (status.statusGet().isComplete()) {
              d.downloadAcknowledge(id);
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
          final long id = d.downloadEnqueue(uri, "Title");
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

      } catch (final URISyntaxException e) {
        System.err.println("error: invalid uri: " + e.getMessage());
      }
    }
  }
}
