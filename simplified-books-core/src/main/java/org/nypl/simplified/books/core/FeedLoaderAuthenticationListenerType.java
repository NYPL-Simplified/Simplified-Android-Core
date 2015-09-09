package org.nypl.simplified.books.core;

import com.io7m.jfunctional.OptionType;

/**
 * The type of listeners that will receive authentication data when loading
 * feeds.
 */

public interface FeedLoaderAuthenticationListenerType
{
  /**
   * Authentication data was provided.
   *
   * @param user     The user
   * @param password The password
   */

  void onAuthenticationProvided(
    AccountBarcode user,
    AccountPIN password);

  /**
   * No authentication data was provided.
   */

  void onAuthenticationNotProvided();

  /**
   * An error occurred whilst attempting to retrieve authentication data.
   *
   * @param error   The exception raised, if any
   * @param message The error message
   */

  void onAuthenticationError(
    OptionType<Throwable> error,
    String message);
}
