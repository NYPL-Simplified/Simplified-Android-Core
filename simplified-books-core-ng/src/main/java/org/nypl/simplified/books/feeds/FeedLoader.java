package org.nypl.simplified.books.feeds;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.io7m.junreachable.UnreachableCodeException;

import net.jodah.expiringmap.ExpiringMap;
import net.jodah.expiringmap.ExpiringMap.Builder;
import net.jodah.expiringmap.ExpiringMap.ExpirationListener;
import net.jodah.expiringmap.ExpiringMap.ExpirationPolicy;

import org.nypl.drm.core.Assertions;
import org.nypl.simplified.books.accounts.AccountAuthenticatedHTTP;
import org.nypl.simplified.books.accounts.AccountAuthenticationCredentials;
import org.nypl.simplified.books.book_database.BookID;
import org.nypl.simplified.books.book_registry.BookRegistryReadableType;
import org.nypl.simplified.books.book_registry.BookWithStatus;
import org.nypl.simplified.books.bundled_content.BundledContentResolverType;
import org.nypl.simplified.books.bundled_content.BundledURIs;
import org.nypl.simplified.books.logging.LogUtilities;
import org.nypl.simplified.http.core.HTTPAuthType;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeed;
import org.nypl.simplified.opds.core.OPDSFeedParserType;
import org.nypl.simplified.opds.core.OPDSFeedTransportException;
import org.nypl.simplified.opds.core.OPDSFeedTransportType;
import org.nypl.simplified.opds.core.OPDSOpenSearch1_1;
import org.nypl.simplified.opds.core.OPDSSearchLink;
import org.nypl.simplified.opds.core.OPDSSearchParserType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The default implementation of the {@link FeedLoaderType} interface.
 * <p>
 * This implementation caches feeds. A feed is ejected from the cache if it has
 * not been accessed for five minutes.
 */

public final class FeedLoader implements FeedLoaderType, ExpirationListener<URI, FeedType> {
  private static final Logger LOG;

  static {
    LOG = NullCheck.notNull(LoggerFactory.getLogger(FeedLoader.class));
  }

  private final ExpiringMap<URI, FeedType> cache;
  private final ListeningExecutorService exec;
  private final OPDSFeedParserType parser;
  private final OPDSSearchParserType search_parser;
  private final OPDSFeedTransportType<OptionType<HTTPAuthType>> transport;
  private final BookRegistryReadableType book_registry;
  private final BundledContentResolverType bundled_content;

  private FeedLoader(
      final ExecutorService in_exec,
      final BookRegistryReadableType in_book_registry,
      final BundledContentResolverType in_bundled_content,
      final OPDSFeedParserType in_parser,
      final OPDSFeedTransportType<OptionType<HTTPAuthType>> in_transport,
      final OPDSSearchParserType in_search_parser,
      final ExpiringMap<URI, FeedType> in_m) {

    this.exec =
        MoreExecutors.listeningDecorator(NullCheck.notNull(in_exec));
    this.book_registry =
        NullCheck.notNull(in_book_registry);
    this.bundled_content =
        NullCheck.notNull(in_bundled_content, "in_bundled_content");
    this.parser =
        NullCheck.notNull(in_parser);
    this.search_parser =
        NullCheck.notNull(in_search_parser);
    this.transport =
        NullCheck.notNull(in_transport);
    this.cache =
        NullCheck.notNull(in_m);

    this.cache.addExpirationListener(this);
  }

  /**
   * Construct a new feed loader.
   *
   * @param in_exec            An executor
   * @param in_book_registry   A book registry
   * @param in_bundled_content A resolver for bundled content
   * @param in_parser          A feed parser
   * @param in_transport       A feed transport
   * @param in_search_parser   A search document parser
   * @return A new feed loader
   */

  public static FeedLoaderType newFeedLoader(
      final ExecutorService in_exec,
      final BookRegistryReadableType in_book_registry,
      final BundledContentResolverType in_bundled_content,
      final OPDSFeedParserType in_parser,
      final OPDSFeedTransportType<OptionType<HTTPAuthType>> in_transport,
      final OPDSSearchParserType in_search_parser) {

    final Builder<Object, Object> b = ExpiringMap.builder();
    b.expirationPolicy(ExpirationPolicy.CREATED);
    b.expiration(5L, TimeUnit.MINUTES);
    final ExpiringMap<URI, FeedType> m = b.build();
    return FeedLoader.newFeedLoaderFromExpiringMap(
        in_exec,
        in_book_registry,
        in_bundled_content,
        in_parser,
        in_transport,
        in_search_parser,
        NullCheck.notNull(m, "Map"));
  }

  /**
   * Construct a feed loader from an existing map.
   *
   * @param in_exec          An executor
   * @param in_book_registry A book registry
   * @param in_parser        A feed parser
   * @param in_transport     A feed transport
   * @param in_search_parser A search document parser
   * @param m                A map
   * @return A new feed loader
   */

  public static FeedLoaderType newFeedLoaderFromExpiringMap(
      final ExecutorService in_exec,
      final BookRegistryReadableType in_book_registry,
      final BundledContentResolverType in_bundled_content,
      final OPDSFeedParserType in_parser,
      final OPDSFeedTransportType<OptionType<HTTPAuthType>> in_transport,
      final OPDSSearchParserType in_search_parser,
      final ExpiringMap<URI, FeedType> m) {

    return new FeedLoader(
        in_exec, in_book_registry, in_bundled_content, in_parser, in_transport, in_search_parser, m);
  }

  private static void updateFeedFromBookRegistry(
      final BookRegistryReadableType registry,
      final FeedType f) {

    f.matchFeed(
        new FeedMatcherType<Unit, UnreachableCodeException>() {
          @Override
          public Unit onFeedWithGroups(final FeedWithGroups feed_with_groups) {
            final int size = feed_with_groups.size();
            LOG.debug("updating {} entries (with groups) from book registry", size);

            for (int index = 0; index < size; ++index) {
              final FeedGroup group = feed_with_groups.get(index);
              final List<FeedEntryType> entries = group.getGroupEntries();
              for (int gi = 0; gi < entries.size(); ++gi) {
                final FeedEntryType e = entries.get(gi);
                final BookID id = e.getBookID();
                final BookWithStatus book_with_status = registry.books().get(id);
                if (book_with_status != null) {
                  LOG.debug("updating entry {} from book registry", id);
                  entries.set(gi, FeedEntryOPDS.fromOPDSAcquisitionFeedEntry(
                      book_with_status.book().entry()));
                }
              }
            }
            return Unit.unit();
          }

          @Override
          public Unit onFeedWithoutGroups(final FeedWithoutGroups feed_without_groups) {
            final int size = feed_without_groups.size();
            LOG.debug("updating {} entries (without groups) from book registry", size);

            for (int index = 0; index < size; ++index) {
              final FeedEntryType e = feed_without_groups.get(index);
              final BookID id = e.getBookID();
              final BookWithStatus book_with_status = registry.books().get(id);
              if (book_with_status != null) {
                LOG.debug("updating entry {} from book registry", id);
                final FeedEntryType en =
                    FeedEntryOPDS.fromOPDSAcquisitionFeedEntry(book_with_status.book().entry());
                feed_without_groups.set(index, en);
              }
            }

            return Unit.unit();
          }
        });
  }

  @Override
  public void expired(
      final @Nullable URI key,
      final @Nullable FeedType value) {
    LOG.debug("expired: {}", key);
  }

  private ListenableFuture<FeedType> fetch(
      final URI uri,
      final String method,
      final OptionType<HTTPAuthType> auth,
      final FeedLoaderListenerType listener,
      final boolean update_from_database) {
    LOG.debug("not cached, fetching ({}): {} (auth {})", method, uri, auth);
    return this.exec.submit(() -> fetchInner(uri, method, auth, listener, update_from_database));
  }

  private FeedType fetchInner(
      final URI uri,
      final String method,
      final OptionType<HTTPAuthType> auth,
      final FeedLoaderListenerType listener,
      final boolean update_from_database) throws Exception {

    final ProtectedListener p_listener = new ProtectedListener(listener);
    try {
      final FeedType f = this.loadFeed(uri, method, auth, p_listener);
      if (update_from_database) {
        FeedLoader.updateFeedFromBookRegistry(this.book_registry, f);
      }
      this.cache.put(uri, f);
      LOG.debug("added to cache: {} ({} entries)", uri, f.size());
      p_listener.onFeedLoadSuccess(uri, f);
      return f;
    } catch (final Exception x) {
      p_listener.onFeedLoadFailure(uri, x);
      throw x;
    }
  }

  @Override
  public ListenableFuture<FeedType> fromURI(
      final URI uri,
      final OptionType<HTTPAuthType> auth,
      final FeedLoaderListenerType listener) {

    NullCheck.notNull(uri);
    NullCheck.notNull(auth);
    NullCheck.notNull(listener);

    if (this.cache.containsKey(uri)) {
      LOG.debug("retrieved from cache: {}", uri);
      final FeedType f = NullCheck.notNull(this.cache.get(uri));
      final ProtectedListener p_listener = new ProtectedListener(listener);
      p_listener.onFeedLoadSuccess(uri, f);
      return Futures.immediateFuture(f);
    }

    return this.fetch(uri, "GET", auth, listener, false);
  }

  @Override
  public ListenableFuture<FeedType> fromURIRefreshing(
      final URI uri,
      final OptionType<HTTPAuthType> auth,
      final String method,
      final FeedLoaderListenerType listener) {
    NullCheck.notNull(uri);
    NullCheck.notNull(auth);
    NullCheck.notNull(listener);
    return this.fetch(uri, method, auth, listener, false);
  }

  @Override
  public ListenableFuture<FeedType> fromURIWithBookRegistryEntries(
      final URI uri,
      final OptionType<HTTPAuthType> auth,
      final FeedLoaderListenerType listener) {
    NullCheck.notNull(uri);
    NullCheck.notNull(auth);
    NullCheck.notNull(listener);

    if (this.cache.containsKey(uri)) {
      LOG.debug("retrieved from cache: {}", uri);
      final FeedType f = NullCheck.notNull(this.cache.get(uri));
      FeedLoader.updateFeedFromBookRegistry(this.book_registry, f);
      final ProtectedListener p_listener = new ProtectedListener(listener);
      p_listener.onFeedLoadSuccess(uri, f);
      return Futures.immediateFuture(f);
    }

    return this.fetch(uri, "GET", auth, listener, true);
  }

  @Override
  public void invalidate(final URI uri) {
    NullCheck.notNull(uri);
    this.cache.remove(uri);
  }

  private FeedType loadFeed(
      final URI uri,
      final String method,
      final OptionType<HTTPAuthType> auth,
      final FeedLoaderListenerType listener)
      throws InterruptedException, OPDSFeedTransportException, IOException {

    /*
     * If the URI has a scheme that refers to bundled content, fetch the data from
     * the resolver instead.
     */

    if (BundledURIs.isBundledURI(uri)) {
      try (final InputStream stream = this.bundled_content.resolve(uri)) {
        return Feeds.fromAcquisitionFeed(this.parser.parse(uri, stream), Option.none());
      }
    }

    final AtomicReference<OptionType<HTTPAuthType>> auth_ref =
        new AtomicReference<OptionType<HTTPAuthType>>(auth);

    try (InputStream main_stream =
             this.loadFeedStreamRetryingAuth(uri, method, listener, auth_ref)) {
      final OPDSAcquisitionFeed parsed = this.parser.parse(uri, main_stream);

      /*
       * If a search link was provided, fetch the search link and parse it.
       */

      final OptionType<OPDSSearchLink> search_opt = parsed.getFeedSearchURI();

      if (search_opt.isSome()) {
        final Some<OPDSSearchLink> some = (Some<OPDSSearchLink>) search_opt;
        final URI search_uri = some.get().getURI();

        try (InputStream search_stream =
                 this.loadFeedStreamRetryingAuth(search_uri, method, listener, auth_ref)) {
          final OptionType<OPDSOpenSearch1_1> search =
              Option.some(this.search_parser.parse(search_uri, search_stream));
          return Feeds.fromAcquisitionFeed(parsed, search);
        }
      } else {

        /*
         * Otherwise, return a feed that doesn't have a search link.
         */

        final OptionType<OPDSOpenSearch1_1> none = Option.none();
        return Feeds.fromAcquisitionFeed(parsed, none);
      }
    }
  }

  /**
   * Try to fetch {@code uri}, consulting {@code listener} if authentication
   * details are required. If the final attempt results in a successful
   * authentication attempt, the given credentials are saved in {@code auth}.
   */

  private InputStream loadFeedStreamRetryingAuth(
      final URI uri,
      final String method,
      final FeedLoaderListenerType listener,
      final AtomicReference<OptionType<HTTPAuthType>> auth)
      throws OPDSFeedTransportException, IOException, InterruptedException {
    OptionType<HTTPAuthType> auth_current = NullCheck.notNull(auth.get());
    final AtomicInteger attempts = new AtomicInteger();

    while (true) {
      InputStream stream = null;
      try {
        LOG.debug("fetching stream for {}", uri);
        stream = this.transport.getStream(auth_current, uri, method);
        LOG.debug("received stream for {}", uri);
        auth.set(auth_current);
        return stream;
      } catch (final FeedHTTPTransportException e) {
        try {
          if (e.getCode() == 401) {
            final HTTPAuthType basic =
                this.getCredentialsAfterError(uri, listener, attempts, e);
            auth_current = Option.some(basic);
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

  private HTTPAuthType getCredentialsAfterError(
      final URI uri,
      final FeedLoaderListenerType listener,
      final AtomicInteger attempts,
      final FeedHTTPTransportException e)
      throws InterruptedException, FeedHTTPTransportException {
    /*
     * Call a blocking auth listener and wait for authentication data
     * to be provided.
     */

    final BlockingAuthenticationListener auth_listener =
        new BlockingAuthenticationListener();
    listener.onFeedRequiresAuthentication(
        uri, attempts.getAndIncrement(), auth_listener);

    LOG.trace("waiting for auth listener completion");
    auth_listener.waitForCompletion(5L, TimeUnit.MINUTES);
    LOG.trace("finished waiting for completion");

    /*
     * If no authentication data was provided, the feed can't be loaded.
     */

    final OptionType<AccountAuthenticationCredentials> result_opt = auth_listener.getResult();
    if (result_opt.isNone()) {
      throw e;
    }

    /*
     * Otherwise, try with the provided credentials.
     */

    final Some<AccountAuthenticationCredentials> result_some =
        (Some<AccountAuthenticationCredentials>) result_opt;

    return AccountAuthenticatedHTTP.createAuthenticatedHTTP(result_some.get());
  }

  private static final class ProtectedListener implements FeedLoaderListenerType {
    private final FeedLoaderListenerType delegate;

    private ProtectedListener(final FeedLoaderListenerType in_delegate) {
      this.delegate = NullCheck.notNull(in_delegate);
    }

    @Override
    public void onFeedLoadFailure(
        final URI u,
        final Throwable x) {
      try {
        this.delegate.onFeedLoadFailure(u, x);
      } catch (final Throwable xe) {
        LOG.error("listener raised error: ", xe);
      }
    }

    @Override
    public void onFeedLoadSuccess(
        final URI u,
        final FeedType f) {
      try {
        this.delegate.onFeedLoadSuccess(u, f);
      } catch (final Throwable x) {
        this.onFeedLoadFailure(u, x);
      }
    }

    @Override
    public void onFeedRequiresAuthentication(
        final URI u,
        final int attempts,
        final FeedLoaderAuthenticationListenerType listener) {
      try {
        this.delegate.onFeedRequiresAuthentication(u, attempts, listener);
      } catch (final Throwable x) {
        this.onFeedLoadFailure(u, x);
      }
    }
  }

  /**
   * An authentication listener that allows external threads to wait until one
   * of the listener methods has been called.
   */

  private static final class BlockingAuthenticationListener
      implements FeedLoaderAuthenticationListenerType {

    private final CountDownLatch latch;
    private final AtomicReference<OptionType<AccountAuthenticationCredentials>> result;

    BlockingAuthenticationListener() {
      this.latch = new CountDownLatch(1);
      final OptionType<AccountAuthenticationCredentials> none = Option.none();
      this.result = new AtomicReference<>(none);
    }

    /**
     * @return The result, if the listener has completed
     */

    public OptionType<AccountAuthenticationCredentials> getResult() {
      return this.result.get();
    }

    /**
     * Wait until another thread calls {@link #completeNow()}.
     *
     * @param unit The time unit
     * @param time The time to wait
     * @throws InterruptedException If waiting is interrupted
     */

    public void waitForCompletion(
        final long time,
        final TimeUnit unit)
        throws InterruptedException {
      this.latch.await(time, unit);
    }

    /**
     * Complete the authentication process. Any thread blocking on this listener
     * will be able to retrieve results, if any were provided.
     */

    public void completeNow() {
      Assertions.checkPrecondition(
          this.latch.getCount() == 1, "Listener has already completed");
      this.latch.countDown();
    }

    @Override
    public void onAuthenticationProvided(
        final AccountAuthenticationCredentials credentials) {
      this.result.set(Option.some(credentials));
      this.completeNow();
    }

    @Override
    public void onAuthenticationNotProvided() {
      this.completeNow();
    }

    @Override
    public void onAuthenticationError(
        final OptionType<Throwable> error,
        final String message) {
      LogUtilities.errorWithOptionalException(LOG, message, error);
      this.completeNow();
    }
  }
}
