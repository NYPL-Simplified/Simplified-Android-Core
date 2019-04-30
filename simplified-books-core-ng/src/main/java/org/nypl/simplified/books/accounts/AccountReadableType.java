package org.nypl.simplified.books.accounts;

import java.io.File;

/**
 * <p>The read-only interface exposed by accounts.</p>
 *
 * <p>An account aggregates a set of credentials and a book database.
 * Account are assigned monotonically increasing identifiers by the
 * application, but the identifiers themselves carry no meaning. It is
 * an error to depend on the values of identifiers for any kind of
 * program logic.</p>
 */

public interface AccountReadableType {

  /**
   * @return The account ID
   */

  AccountID id();

  /**
   * @return The full path to the on-disk directory storing data for this account
   */

  File directory();

  /**
   * @return The account provider associated with this account
   */

  AccountProvider provider();

  /**
   * @return The current state of the account with respect to logging in/out
   */

  AccountLoginState loginState();

  /**
   * @return The account preferences
   */

  AccountPreferences preferences();

  /**
   * @return {@code true} if the account requires credentials to perform operations
   */

  default boolean requiresCredentials() {
    return this.provider().authentication().isSome();
  }
}
