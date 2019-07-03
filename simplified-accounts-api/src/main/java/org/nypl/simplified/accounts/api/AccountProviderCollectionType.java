package org.nypl.simplified.accounts.api;

import java.net.URI;
import java.util.Objects;
import java.util.SortedMap;

/**
 * The type of account provider collections.
 *
 * @deprecated To be replaced with an API to fetch account providers via their description
 */

@Deprecated
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

  default AccountProviderType provider(URI provider_id)
    throws IllegalArgumentException {
    final AccountProviderType provider =
      this.providers().get(Objects.requireNonNull(provider_id, "provider_id"));
    if (provider == null) {
      throw new IllegalArgumentException("No such provider: " + provider_id);
    }
    return provider;
  }
}
