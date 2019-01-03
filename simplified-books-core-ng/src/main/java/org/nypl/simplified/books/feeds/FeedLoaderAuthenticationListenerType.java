package org.nypl.simplified.books.feeds;

import com.io7m.jfunctional.OptionType;

import org.nypl.simplified.books.accounts.AccountAuthenticationCredentials;

/**
 * The type of listeners that will receive authentication data when loading
 * feeds.
 */

public interface FeedLoaderAuthenticationListenerType
{
  /**
   * Authentication data was provided.
   *
   * @param credentials The account credentials
   */

  void onAuthenticationProvided(
      AccountAuthenticationCredentials credentials);

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
