package org.nypl.simplified.books.controller;

import com.io7m.jfunctional.FunctionType;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;

import org.nypl.simplified.books.accounts.AccountEvent;
import org.nypl.simplified.books.accounts.AccountEventLogout;
import org.nypl.simplified.books.accounts.AccountEventLogout.AccountLogoutFailed;
import org.nypl.simplified.books.accounts.AccountEventLogout.AccountLogoutSucceeded;
import org.nypl.simplified.books.accounts.AccountProvider;
import org.nypl.simplified.books.accounts.AccountProviderCollection;
import org.nypl.simplified.books.accounts.AccountType;
import org.nypl.simplified.books.accounts.AccountsDatabaseException;
import org.nypl.simplified.books.book_database.BookDatabaseException;
import org.nypl.simplified.books.book_database.BookID;
import org.nypl.simplified.books.book_registry.BookRegistryType;
import org.nypl.simplified.books.profiles.ProfileNoneCurrentException;
import org.nypl.simplified.books.profiles.ProfileNonexistentAccountProviderException;
import org.nypl.simplified.books.profiles.ProfileReadableType;
import org.nypl.simplified.books.profiles.ProfilesDatabaseType;
import org.nypl.simplified.observable.ObservableType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

import static org.nypl.simplified.books.accounts.AccountEventLogout.AccountLogoutFailed.ErrorCode.ERROR_ACCOUNTS_DATABASE;
import static org.nypl.simplified.books.accounts.AccountEventLogout.AccountLogoutFailed.ErrorCode.ERROR_GENERAL;
import static org.nypl.simplified.books.accounts.AccountEventLogout.AccountLogoutFailed.ErrorCode.ERROR_PROFILE_CONFIGURATION;

final class ProfileAccountLogoutTask implements Callable<AccountEventLogout> {

  private static final Logger LOG = LoggerFactory.getLogger(ProfileAccountLogoutTask.class);

  private final ProfilesDatabaseType profiles;
  private final ObservableType<AccountEvent> account_events;
  private final BookRegistryType book_registry;

  ProfileAccountLogoutTask(
      final ProfilesDatabaseType profiles,
      final BookRegistryType in_book_registry,
      final ObservableType<AccountEvent> account_events) {

    this.profiles =
        NullCheck.notNull(profiles, "Profiles");
    this.book_registry =
        NullCheck.notNull(in_book_registry, "Book registry");
    this.account_events =
        NullCheck.notNull(account_events, "Account events");
  }

  @Override
  public AccountEventLogout call() {
    final AccountEventLogout event = run();
    this.account_events.send(event);
    return event;
  }

  private AccountEventLogout run() {
    try {
      final ProfileReadableType profile = this.profiles.currentProfileUnsafe();
      final AccountType account = profile.accountCurrent();
      return runForAccount(account);
    } catch (final ProfileNoneCurrentException e) {
      return AccountLogoutFailed.of(ERROR_PROFILE_CONFIGURATION, Option.some(e));
    } catch (final AccountsDatabaseException e) {
      return AccountLogoutFailed.of(ERROR_ACCOUNTS_DATABASE, Option.some(e));
    } catch (final IOException e) {
      return AccountLogoutFailed.of(ERROR_GENERAL, Option.some(e));
    }
  }

  private AccountEventLogout runForAccount(
      final AccountType account)
      throws AccountsDatabaseException, IOException {

    LOG.debug("clearing account credentials");
    account.setCredentials(Option.none());
    final Set<BookID> account_books = account.bookDatabase().books();
    try {
      LOG.debug("deleting book database");
      account.bookDatabase().delete();
    } catch (final BookDatabaseException e) {
      LOG.error("deleting book database: ", e);
    } finally {
      LOG.debug("clearing books from book registry");
      for (final BookID book : account_books) {
        this.book_registry.clearFor(book);
      }
    }

    LOG.debug("logged out successfully");
    return AccountLogoutSucceeded.of();
  }

}
