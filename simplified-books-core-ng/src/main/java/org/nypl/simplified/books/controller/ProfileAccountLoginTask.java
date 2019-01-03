package org.nypl.simplified.books.controller;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.PartialFunctionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;

import org.nypl.simplified.books.accounts.AccountAuthenticatedHTTP;
import org.nypl.simplified.books.accounts.AccountAuthenticationCredentials;
import org.nypl.simplified.books.accounts.AccountEvent;
import org.nypl.simplified.books.accounts.AccountEventLogin;
import org.nypl.simplified.books.accounts.AccountEventLogin.AccountLoginFailed;
import org.nypl.simplified.books.accounts.AccountEventLogin.AccountLoginSucceeded;
import org.nypl.simplified.books.accounts.AccountProviderAuthenticationDescription;
import org.nypl.simplified.books.accounts.AccountType;
import org.nypl.simplified.books.accounts.AccountsDatabaseException;
import org.nypl.simplified.books.accounts.AccountsDatabaseNonexistentException;
import org.nypl.simplified.books.profiles.ProfileNoneCurrentException;
import org.nypl.simplified.books.profiles.ProfileReadableType;
import org.nypl.simplified.books.profiles.ProfilesDatabaseType;
import org.nypl.simplified.http.core.HTTPAuthType;
import org.nypl.simplified.http.core.HTTPResultError;
import org.nypl.simplified.http.core.HTTPResultException;
import org.nypl.simplified.http.core.HTTPResultOKType;
import org.nypl.simplified.http.core.HTTPResultType;
import org.nypl.simplified.http.core.HTTPType;
import org.nypl.simplified.observable.ObservableType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.concurrent.Callable;

import static org.nypl.simplified.books.accounts.AccountEventLogin.AccountLoginFailed.ErrorCode.ERROR_CREDENTIALS_INCORRECT;
import static org.nypl.simplified.books.accounts.AccountEventLogin.AccountLoginFailed.ErrorCode.ERROR_NETWORK_EXCEPTION;
import static org.nypl.simplified.books.accounts.AccountEventLogin.AccountLoginFailed.ErrorCode.ERROR_PROFILE_CONFIGURATION;
import static org.nypl.simplified.books.accounts.AccountEventLogin.AccountLoginFailed.ErrorCode.ERROR_SERVER_ERROR;
import static org.nypl.simplified.books.accounts.AccountEventLogin.AccountLoginFailed.ErrorCode.ERROR_ACCOUNT_NONEXISTENT;

final class ProfileAccountLoginTask implements Callable<AccountEventLogin> {

  private static final Logger LOG = LoggerFactory.getLogger(ProfileAccountLoginTask.class);

  private final ProfilesDatabaseType profiles;
  private final AccountAuthenticationCredentials credentials;
  private final BooksControllerType books_controller;
  private final HTTPType http;
  private final ObservableType<AccountEvent> account_events;
  private final PartialFunctionType<ProfileReadableType, AccountType, AccountsDatabaseNonexistentException> account_id_request;

  ProfileAccountLoginTask(
      final BooksControllerType in_books_controller,
      final HTTPType http,
      final ProfilesDatabaseType profiles,
      final ObservableType<AccountEvent> account_events,
      final PartialFunctionType<ProfileReadableType, AccountType, AccountsDatabaseNonexistentException> account_id,
      final AccountAuthenticationCredentials credentials) {

    this.books_controller =
        NullCheck.notNull(in_books_controller, "Books controller");
    this.http =
        NullCheck.notNull(http, "Http");
    this.profiles =
        NullCheck.notNull(profiles, "Profiles");
    this.account_events =
        NullCheck.notNull(account_events, "Account events");
    this.account_id_request =
        NullCheck.notNull(account_id, "Account ID");
    this.credentials =
        NullCheck.notNull(credentials, "Credentials");
  }

  @Override
  public AccountEventLogin call() {
    final AccountEventLogin event = run();
    this.account_events.send(event);
    return event;
  }

  private AccountEventLogin run() {
    try {
      final ProfileReadableType profile = this.profiles.currentProfileUnsafe();
      final AccountType account = this.account_id_request.call(profile);
      return runForAccount(account);
    } catch (final ProfileNoneCurrentException e) {
      return AccountLoginFailed.of(ERROR_PROFILE_CONFIGURATION, Option.some(e));
    } catch (final AccountsDatabaseNonexistentException e) {
      return AccountLoginFailed.of(ERROR_ACCOUNT_NONEXISTENT, Option.some(e));
    }
  }

  private AccountEventLogin runForAccount(
      final AccountType account) {

    final OptionType<AccountProviderAuthenticationDescription> auth_opt =
        account.provider().authentication();
    if (auth_opt.isNone()) {
      LOG.debug("account does not require authentication");
      return AccountLoginSucceeded.of(this.credentials);
    }

    return runHTTPRequest(account, ((Some<AccountProviderAuthenticationDescription>) auth_opt).get());
  }

  /**
   * Hit the login URI using the given authenticated HTTP instance.
   */

  private AccountEventLogin runHTTPRequest(
      final AccountType account,
      final AccountProviderAuthenticationDescription auth) {

    final HTTPAuthType http_auth =
        AccountAuthenticatedHTTP.createAuthenticatedHTTP(this.credentials);
    final HTTPResultType<InputStream> result =
        this.http.head(Option.some(http_auth), auth.loginURI());

    return result.match(
        this::onHTTPError,
        this::onHTTPException,
        http_result -> onHTTPOK(account, http_result));
  }

  private AccountEventLogin onHTTPOK(
      final AccountType account,
      final HTTPResultOKType<InputStream> result) {

    LOG.debug("received http OK: {}", result.getMessage());

    try {
      account.setCredentials(Option.some(this.credentials));
    } catch (final AccountsDatabaseException e) {
      return AccountLoginFailed.of(ERROR_PROFILE_CONFIGURATION, Option.some(e));
    }

    this.books_controller.booksSync(account);
    return AccountLoginSucceeded.of(this.credentials);
  }

  private AccountEventLogin onHTTPException(final HTTPResultException<InputStream> result) {
    LOG.debug("received http exception: {}: ", result.getURI(), result.getError());
    return AccountLoginFailed.of(ERROR_NETWORK_EXCEPTION, Option.some(result.getError()));
  }

  private AccountEventLogin onHTTPError(final HTTPResultError<InputStream> result) {
    LOG.debug("received http error: {}: {}", result.getMessage(), result.getStatus());

    final int code = result.getStatus();
    switch (code) {
      case HttpURLConnection.HTTP_UNAUTHORIZED: {
        return AccountLoginFailed.of(ERROR_CREDENTIALS_INCORRECT, Option.none());
      }
      default: {
        return AccountLoginFailed.of(ERROR_SERVER_ERROR, Option.none());
      }
    }
  }
}
