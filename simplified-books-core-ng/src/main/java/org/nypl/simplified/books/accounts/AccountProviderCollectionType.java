package org.nypl.simplified.books.accounts;

import java.net.URI;
import java.util.SortedMap;

/**
 * The type of account provider collections.
 */

public interface AccountProviderCollectionType {

  /**
   * @return The default account provider
   */

  AccountProvider providerDefault();

  /**
   * @return The account providers
   */

  SortedMap<URI, AccountProvider> providers();

  /**
   * @param provider_id The provider ID
   * @return The account provider for the given ID
   * @throws AccountsDatabaseNonexistentProviderException If no provider exists with the given ID
   */

  AccountProvider provider(URI provider_id)
      throws AccountsDatabaseNonexistentProviderException;
}
