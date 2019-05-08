package org.nypl.simplified.accounts.database;

import android.content.Context;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.io7m.jfunctional.FunctionType;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Unit;

import net.jcip.annotations.GuardedBy;

import org.jetbrains.annotations.Nullable;
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials;
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentialsStoreType;
import org.nypl.simplified.accounts.api.AccountDescription;
import org.nypl.simplified.accounts.api.AccountEvent;
import org.nypl.simplified.accounts.api.AccountEventLoginStateChanged;
import org.nypl.simplified.accounts.api.AccountEventUpdated;
import org.nypl.simplified.accounts.api.AccountID;
import org.nypl.simplified.accounts.api.AccountLoginState;
import org.nypl.simplified.accounts.api.AccountPreferences;
import org.nypl.simplified.accounts.api.AccountProvider;
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription;
import org.nypl.simplified.accounts.api.AccountProviderCollectionType;
import org.nypl.simplified.accounts.database.api.AccountType;
import org.nypl.simplified.accounts.database.api.AccountsDatabaseBooksException;
import org.nypl.simplified.accounts.database.api.AccountsDatabaseException;
import org.nypl.simplified.accounts.database.api.AccountsDatabaseIOException;
import org.nypl.simplified.accounts.database.api.AccountsDatabaseLastAccountException;
import org.nypl.simplified.accounts.database.api.AccountsDatabaseNonexistentException;
import org.nypl.simplified.accounts.database.api.AccountsDatabaseOpenException;
import org.nypl.simplified.accounts.database.api.AccountsDatabaseType;
import org.nypl.simplified.books.book_database.api.BookDatabaseException;
import org.nypl.simplified.books.book_database.api.BookDatabaseFactoryType;
import org.nypl.simplified.books.book_database.api.BookDatabaseType;
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
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;

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
  private final AccountAuthenticationCredentialsStoreType credentials_store;

  private AccountsDatabase(
    final Context context,
    final File directory,
    final ObservableType<AccountEvent> account_events,
    final SortedMap<AccountID, Account> accounts,
    final SortedMap<URI, Account> accounts_by_provider,
    final AccountAuthenticationCredentialsStoreType credentials,
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
    this.credentials_store =
      Objects.requireNonNull(credentials, "credentials");

    this.accounts_read =
      castMap(Collections.unmodifiableSortedMap(accounts));
    this.accounts_by_provider_read =
      castMap(Collections.unmodifiableSortedMap(accounts_by_provider));
    this.accounts_lock = new Object();
  }

  private static AccountID freshAccountID(
    final SortedMap<AccountID, Account> accounts) {

    for (int index = 0; index < 100; ++index) {
      AccountID account_id = AccountID.Companion.generate();
      if (accounts.containsKey(account_id)) {
        continue;
      }
      return account_id;
    }

    throw new IllegalStateException(
      "Could not generate a fresh account ID after multiple attempts");
  }

  /**
   * Open an accounts database from the given directory, creating a new database if one does not exist.
   *
   * @throws AccountsDatabaseException If any errors occurred whilst trying to open the database
   */

  public static AccountsDatabaseType open(
    final Context context,
    final ObservableType<AccountEvent> account_events,
    final BookDatabaseFactoryType book_databases,
    final AccountProviderCollectionType account_providers,
    final AccountAuthenticationCredentialsStoreType account_credentials,
    final File directory)
    throws AccountsDatabaseException {

    Objects.requireNonNull(context, "Context");
    Objects.requireNonNull(account_events, "Account events");
    Objects.requireNonNull(book_databases, "Book databases");
    Objects.requireNonNull(directory, "Directory");
    Objects.requireNonNull(account_providers, "Account providers");
    Objects.requireNonNull(account_credentials, "Account credentials");

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
      account_credentials,
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
      account_credentials,
      book_databases);
  }

  private static void openAllAccounts(
    final Context context,
    final ObservableType<AccountEvent> account_events,
    final BookDatabaseFactoryType book_databases,
    final AccountProviderCollectionType account_providers,
    final AccountAuthenticationCredentialsStoreType account_credentials,
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
            account_credentials,
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
                .append(existing_account.id.getUuid())
                .append("\n")
                .append("  Opening Account: ")
                .append(account.id.getUuid())
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

  private static AccountID openOneAccountDirectory(
    final List<Exception> errors,
    final File directory,
    final String account_id_name) {

    /*
     * If the account directory is not a directory, then give up.
     */

    final File account_dir_old = new File(directory, account_id_name);
    if (!account_dir_old.isDirectory()) {
      errors.add(new IOException("Not a directory: " + account_dir_old));
      return null;
    }

    /*
     * Try to parse the existing directory name as a UUID. If it cannot be parsed
     * as a UUID, attempt to rename it to a new UUID value. The reason for this is because
     * account IDs used to be plain integers, and we have existing deployed clients that are
     * still using those IDs.
     */

    UUID accountUUid;
    try {
      accountUUid = UUID.fromString(account_id_name);
      return new AccountID(accountUUid);
    } catch (Exception e) {
      LOG.warn("could not parse {} as a UUID", account_id_name);
      return openOneAccountDirectoryDoMigration(directory, account_dir_old);
    }
  }

  /**
   * Migrate a non-UUID account directory to a new UUID one.
   */

  @Nullable
  private static AccountID openOneAccountDirectoryDoMigration(
    final File owner_directory,
    final File existing_directory) {

    LOG.debug("attempting to migrate {} directory", existing_directory);

    for (int index = 0; index < 100; ++index) {
      final UUID accountUUid = UUID.randomUUID();
      final File account_dir_new = new File(owner_directory, accountUUid.toString());
      if (account_dir_new.exists()) {
        continue;
      }

      try {
        FileUtilities.fileRename(existing_directory, account_dir_new);
        LOG.debug("migrated {} to {}", existing_directory, account_dir_new);
        return new AccountID(accountUUid);
      } catch (IOException ex) {
        LOG.error("could not migrate directory {} -> {}", existing_directory, account_dir_new);
        return null;
      }
    }

    LOG.error("could not migrate directory {} after multiple attempts, aborting!", existing_directory);
    return null;
  }

  private static Account openOneAccount(
    final Context context,
    final ObservableType<AccountEvent> account_events,
    final BookDatabaseFactoryType book_databases,
    final AccountProviderCollectionType account_providers,
    final AccountAuthenticationCredentialsStoreType credentials_store,
    final File directory,
    final ObjectMapper jom,
    final List<Exception> errors,
    final String account_id_name) {

    final AccountID account_id = openOneAccountDirectory(errors, directory, account_id_name);
    if (account_id == null) {
      return null;
    }

    final File account_dir =
      new File(directory, account_id.toString());
    final File account_file =
      new File(account_dir, "account.json");
    final File books_dir =
      new File(account_dir, "books");

    try {
      final BookDatabaseType book_database =
        book_databases.openDatabase(context, account_id, books_dir);
      final AccountDescription desc =
        AccountDescriptionJSON.deserializeFromFile(jom, account_file);
      final AccountProvider provider =
        account_providers.provider(desc.provider());

      final OptionType<AccountProviderAuthenticationDescription> authentication =
        provider.authentication();
      final AccountAuthenticationCredentials credentials =
        credentials_store.get(account_id);

      final AccountLoginState login_state;
      if (authentication.isSome() && credentials != null) {
        login_state = new AccountLoginState.AccountLoggedIn(credentials);
      } else {
        login_state = AccountLoginState.AccountNotLoggedIn.INSTANCE;
      }

      return new Account(
        account_id,
        account_dir,
        account_events,
        desc,
        provider,
        login_state,
        credentials_store,
        book_database);
    } catch (final IOException e) {
      errors.add(new IOException("Could not parse account: " + account_file, e));
      return null;
    } catch (final IllegalArgumentException e) {
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


    final AccountID account_id;
    synchronized (this.accounts_lock) {
      account_id = freshAccountID(this.accounts);

      LOG.debug("creating account {} (provider {})", account_id, account_provider.id());

      Preconditions.checkArgument(
        !this.accounts.containsKey(account_id),
        "Account ID %s cannot have been used", account_id);
    }

    try {
      final File account_dir =
        new File(this.directory, account_id.toString());
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
        this.book_databases.openDatabase(this.context, account_id, books_dir);

      final AccountPreferences preferences =
        new AccountPreferences(false);

      final AccountDescription desc =
        AccountDescription.builder(account_provider.id(), preferences)
          .build();

      writeDescription(account_lock, account_file, account_file_tmp, desc);

      final Account account;
      synchronized (this.accounts_lock) {
        account = new Account(
          account_id,
          account_dir,
          this.account_events,
          desc,
          account_provider,
          AccountLoginState.AccountNotLoggedIn.INSTANCE,
          this.credentials_store,
          book_database);
        this.accounts.put(account_id, account);
        this.accounts_by_provider.put(account_provider.id(), account);
      }
      return account;
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
        account.delete();
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
    private final AccountAuthenticationCredentialsStoreType credentials;
    private @GuardedBy("description_lock")
    AccountDescription description;
    private @GuardedBy("description_lock")
    AccountLoginState login_state;
    private final BookDatabaseType book_database;
    private final AccountProvider provider;

    Account(
      final AccountID id,
      final File directory,
      final ObservableType<AccountEvent> account_events,
      final AccountDescription description,
      final AccountProvider provider,
      final AccountLoginState login_state,
      final AccountAuthenticationCredentialsStoreType credentials,
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
      this.login_state =
        Objects.requireNonNull(login_state, "login_state");
      this.credentials =
        Objects.requireNonNull(credentials, "credentials");

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
    public AccountLoginState loginState() {
      synchronized (this.description_lock) {
        return this.login_state;
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
    public void setLoginState(AccountLoginState state) {
      Objects.requireNonNull(state, "state");

      synchronized (this.description_lock) {
        this.login_state = state;
      }

      this.account_events.send(new AccountEventLoginStateChanged(this.id, state));

      final AccountAuthenticationCredentials credentials = state.getCredentials();
      if (credentials != null) {
        this.credentials.put(this.id, credentials);
      } else {
        this.credentials.delete(this.id);
      }
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
        LOG.debug("account [{}]: delete: {}", this.id, this.directory);

        Exception exception = null;
        try {
          LOG.debug("account [{}]: delete book database", this.id);
          this.book_database.delete();
        } catch (Exception e) {
          exception = accumulateOrSuppress(exception, e);
        }

        try {
          LOG.debug("account [{}]: delete credentials", this.id);
          this.credentials.delete(this.id);
        } catch (Exception e) {
          exception = accumulateOrSuppress(exception, e);
        }

        try {
          LOG.debug("account [{}]: delete directory", this.id);
          DirectoryUtilities.directoryDelete(this.directory);
        } catch (Exception e) {
          exception = accumulateOrSuppress(exception, e);
        }

        if (exception != null) {
          throw new AccountsDatabaseIOException(exception.getMessage(), new IOException(exception));
        }
      } finally {
        LOG.debug("account [{}]: deleted", this.id);
      }
    }

    private static Exception accumulateOrSuppress(
      final Exception exception,
      final Exception next) {
      final Exception result;
      if (exception != null) {
        result = exception;
        exception.addSuppressed(next);
      } else {
        result = next;
      }
      return result;
    }
  }
}
