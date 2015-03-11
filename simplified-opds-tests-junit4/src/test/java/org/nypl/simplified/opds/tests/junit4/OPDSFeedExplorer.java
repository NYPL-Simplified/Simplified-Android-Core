package org.nypl.simplified.opds.tests.junit4;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.nypl.simplified.opds.core.OPDSAcquisitionFeed;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.nypl.simplified.opds.core.OPDSFeedLoadListenerType;
import org.nypl.simplified.opds.core.OPDSFeedLoader;
import org.nypl.simplified.opds.core.OPDSFeedLoaderType;
import org.nypl.simplified.opds.core.OPDSFeedMatcherType;
import org.nypl.simplified.opds.core.OPDSFeedParser;
import org.nypl.simplified.opds.core.OPDSFeedParserType;
import org.nypl.simplified.opds.core.OPDSFeedTransport;
import org.nypl.simplified.opds.core.OPDSFeedTransportType;
import org.nypl.simplified.opds.core.OPDSFeedType;
import org.nypl.simplified.opds.core.OPDSNavigationFeed;
import org.nypl.simplified.opds.core.OPDSNavigationFeedEntry;

import com.google.common.util.concurrent.ListenableFuture;
import com.io7m.jfunctional.Unit;
import com.io7m.junreachable.UnreachableCodeException;

public final class OPDSFeedExplorer
{
  public static void main(
    final String args[])
    throws IOException
  {
    final BufferedReader r =
      new BufferedReader(new InputStreamReader(System.in));

    final ExecutorService e = Executors.newFixedThreadPool(1);
    final OPDSFeedParserType p = OPDSFeedParser.newParser();
    final OPDSFeedTransportType t = OPDSFeedTransport.newTransport();
    final OPDSFeedLoaderType loader = OPDSFeedLoader.newLoader(e, p, t);

    for (;;) {
      System.out.print("explorer $ ");

      final String line = r.readLine();
      if (line == null) {
        break;
      }

      final ListenableFuture<OPDSFeedType> f =
        loader.fromURI(URI.create(line), new OPDSFeedLoadListenerType() {
          @Override public void onFeedLoadingSuccess(
            final OPDSFeedType ff)
          {

          }

          @Override public void onFeedLoadingFailure(
            final Throwable x)
          {

          }
        });

      try {
        final OPDSFeedType ff = f.get();
        System.out.println("info: download completed: " + ff.getFeedTitle());
        OPDSFeedExplorer.showFeed(ff);
      } catch (final Throwable x) {
        System.out.println("info: download failed: " + x.getMessage());
      }
    }
  }

  private static void showFeed(
    final OPDSFeedType ff)
  {
    ff
      .matchFeedType(new OPDSFeedMatcherType<Unit, UnreachableCodeException>() {
        @Override public Unit onAcquisitionFeed(
          final OPDSAcquisitionFeed f)
        {
          System.out.println("  Feed title: " + f.getFeedTitle());
          System.out.println("  Feed entries:");
          final List<OPDSAcquisitionFeedEntry> entries = f.getFeedEntries();
          final int size = entries.size();
          for (int index = 0; index < size; ++index) {
            final OPDSAcquisitionFeedEntry e = entries.get(index);
            System.out.printf("    [%d] %s\n", index, e.getTitle());
          }
          return Unit.unit();
        }

        @Override public Unit onNavigationFeed(
          final OPDSNavigationFeed f)
        {
          System.out.println("  Feed title: " + f.getFeedTitle());
          System.out.println("  Feed entries:");
          final List<OPDSNavigationFeedEntry> entries = f.getFeedEntries();
          final int size = entries.size();
          for (int index = 0; index < size; ++index) {
            final OPDSNavigationFeedEntry e = entries.get(index);
            System.out.printf("    [%d] %s\n", index, e.getTargetURI());
          }
          return Unit.unit();
        }
      });
  }
}
