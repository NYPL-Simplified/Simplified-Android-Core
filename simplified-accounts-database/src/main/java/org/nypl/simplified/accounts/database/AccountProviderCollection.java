package org.nypl.simplified.accounts.database;

import com.google.auto.value.AutoValue;
import com.io7m.jnull.NullCheck;

import org.nypl.simplified.accounts.api.AccountProvider;
import org.nypl.simplified.accounts.api.AccountProviderCollectionType;

import java.net.URI;
import java.util.Collections;
import java.util.SortedMap;

/**
 * <p>An immutable account provider collection.</p>
 */

@AutoValue
public abstract class AccountProviderCollection implements AccountProviderCollectionType {

  AccountProviderCollection() {

  }

  /**
   * Construct a provider collection.
   *
   * @param in_default_provider The default provider
   * @param in_providers        The available providers
   * @throws IllegalArgumentException If the default provider is not present in the available providers
   */

  public static AccountProviderCollection create(
    final AccountProvider in_default_provider,
    final SortedMap<URI, AccountProvider> in_providers)
    throws IllegalArgumentException {

    return new AutoValue_AccountProviderCollection(
      in_default_provider, Collections.unmodifiableSortedMap(in_providers));
  }

  @Override
  public final AccountProvider provider(final URI provider_id)
    throws IllegalArgumentException {

    final AccountProvider provider =
      this.providers().get(NullCheck.notNull(provider_id, "Provider"));
    if (provider == null) {
      throw new IllegalArgumentException(
        "Nonexistent provider: " + provider_id);
    }
    return provider;
  }
}
