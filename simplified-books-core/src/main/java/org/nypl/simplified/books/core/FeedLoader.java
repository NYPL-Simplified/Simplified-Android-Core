package org.nypl.simplified.books.core;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Pair;
import com.io7m.jfunctional.Some;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import net.jodah.expiringmap.ExpiringMap;
import net.jodah.expiringmap.ExpiringMap.Builder;
import net.jodah.expiringmap.ExpiringMap.ExpirationListener;
import net.jodah.expiringmap.ExpiringMap.ExpirationPolicy;
import org.nypl.drm.core.Assertions;
import org.nypl.simplified.http.core.HTTPAuthBasic;
import org.nypl.simplified.http.core.HTTPAuthType;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeed;
import org.nypl.simplified.opds.core.OPDSFeedParserType;
import org.nypl.simplified.opds.core.OPDSFeedTransportException;
import org.nypl.simplified.opds.core.OPDSFeedTransportHTTPException;
import org.nypl.simplified.opds.core.OPDSFeedTransportType;
import org.nypl.simplified.opds.core.OPDSOpenSearch1_1;
import org.nypl.simplified.opds.core.OPDSSearchLink;
import org.nypl.simplified.opds.core.OPDSSearchParserType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The default implementation of the {@link FeedLoaderType} interface.
 *
 * This implementation caches feeds. A feed is ejected from the cache if it has
 * not been accessed for five minutes.
 */

public final class FeedLoader
  implements FeedLoaderType, ExpirationListener<URI, FeedType>
{
  private static final Logger LOG;

  static {
    LOG = NullCheck.notNull(LoggerFactory.getLogger(FeedLoader.class));
  }

  private final ExpiringMap<URI, FeedType>                      cache;
  private final ExecutorService                                 exec;
  private final OPDSFeedParserType                              parser;
  private final OPDSSearchParserType                            search_parser;
  private final OPDSFeedTransportType<OptionType<HTTPAuthType>> transport;

  private FeedLoader(
    final ExecutorService in_exec,
    final OPDSFeedParserType in_parser,
    final OPDSFeedTransportType<OptionType<HTTPAuthType>> in_transport,
    final OPDSSearchParserType in_search_parser,
    final ExpiringMap<URI, FeedType> in_m)
  {
    this.exec = NullCheck.notNull(in_exec);
    this.parser = NullCheck.notNull(in_parser);
    this.search_parser = NullCheck.notNull(in_search_parser);
    this.transport = NullCheck.notNull(in_transport);
    this.cache = NullCheck.notNull(in_m);
    this.cache.addExpirationListener(this);
  }

  /**
   * Construct a new feed loader.
   *
   * @param in_exec          An executor
   * @param in_parser        A feed parser
   * @param in_transport     A feed transport
   * @param in_search_parser A search document parser
   *
   * @return A new feed loader
   */

  public static FeedLoaderType newFeedLoader(
    final ExecutorService in_exec,
    final OPDSFeedParserType in_parser,
    final OPDSFeedTransportType<OptionType<HTTPAuthType>> in_transport,
    final OPDSSearchParserType in_search_parser)
  {
    final Builder<Object, Object> b = ExpiringMap.builder();
    b.expirationPolicy(ExpirationPolicy.ACCESSED);
    b.expiration(5L, TimeUnit.MINUTES);
    final ExpiringMap<URI, FeedType> m = b.build();
    return FeedLoader.newFeedLoaderFromExpiringMap(
      in_exec, in_parser, in_transport, in_search_parser, NullCheck.notNull(m));
  }

  /**
   * Construct a feed loader from an existing map.
   *
   * @param in_exec          An executor
   * @param in_parser        A feed parser
   * @param in_transport     A feed transport
   * @param in_search_parser A search document parser
   * @param m                A map
   *
   * @return A new feed loader
   */

  public static FeedLoaderType newFeedLoaderFromExpiringMap(
    final ExecutorService in_exec,
    final OPDSFeedParserType in_parser,
    final OPDSFeedTransportType<OptionType<HTTPAuthType>> in_transport,
    final OPDSSearchParserType in_search_parser,
    final ExpiringMap<URI, FeedType> m)
  {
    return new FeedLoader(
      in_exec, in_parser, in_transport, in_search_parser, m);
  }

  @Override public void expired(
    final @Nullable URI key,
    final @Nullable FeedType value)
  {
    FeedLoader.LOG.debug("expired: {}", key);
  }

  private Future<Unit> fetch(
    final URI uri,
    final OptionType<HTTPAuthType> auth,
    final FeedLoaderListenerType listener)
  {
    FeedLoader.LOG.debug("not cached, fetching: {} (auth {})", uri, auth);

    final Callable<Unit> c = new Callable<Unit>()
    {
      @Override public Unit call()
      {
        final ProtectedListener p_listener = new ProtectedListener(listener);
        try {
          final FeedType f = FeedLoader.this.loadFeed(uri, auth, p_listener);
          FeedLoader.this.cache.put(uri, f);
          FeedLoader.LOG.debug("added to cache: {}", uri);
          p_listener.onFeedLoadSuccess(uri, f);
        } catch (final Throwable x) {
          p_listener.onFeedLoadFailure(uri, x);
        }

        return Unit.unit();
      }
    };

    return NullCheck.notNull(this.exec.submit(c));
  }

  @Override public Future<Unit> fromURI(
    final URI uri,
    final OptionType<HTTPAuthType> auth,
    final FeedLoaderListenerType listener)
  {
    NullCheck.notNull(uri);
    NullCheck.notNull(auth);
    NullCheck.notNull(listener);

    if (this.cache.containsKey(uri)) {
      FeedLoader.LOG.debug("retrieved from cache: {}", uri);
      final FeedType f = NullCheck.notNull(this.cache.get(uri));
      final ProtectedListener p_listener = new ProtectedListener(listener);
      p_listener.onFeedLoadSuccess(uri, f);
      return new ImmediateFuture<Unit>(Unit.unit());
    }

    return this.fetch(uri, auth, listener);
  }

  @Override public Future<Unit> fromURIRefreshing(
    final URI uri,
    final OptionType<HTTPAuthType> auth,
    final FeedLoaderListenerType listener)
  {
    NullCheck.notNull(uri);
    NullCheck.notNull(auth);
    NullCheck.notNull(listener);
    return this.fetch(uri, auth, listener);
  }

  @Override public OPDSFeedParserType getOPDSFeedParser()
  {
    return this.parser;
  }

  @Override
  public OPDSFeedTransportType<OptionType<HTTPAuthType>> getOPDSFeedTransport()
  {
    return this.transport;
  }

  @Override public OPDSSearchParserType getOPDSSearchParser()
  {
    return this.search_parser;
  }

  @Override public void invalidate(
    final URI uri)
  {
    NullCheck.notNull(uri);
    this.cache.remove(uri);
  }

  private FeedType loadFeed(
    final URI uri,
    final OptionType<HTTPAuthType> auth,
    final FeedLoaderListenerType listener)
    throws InterruptedException, OPDSFeedTransportException, IOException
  {
    final AtomicReference<OptionType<HTTPAuthType>> auth_ref =
      new AtomicReference<OptionType<HTTPAuthType>>(auth);

    final InputStream main_stream =
      this.loadFeedStreamRetryingAuth(uri, listener, auth_ref);

    try {
      final OPDSAcquisitionFeed parsed = this.parser.parse(uri, main_stream);

      /**
       * If a search link was provided, fetch the search link and parse it.
       */

      final OptionType<OPDSSearchLink> search_opt = parsed.getFeedSearchURI();

      if (search_opt.isSome()) {
        final Some<OPDSSearchLink> some = (Some<OPDSSearchLink>) search_opt;
        final URI search_uri = some.get().getURI();
        final InputStream search_stream =
          this.loadFeedStreamRetryingAuth(search_uri, listener, auth_ref);
        try {
          final OptionType<OPDSOpenSearch1_1> search =
            Option.some(this.search_parser.parse(search_uri, search_stream));
          return Feeds.fromAcquisitionFeed(parsed, search);
        } finally {
          search_stream.close();
        }
      } else {

        /**
         * Otherwise, return a feed that doesn't have a search link.
         */

        final OptionType<OPDSOpenSearch1_1> none = Option.none();
        return Feeds.fromAcquisitionFeed(parsed, none);
      }

    } finally {
      main_stream.close();
    }
  }

  /**
   * Try to fetch {@code uri}, consulting {@code listener} if authentication
   * details are required. If the final attempt results in a successful
   * authentication attempt, the given credentials are saved in {@code auth}.
   */

  private InputStream loadFeedStreamRetryingAuth(
    final URI uri,
    final FeedLoaderListenerType listener,
    final AtomicReference<OptionType<HTTPAuthType>> auth)
    throws OPDSFeedTransportException, IOException, InterruptedException
  {
    OptionType<HTTPAuthType> auth_current = NullCheck.notNull(auth.get());
    final AtomicInteger attempts = new AtomicInteger();

    while (true) {
      InputStream stream = null;
      try {
        stream = this.transport.getStream(auth_current, uri);
        auth.set(auth_current);
        return stream;
      } catch (final OPDSFeedTransportHTTPException e) {
        try {
          if (e.getCode() == 401) {
            final HTTPAuthBasic basic =
              this.getCredentialsAfterError(uri, listener, attempts, e);
            auth_current = Option.some((HTTPAuthType) basic);
          } else {
            throw e;
          }
        } finally {
          if (stream != null) {
            stream.close();
          }
        }
      }
    }
  }

  /**
   * Ask {@code listener} for credentials after an initial authentication error
   * has been encountered. If no credentials are provided, {@code e} is
   * rethrown.
   */

  private HTTPAuthBasic getCredentialsAfterError(
    final URI uri,
    final FeedLoaderListenerType listener,
    final AtomicInteger attempts,
    final OPDSFeedTransportHTTPException e)
    throws InterruptedException, OPDSFeedTransportHTTPException
  {
    /**
     * Call a blocking auth listener and wait for authentication data
     * to be provided.
     */

    final BlockingAuthenticationListener auth_listener =
      new BlockingAuthenticationListener();
    listener.onFeedRequiresAuthentication(
      uri, attempts.getAndIncrement(), auth_listener);

    FeedLoader.LOG.trace("waiting for auth listener completion");
    auth_listener.waitForCompletion(60L, TimeUnit.SECONDS);
    FeedLoader.LOG.trace("finished waiting for completion");

    /**
     * If no authentication data was provided, the feed can't be loaded.
     */

    final OptionType<Pair<AccountBarcode, AccountPIN>> result_opt =
      auth_listener.getResult();
    if (result_opt.isNone()) {
      throw e;
    }

    /**
     * Otherwise, record the provided credentials and try again.
     */

    final Some<Pair<AccountBarcode, AccountPIN>> result_some =
      (Some<Pair<AccountBarcode, AccountPIN>>) result_opt;
    final Pair<AccountBarcode, AccountPIN> result = result_some.get();
    final String user = result.getLeft().toString();
    final String pass = result.getRight().toString();
    return new HTTPAuthBasic(user, pass);
  }

  private static final class ProtectedListener implements FeedLoaderListenerType
  {
    private final FeedLoaderListenerType delegate;

    private ProtectedListener(final FeedLoaderListenerType in_delegate)
    {
      this.delegate = NullCheck.notNull(in_delegate);
    }

    @Override public void onFeedLoadFailure(
      final URI u,
      final Throwable x)
    {
      try {
        this.delegate.onFeedLoadFailure(u, x);
      } catch (final Throwable xe) {
        FeedLoader.LOG.error("listener raised error: ", xe);
      }
    }

    @Override public void onFeedLoadSuccess(
      final URI u,
      final FeedType f)
    {
      try {
        this.delegate.onFeedLoadSuccess(u, f);
      } catch (final Throwable x) {
        this.onFeedLoadFailure(u, x);
      }
    }

    @Override public void onFeedRequiresAuthentication(
      final URI u,
      final int attempts,
      final FeedLoaderAuthenticationListenerType listener)
    {
      try {
        this.delegate.onFeedRequiresAuthentication(u, attempts, listener);
      } catch (final Throwable x) {
        this.onFeedLoadFailure(u, x);
      }
    }
  }

  private static final class ImmediateFuture<T> implements Future<T>
  {
    private final T value;

    private ImmediateFuture(
      final T in_value)
    {
      this.value = NullCheck.notNull(in_value);
    }

    @Override public boolean cancel(
      final boolean x)
    {
      return false;
    }

    @Override public T get()
      throws InterruptedException, ExecutionException
    {
      return this.value;
    }

    @Override public T get(
      final long time,
      final @Nullable TimeUnit time_unit)
      throws InterruptedException, ExecutionException, TimeoutException
    {
      return this.value;
    }

    @Override public boolean isCancelled()
    {
      return false;
    }

    @Override public boolean isDone()
    {
      return true;
    }
  }

  /**
   * An authentication listener that allows external threads to wait until one
   * of the listener methods has been called.
   */

  private final class BlockingAuthenticationListener
    implements FeedLoaderAuthenticationListenerType
  {
    private final CountDownLatch latch;
    private final AtomicReference<OptionType<Pair<AccountBarcode, AccountPIN>>>
                                 result;

    public BlockingAuthenticationListener()
    {
      this.latch = new CountDownLatch(1);
      final OptionType<Pair<AccountBarcode, AccountPIN>> none = Option.none();
      this.result =
        new AtomicReference<OptionType<Pair<AccountBarcode, AccountPIN>>>(none);
    }

    /**
     * @return The result, if the listener has completed
     */

    public OptionType<Pair<AccountBarcode, AccountPIN>> getResult()
    {
      Assertions.checkPrecondition(
        this.latch.getCount() == 0, "Listener has not completed");
      return this.result.get();
    }

    /**
     * Wait until another thread calls {@link #completeNow()}.
     *
     * @param unit The time unit
     * @param time The time to wait
     *
     * @throws InterruptedException If waiting is interrupted
     */

    public void waitForCompletion(
      final long time,
      final TimeUnit unit)
      throws InterruptedException
    {
      this.latch.await(time, unit);
    }

    /**
     * Complete the authentication process. Any thread blocking on this listener
     * will be able to retrieve results, if any were provided.
     */

    public void completeNow()
    {
      Assertions.checkPrecondition(
        this.latch.getCount() == 1, "Listener has already completed");
      this.latch.countDown();
    }

    @Override public void onAuthenticationProvided(
      final AccountBarcode user,
      final AccountPIN password)
    {
      this.result.set(Option.some(Pair.pair(user, password)));
      this.completeNow();
    }

    @Override public void onAuthenticationNotProvided()
    {
      this.completeNow();
    }

    @Override public void onAuthenticationError(
      final OptionType<Throwable> error,
      final String message)
    {
      LogUtilities.errorWithOptionalException(FeedLoader.LOG, message, error);
      this.completeNow();
    }
  }
}
