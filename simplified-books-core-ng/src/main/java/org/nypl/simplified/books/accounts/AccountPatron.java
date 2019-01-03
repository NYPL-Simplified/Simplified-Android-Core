package org.nypl.simplified.books.accounts;

import com.google.auto.value.AutoValue;

/**
 * Information about an account patron.
 */

@AutoValue
public abstract class AccountPatron {

  AccountPatron() {

  }

  /**
   * Construct a patron.
   *
   * @param in_value The raw patron value
   */

  public static AccountPatron create(
      final String in_value) {
    return new AutoValue_AccountPatron(in_value);
  }

  /**
   * @return The raw patron value
   */

  public abstract String value();

  @Override
  public String toString() {
    return this.value();
  }

}
