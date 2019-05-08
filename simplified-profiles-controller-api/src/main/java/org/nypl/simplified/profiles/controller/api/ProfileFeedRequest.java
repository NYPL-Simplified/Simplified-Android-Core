package org.nypl.simplified.profiles.controller.api;

import com.google.auto.value.AutoValue;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;

import org.joda.time.DateTime;
import org.nypl.simplified.feeds.api.FeedBooksSelection;
import org.nypl.simplified.feeds.api.FeedFacetPseudo;
import org.nypl.simplified.feeds.api.FeedFacetPseudoTitleProviderType;

import java.net.URI;

/**
 * The type of feed requests.
 */

@AutoValue
public abstract class ProfileFeedRequest {

  public abstract URI uri();

  public abstract String id();

  public abstract DateTime updated();

  public abstract String title();

  public abstract FeedFacetPseudo.FacetType facetActive();

  public abstract String facetGroup();

  public abstract FeedFacetPseudoTitleProviderType facetTitleProvider();

  public abstract OptionType<String> search();

  public abstract FeedBooksSelection feedSelection();

  @AutoValue.Builder
  public static abstract class Builder {

    public abstract Builder setUri(URI x);

    public abstract Builder setId(String x);

    public abstract Builder setUpdated(DateTime x);

    public abstract Builder setTitle(String x);

    public abstract Builder setFacetActive(FeedFacetPseudo.FacetType x);

    public abstract Builder setFacetGroup(String x);

    public abstract Builder setFacetTitleProvider(FeedFacetPseudoTitleProviderType x);

    public abstract Builder setSearch(OptionType<String> x);

    public abstract Builder setFeedSelection(FeedBooksSelection x);

    public abstract ProfileFeedRequest build();

  }

  public static Builder builder(
      final URI uri,
      final String title,
      final String facet_group,
      final FeedFacetPseudoTitleProviderType title_provider)
  {
    final Builder b = new AutoValue_ProfileFeedRequest.Builder();
    b.setUri(uri);
    b.setId(uri.toString());
    b.setUpdated(DateTime.now());
    b.setTitle(title);
    b.setFacetActive(FeedFacetPseudo.FacetType.SORT_BY_TITLE);
    b.setFacetGroup(facet_group);
    b.setFacetTitleProvider(title_provider);
    b.setSearch(Option.none());
    b.setFeedSelection(FeedBooksSelection.BOOKS_FEED_LOANED);
    return b;
  }
}
