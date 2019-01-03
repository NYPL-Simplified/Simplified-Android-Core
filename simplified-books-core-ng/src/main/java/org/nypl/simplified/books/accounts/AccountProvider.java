package org.nypl.simplified.books.accounts;

import com.google.auto.value.AutoValue;
import com.io7m.jfunctional.None;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.OptionVisitorType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;

import java.net.URI;

/**
 * <p>The interface exposed by account providers.</p>
 * <p>An account provider supplies branding information and defines various
 * aspects of the account such as whether or not syncing of bookmarks to
 * a remote server is supported, etc. Account providers may be registered
 * statically within the application as resources, or may be fetched from
 * a remote server. Account providers are identified by opaque account
 * provider URIs. Each account stores the identifier of the
 * account provider with which it is associated. It is an error to depend on
 * the values of identifiers for any kind of program logic.</p>
 */

@AutoValue
public abstract class AccountProvider implements Comparable<AccountProvider> {

  AccountProvider() {

  }

  @Override
  public final int compareTo(final AccountProvider other) {
    return id().compareTo(NullCheck.notNull(other, "Other").id());
  }

  /**
   * @return The account provider URI
   */

  public abstract URI id();

  /**
   * @return The display name
   */

  public abstract String displayName();

  /**
   * @return The subtitle
   */

  public abstract String subtitle();

  /**
   * @return The logo URI
   */

  public abstract URI logo();

  /**
   * @return An authentication description if authentication is required, or nothing if it isn't
   */

  public abstract OptionType<AccountProviderAuthenticationDescription> authentication();

  /**
   * @return {@code true} iff the SimplyE synchronization is supported
   */

  public abstract boolean supportsSimplyESynchronization();

  /**
   * @return {@code true} iff the barcode scanner is supported
   */

  public abstract boolean supportsBarcodeScanner();

  /**
   * @return {@code true} iff the barcode display is supported
   */

  public abstract boolean supportsBarcodeDisplay();

  /**
   * @return {@code true} iff reservations are supported
   */

  public abstract boolean supportsReservations();

  /**
   * XXX: There is an associated Card Creator URL; this should be an OptionType[URI]
   * @return {@code true} iff the card creator is supported
   */

  public abstract boolean supportsCardCreator();

  /**
   * @return {@code true} iff the help center is supported
   */

  public abstract boolean supportsHelpCenter();

  /**
   * @return The base URI of the catalog
   */

  public abstract URI catalogURI();

  /**
   * The Over-13s catalog URI.
   *
   * @return The URI of the catalog for readers over the age of 13
   */

  public abstract OptionType<URI> catalogURIForOver13s();

  /**
   * @return The URI of the catalog for readers under the age of 13
   */

  public abstract OptionType<URI> catalogURIForUnder13s();

  /**
   * @return The support email address
   */

  public abstract OptionType<String> supportEmail();

  /**
   * @return The URI of the EULA if one is required
   */

  public abstract OptionType<URI> eula();

  /**
   * @return The URI of the EULA if one is required
   */

  public abstract OptionType<URI> license();

  /**
   * @return The URI of the privacy policy if one is required
   */

  public abstract OptionType<URI> privacyPolicy();

  /**
   * @return The main color used to decorate the application when using this provider
   */

  public abstract String mainColor();

  /**
   * @return The name of the Android theme to use instead of the standard theme
   */

  public abstract OptionType<String> styleNameOverride();

  /**
   * Determine the correct catalog URI to use for readers of a given age.
   * @param age The age of the reader
   * @return The correct catalog URI for the given age
   */

  public final URI catalogURIForAge(
      final int age)
  {
    if (age >= 13) {
      return this.catalogURIForOver13s().accept(new OptionVisitorType<URI, URI>() {
        @Override
        public URI none(final None<URI> catalog_none) {
          return catalogURI();
        }

        @Override
        public URI some(final Some<URI> catalog_some) {
          return catalog_some.get();
        }
      });
    }

    return this.catalogURIForUnder13s().accept(new OptionVisitorType<URI, URI>() {
      @Override
      public URI none(final None<URI> catalog_none) {
        return catalogURI();
      }

      @Override
      public URI some(final Some<URI> catalog_some) {
        return catalog_some.get();
      }
    });
  }

  /**
   * The type of mutable builders for account providers.
   */

  @AutoValue.Builder
  public abstract static class Builder {

    Builder() {

    }

    /**
     * @see #id()
     * @param id The provider ID
     * @return The current builder
     */

    public abstract Builder setId(URI id);

    /**
     * @see #displayName()
     * @param name The display name
     * @return The current builder
     */

    public abstract Builder setDisplayName(String name);

    /**
     * @see #subtitle()
     * @param subtitle The subtitle
     * @return The current builder
     */

    public abstract Builder setSubtitle(String subtitle);

    /**
     * @see #logo()
     * @param logo The logo URI
     * @return The current builder
     */

    public abstract Builder setLogo(URI logo);

    /**
     * @see #authentication()
     * @param description The required authentication, if any
     * @return The current builder
     */

    public abstract Builder setAuthentication(
        OptionType<AccountProviderAuthenticationDescription> description);

    /**
     * @see #supportsSimplyESynchronization()
     * @param supports {@code true} iff support is present
     * @return The current builder
     */

    public abstract Builder setSupportsSimplyESynchronization(boolean supports);

    /**
     * @see #supportsBarcodeScanner()
     * @param supports {@code true} iff support is present
     * @return The current builder
     */

    public abstract Builder setSupportsBarcodeScanner(boolean supports);

    /**
     * @see #supportsBarcodeDisplay()
     * @param supports {@code true} iff support is present
     * @return The current builder
     */

    public abstract Builder setSupportsBarcodeDisplay(boolean supports);

    /**
     * @see #supportsReservations()
     * @param supports {@code true} iff support is present
     * @return The current builder
     */

    public abstract Builder setSupportsReservations(boolean supports);

    /**
     * @see #supportsCardCreator()
     * @param supports {@code true} iff support is present
     * @return The current builder
     */

    public abstract Builder setSupportsCardCreator(boolean supports);

    /**
     * @see #supportsHelpCenter()
     * @param supports {@code true} iff support is present
     * @return The current builder
     */

    public abstract Builder setSupportsHelpCenter(boolean supports);

    /**
     * @see #catalogURI()
     * @param uri The default catalog URI
     * @return The current builder
     */

    public abstract Builder setCatalogURI(URI uri);

    /**
     * @see #catalogURIForOver13s()
     * @param uri The catalog URI for over 13s
     * @return The current builder
     */

    public abstract Builder setCatalogURIForOver13s(
        OptionType<URI> uri);

    /**
     * @see #catalogURIForUnder13s()
     * @param uri The catalog URI for over 13s
     * @return The current builder
     */

    public abstract Builder setCatalogURIForUnder13s(
        OptionType<URI> uri);

    /**
     * @see #supportEmail()
     * @param email The support email
     * @return The current builder
     */

    public Builder setSupportEmail(String email)
    {
      return setSupportEmail(Option.some(email));
    }

    /**
     * @see #supportEmail()
     * @param email The support email
     * @return The current builder
     */

    public abstract Builder setSupportEmail(OptionType<String> email);

    /**
     * @see #eula()
     * @param uri The URI
     * @return The current builder
     */

    public abstract Builder setEula(
        OptionType<URI> uri);

    /**
     * @see #license()
     * @param uri The URI
     * @return The current builder
     */

    public abstract Builder setLicense(
        OptionType<URI> uri);

    /**
     * @see #privacyPolicy()
     * @param uri The URI
     * @return The current builder
     */

    public abstract Builder setPrivacyPolicy(
        OptionType<URI> uri);

    /**
     * @see #mainColor()
     * @param color The color
     * @return The current builder
     */

    public abstract Builder setMainColor(
        String color);

    /**
     * @see #styleNameOverride()
     * @param style The style name override
     * @return The current builder
     */

    public abstract Builder setStyleNameOverride(
        OptionType<String> style);

    /**
     * @return The constructed account provider
     */

    public abstract AccountProvider build();
  }

  /**
   * @return The current value as a mutable builder
   */

  public abstract Builder toBuilder();

  /**
   * @return A new account provider builder
   */

  public static Builder builder() {
    final Builder b = new AutoValue_AccountProvider.Builder();
    b.setAuthentication(Option.<AccountProviderAuthenticationDescription>none());
    b.setSupportsSimplyESynchronization(false);
    b.setSupportsBarcodeDisplay(false);
    b.setSupportsBarcodeScanner(false);
    b.setSupportsReservations(false);
    b.setSupportsCardCreator(false);
    b.setSupportsHelpCenter(false);
    b.setSupportEmail(Option.<String>none());
    b.setCatalogURIForOver13s(Option.<URI>none());
    b.setCatalogURIForUnder13s(Option.<URI>none());
    b.setEula(Option.<URI>none());
    b.setLicense(Option.<URI>none());
    b.setPrivacyPolicy(Option.<URI>none());
    b.setMainColor("#da2527");
    b.setStyleNameOverride(Option.<String>none());
    return b;
  }
}
