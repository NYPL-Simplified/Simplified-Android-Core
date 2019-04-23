package org.nypl.simplified.books.accounts;

import android.content.Context;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.io7m.jfunctional.FunctionType;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Unit;

import org.nypl.simplified.books.book_database.BookDatabaseException;
import org.nypl.simplified.books.book_database.BookDatabaseFactoryType;
import org.nypl.simplified.books.book_database.BookDatabaseType;
import org.nypl.simplified.files.DirectoryUtilities;
import org.nypl.simplified.files.FileLocking;
import org.nypl.simplified.files.FileUtilities;
import org.nypl.simplified.observable.ObservableType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.annotation.concurrent.GuardedBy;

/**
 * The default implementation of the {@link AccountsDatabaseType} interface.
 */

public final class AccountsDatabase implements AccountsDatabaseType {

  private static final Logger LOG = LoggerFactory.getLogger(AccountsDatabase.class);

  private final Context context;
  private final File directory;
  private final Object accounts_lock;
  private final ObservableType<AccountEvent> account_events;
  private final @GuardedBy("accounts_lock")
  SortedMap<AccountID, Account> accounts;
  private final @GuardedBy("accounts_lock")
  SortedMap<URI, Account> accounts_by_provider;
  private final @GuardedBy("accounts_lock")
  SortedMap<AccountID, AccountType> accounts_read;
  private final @GuardedBy("accounts_lock")
  SortedMap<URI, AccountType> accounts_by_provider_read;
  private final BookDatabaseFactoryType book_databases;

  private AccountsDatabase(
    final Context context,
    final File directory,
    final ObservableType<AccountEvent> account_events,
    final SortedMap<AccountID, Account> accounts,
    final SortedMap<URI, Account> accounts_by_provider,
    final BookDatabaseFactoryType book_databases) {

    this.context =
      Objects.requireNonNull(context, "context");
    this.directory =
      Objects.requireNonNull(directory, "directory");
    this.account_events =
      Objects.requireNonNull(account_events, "account_events");
    this.accounts =
      Objects.requireNonNull(accounts, "accounts");
    this.accounts_by_provider =
      Objects.requireNonNull(accounts_by_provider, "accounts_by_provider");
    this.book_databases =
      Objects.requireNonNull(book_databases, "book databases");
    this.accounts_read =
      castMap(Collections.unmodifiableSortedMap(accounts));
    this.accounts_by_provider_read =
      castMap(Collections.unmodifiableSortedMap(accounts_by_provider));
    this.accounts_lock = new Object();
  }

  /**
   * Open an accounts database from the given directory, creating a new database if one does not exist.
   *
   * @param book_databases A factory for book databases
   * @param directory      The directory
   * @return An accounts database
   * @throws AccountsDatabaseException If any errors occurred whilst trying to open the database
   */

  public static AccountsDatabaseType open(
    final Context context,
    final ObservableType<AccountEvent> account_events,
    final BookDatabaseFactoryType book_databases,
    final AccountProviderCollectionType account_providers,
    final File directory)
    throws AccountsDatabaseException {

    Objects.requireNonNull(context, "Context");
    Objects.requireNonNull(account_events, "Account events");
    Objects.requireNonNull(book_databases, "Book databases");
    Objects.requireNonNull(directory, "Directory");
    Objects.requireNonNull(account_providers, "Account providers");

    LOG.debug("opening account database: {}", directory);

    final SortedMap<AccountID, Account> accounts = new ConcurrentSkipListMap<>();
    final SortedMap<URI, Account> accounts_by_provider = new ConcurrentSkipListMap<>();
    final ObjectMapper jom = new ObjectMapper();

    final List<Exception> errors = new ArrayList<>();
    if (!directory.exists()) {
      directory.mkdirs();
    }

    if (!directory.isDirectory()) {
      errors.add(new IOException("Not a directory: " + directory));
    }

    openAllAccounts(
      context,
      account_events,
      book_databases,
      account_providers,
      directory,
      accounts,
      accounts_by_provider,
      jom,
      errors);

    if (!errors.isEmpty()) {
      for (final Exception e : errors) {
        LOG.error("error during account database open: ", e);
      }

      throw new AccountsDatabaseOpenException(
        "One or more errors occurred whilst trying to open the account database.", errors);
    }

    return new AccountsDatabase(
      context,
      directory,
      account_events,
      accounts,
      accounts_by_provider,
      book_databases);
  }

  private static void openAllAccounts(
    final Context context,
    final ObservableType<AccountEvent> account_events,
    final BookDatabaseFactoryType book_databases,
    final AccountProviderCollectionType account_providers,
    final File directory,
    final SortedMap<AccountID, Account> accounts,
    final SortedMap<URI, Account> accounts_by_provider,
    final ObjectMapper jom,
    final List<Exception> errors) {

    final String[] account_dirs = directory.list();
    if (account_dirs != null) {
      for (int index = 0; index < account_dirs.length; ++index) {
        final String account_id_name = account_dirs[index];
        LOG.debug("opening account: {}/{}", directory, account_id_name);

        final Account account =
          openOneAccount(
            context,
            account_events,
            book_databases,
            account_providers,
            directory,
            jom,
            errors,
            account_id_name);

        if (account != null) {
          final Account existing_account = accounts_by_provider.get(account.provider.id());
          if (existing_account != null) {
            final String message =
              new StringBuilder(128)
                .append("Multiple accounts using the same provider.")
                .append("\n")
                .append("  Provider: ")
                .append(account.provider.id())
                .append("\n")
                .append("  Existing Account: ")
                .append(existing_account.id.id())
                .append("\n")
                .append("  Opening Account: ")
                .append(account.id.id())
                .append("\n")
                .toString();
            LOG.error("{}", message);

            try {
              account.delete();
            } catch (final AccountsDatabaseIOException e) {
              LOG.error("could not delete broken account: ", e);
            }

            continue;
          }

          accounts.put(account.id, account);
          accounts_by_provider.put(account.provider().id(), account);
        }
      }
    }
  }

  private static Account openOneAccount(
    final Context context,
    final ObservableType<AccountEvent> account_events,
    final BookDatabaseFactoryType book_databases,
    final AccountProviderCollectionType account_providers,
    final File directory,
    final ObjectMapper jom,
    final List<Exception> errors,
    final String account_id_name) {

    final int id;
    try {
      id = Integer.parseInt(account_id_name);
    } catch (final NumberFormatException e) {
      errors.add(new IOException("Could not parse directory name as an account ID", e));
      return null;
    }

    final AccountID account_id = AccountID.create(id);
    final File account_dir = new File(directory, account_id_name);
    final File account_file = new File(account_dir, "account.json");
    final File books_dir = new File(account_dir, "books");

    try {
      final BookDatabaseType book_database =
        book_databases.openDatabase(context, account_id, books_dir);
      final AccountDescription desc =
        AccountDescriptionJSON.deserializeFromFile(jom, account_file);
      final AccountProvider provider =
        account_providers.provider(desc.provider());

      return new Account(account_id, account_dir, account_events, desc, provider, book_database);
    } catch (final IOException e) {
      errors.add(new IOException("Could not parse account: " + account_file, e));
      return null;
    } catch (final AccountsDatabaseNonexistentProviderException e) {
      LOG.error("could not open account: {}: ", account_file, e);
      return null;
    } catch (final BookDatabaseException e) {
      errors.add(e);
      return null;
    }
  }

  /**
   * Perform an unchecked (but safe) cast of the given map type. The cast is safe because
   * {@code V <: VB}.
   */

  @SuppressWarnings("unchecked")
  private static <K, VB, V extends VB> SortedMap<K, VB> castMap(final SortedMap<K, V> m) {
    return (SortedMap<K, VB>) m;
  }

  @Override
  public File directory() {
    return this.directory;
  }

  @Override
  public SortedMap<AccountID, AccountType> accounts() {
    synchronized (this.accounts_lock) {
      return this.accounts_read;
    }
  }

  @Override
  public SortedMap<URI, AccountType> accountsByProvider() {
    synchronized (this.accounts_lock) {
      return this.accounts_by_provider_read;
    }
  }

  @Override
  public AccountType createAccount(final AccountProvider account_provider)
    throws AccountsDatabaseException {

    Objects.requireNonNull(account_provider, "Account provider");

    final AccountID next;
    synchronized (this.accounts_lock) {
      if (!this.accounts.isEmpty()) {
        next = AccountID.create(this.accounts.lastKey().id() + 1);
      } else {
        next = AccountID.create(0);
      }

      LOG.debug("creating account {} (provider {})", next.id(), account_provider.id());

      Preconditions.checkArgument(
        !this.accounts.containsKey(next),
        "Account ID %s cannot have been used", next);
    }

    try {
      final File account_dir =
        new File(this.directory, Integer.toString(next.id()));
      final File account_lock =
        new File(account_dir, "lock");
      final File account_file =
        new File(account_dir, "account.json");
      final File account_file_tmp =
        new File(account_dir, "account.json.tmp");
      final File books_dir =
        new File(account_dir, "books");

      account_dir.mkdirs();

      final BookDatabaseType book_database =
        this.book_databases.openDatabase(this.context, next, books_dir);

      final AccountPreferences preferences =
        new AccountPreferences(false);

      final AccountDescription desc =
        AccountDescription.builder(account_provider.id(), preferences)
          .build();

      writeDescription(account_lock, account_file, account_file_tmp, desc);

      synchronized (this.accounts_lock) {
        final Account account =
          new Account(next, account_dir, account_events, desc, account_provider, book_database);
        this.accounts.put(next, account);
        this.accounts_by_provider.put(account_provider.id(), account);
        return account;
      }
    } catch (final IOException e) {
      throw new AccountsDatabaseIOException("Could not write account data", e);
    } catch (final BookDatabaseException e) {
      throw new AccountsDatabaseBooksException("Could not create book database", e);
    }
  }

  @Override
  public AccountID deleteAccountByProvider(final AccountProvider account_provider)
    throws AccountsDatabaseException {

    Objects.requireNonNull(account_provider, "Account provider");

    LOG.debug("delete account by provider: {}", account_provider.id());

    synchronized (this.accounts_lock) {
      if (!this.accounts_by_provider.containsKey(account_provider.id())) {
        throw new AccountsDatabaseNonexistentException(
          "No account with the given provider");
      }
      if (this.accounts.size() == 1) {
        throw new AccountsDatabaseLastAccountException(
          "At most one account must exist at any given time");
      }

      final Account account = this.accounts_by_provider.get(account_provider.id());
      this.accounts.remove(account.id());
      this.accounts_by_provider.remove(account_provider.id());

      try {
        DirectoryUtilities.directoryDelete(account.directory());
        return account.id();
      } catch (final IOException e) {
        throw new AccountsDatabaseIOException(e.getMessage(), e);
      }
    }
  }

  private static void writeDescription(
    final File account_lock,
    final File account_file,
    final File account_file_tmp,
    final AccountDescription desc)
    throws IOException {

    FileLocking.withFileThreadLocked(
      account_lock, 1000L, ignored -> {
        FileUtilities.fileWriteUTF8Atomically(
          account_file,
          account_file_tmp,
          AccountDescriptionJSON.serializeToString(new ObjectMapper(), desc));
        return Unit.unit();
      });
  }

  private static final class Account implements AccountType {

    private final AccountID id;
    private final File directory;
    private final ObservableType<AccountEvent> account_events;
    private final Object description_lock;
    private @GuardedBy("description_lock")
    AccountDescription description;
    private final BookDatabaseType book_database;
    private final AccountProvider provider;

    Account(
      final AccountID id,
      final File directory,
      final ObservableType<AccountEvent> account_events,
      final AccountDescription description,
      final AccountProvider provider,
      final BookDatabaseType book_database) {

      this.id =
        Objects.requireNonNull(id, "id");
      this.directory =
        Objects.requireNonNull(directory, "directory");
      this.account_events =
        Objects.requireNonNull(account_events, "account_events");
      this.description =
        Objects.requireNonNull(description, "description");
      this.book_database =
        Objects.requireNonNull(book_database, "book database");
      this.provider =
        Objects.requireNonNull(provider, "provider");

      this.description_lock = new Object();
    }

    @Override
    public AccountID id() {
      return this.id;
    }

    @Override
    public File directory() {
      return this.directory;
    }

    @Override
    public AccountProvider provider() {
      synchronized (this.description_lock) {
        return this.provider;
      }
    }

    @Override
    public OptionType<AccountAuthenticationCredentials> credentials() {
      synchronized (this.description_lock) {
        return this.description.credentials();
      }
    }

    @Override
    public AccountPreferences preferences() {
      synchronized (this.description_lock) {
        return this.description.preferences();
      }
    }

    @Override
    public BookDatabaseType bookDatabase() {
      return this.book_database;
    }

    @Override
    public void setCredentials(
      final OptionType<AccountAuthenticationCredentials> credentials)
      throws AccountsDatabaseException {
      Objects.requireNonNull(credentials, "credentials");

      this.setDescription(
        description -> description.toBuilder().setCredentials(credentials).build());
    }

    @Override
    public void setPreferences(
      final AccountPreferences preferences)
      throws AccountsDatabaseException {
      Objects.requireNonNull(preferences, "preferences");

      this.setDescription(
        description -> description.toBuilder().setPreferences(preferences).build());
    }

    private void setDescription(
      final FunctionType<AccountDescription, AccountDescription> mutator)
      throws AccountsDatabaseIOException {
      try {
        final AccountDescription new_description;
        synchronized (this.description_lock) {
          new_description = mutator.call(this.description);

          final File account_lock =
            new File(this.directory, "lock");
          final File account_file =
            new File(this.directory, "account.json");
          final File account_file_tmp =
            new File(this.directory, "account.json.tmp");

          writeDescription(account_lock, account_file, account_file_tmp, new_description);
          this.description = new_description;
        }

        this.account_events.send(new AccountEventUpdated(this.id));
      } catch (final IOException e) {
        throw new AccountsDatabaseIOException("Could not write account data", e);
      }
    }

    public void delete() throws AccountsDatabaseIOException {
      try {
        LOG.debug("account [{}]: delete: {}", this.id.id(), this.directory);
        this.book_database.delete();
        DirectoryUtilities.directoryDelete(this.directory);
      } catch (final IOException e) {
        throw new AccountsDatabaseIOException(e.getMessage(), e);
      } catch (BookDatabaseException e) {
        throw new AccountsDatabaseIOException(e.getMessage(), new IOException(e));
      }
    }
  }
}
