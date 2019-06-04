package org.nypl.simplified.accounts.api;

import java.net.URI;
import java.util.SortedMap;

/**
 * The type of account provider collections.
 */

public interface AccountProviderCollectionType {

  /**
   * @return The default account provider
   */

  AccountProviderType providerDefault();

  /**
   * @return The account providers
   */

  SortedMap<URI, AccountProviderType> providers();

  /**
   * @param provider_id The provider ID
   * @return The account provider for the given ID
   * @throws IllegalArgumentException If no provider exists with the given ID
   */

  AccountProviderType provider(URI provider_id)
    throws IllegalArgumentException;
}
