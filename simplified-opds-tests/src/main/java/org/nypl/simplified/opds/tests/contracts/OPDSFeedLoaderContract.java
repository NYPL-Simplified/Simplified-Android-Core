package org.nypl.simplified.opds.tests.contracts;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.nypl.simplified.opds.core.OPDSAcquisitionFeed;
import org.nypl.simplified.opds.core.OPDSFeedLoadListenerType;
import org.nypl.simplified.opds.core.OPDSFeedLoader;
import org.nypl.simplified.opds.core.OPDSFeedLoaderType;
import org.nypl.simplified.opds.core.OPDSFeedParseException;
import org.nypl.simplified.opds.core.OPDSFeedParserType;
import org.nypl.simplified.opds.core.OPDSFeedTransportType;
import org.nypl.simplified.opds.core.OPDSFeedType;
import org.nypl.simplified.opds.tests.utilities.TestUtilities;

import com.io7m.junreachable.UnreachableCodeException;

@SuppressWarnings({ "null" }) public final class OPDSFeedLoaderContract implements
  OPDSFeedLoaderContractType
{
  @Override public void testLoaderErrorCorrect()
    throws Exception
  {
    final AtomicBoolean caught = new AtomicBoolean(false);

    final ExecutorService e = Executors.newCachedThreadPool();
    try {
      final OPDSFeedParseException ex = new OPDSFeedParseException("Error!");

      final OPDSFeedParserType p = new OPDSFeedParserType() {
        @Override public OPDSFeedType parse(
          final URI uri,
          final InputStream s)
          throws OPDSFeedParseException
        {
          throw ex;
        }
      };

      final OPDSFeedTransportType t = new OPDSFeedTransportType() {
        @Override public InputStream getStream(
          final URI uri)
          throws IOException
        {
          return new ByteArrayInputStream("text".getBytes());
        }
      };

      final OPDSFeedLoaderType fl = OPDSFeedLoader.newLoader(e, p, t);
      fl.fromURI(
        new URI("http://example.com"),
        new OPDSFeedLoadListenerType() {
          @Override public void onFeedLoadingFailure(
            final Throwable supplied)
          {
            System.err.println("Caught: " + supplied);
            TestUtilities.assertEquals(ex, supplied);
            caught.set(true);
          }

          @Override public void onFeedLoadingSuccess(
            final OPDSFeedType f)
          {
            throw new UnreachableCodeException();
          }
        });

    } finally {
      e.awaitTermination(1, TimeUnit.SECONDS);
    }

    TestUtilities.assertTrue(caught.get());
  }

  @Override public void testLoaderSuccessCorrect()
    throws Exception
  {
    final URI uri = URI.create("http://example.com/base");

    final AtomicBoolean succeeded = new AtomicBoolean(false);
    final OPDSAcquisitionFeed feed =
      OPDSAcquisitionFeed.newBuilder(
        uri,
        "id",
        Calendar.getInstance(),
        "title").build();
    final ExecutorService e = Executors.newCachedThreadPool();

    try {
      final OPDSFeedParserType p = new OPDSFeedParserType() {
        @Override public OPDSFeedType parse(
          final URI u,
          final InputStream s)
          throws OPDSFeedParseException
        {
          return feed;
        }
      };

      final OPDSFeedTransportType t = new OPDSFeedTransportType() {
        @Override public InputStream getStream(
          final URI u)
          throws IOException
        {
          return new ByteArrayInputStream("text".getBytes());
        }
      };

      final OPDSFeedLoaderType fl = OPDSFeedLoader.newLoader(e, p, t);
      fl.fromURI(
        new URI("http://example.com"),
        new OPDSFeedLoadListenerType() {
          @Override public void onFeedLoadingFailure(
            final Throwable supplied)
          {
            throw new UnreachableCodeException();
          }

          @Override public void onFeedLoadingSuccess(
            final OPDSFeedType f)
          {
            TestUtilities.assertEquals(f, feed);
            succeeded.set(true);
          }
        });

    } finally {
      e.awaitTermination(1, TimeUnit.SECONDS);
    }

    TestUtilities.assertTrue(succeeded.get());
  }
}
