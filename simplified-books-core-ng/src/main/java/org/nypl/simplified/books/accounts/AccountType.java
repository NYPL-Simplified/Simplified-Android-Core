package org.nypl.simplified.books.accounts;

import com.io7m.jfunctional.OptionType;

import org.nypl.simplified.books.book_database.BookDatabaseType;

/**
 * <p>The interface exposed by accounts.</p>
 *
 * <p>An account aggregates a set of credentials and a book database.
 * Account are assigned monotonically increasing identifiers by the
 * application, but the identifiers themselves carry no meaning. It is
 * an error to depend on the values of identifiers for any kind of
 * program logic.</p>
 */

public interface AccountType extends AccountReadableType {

  /**
   * @return The book database owned by this account
   */

  BookDatabaseType bookDatabase();

  /**
   * Update the account credentials.
   *
   * @param credentials The new credentials
   * @throws AccountsDatabaseException On database errors
   */

  void setCredentials(OptionType<AccountAuthenticationCredentials> credentials)
    throws AccountsDatabaseException;

  /**
   * Update the account preferences.
   *
   * @param preferences The new preferences
   * @throws AccountsDatabaseException On database errors
   */

  void setPreferences(AccountPreferences preferences)
    throws AccountsDatabaseException;
}
