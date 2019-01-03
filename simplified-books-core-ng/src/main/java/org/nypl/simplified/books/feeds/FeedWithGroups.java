package org.nypl.simplified.books.feeds;

import com.io7m.jfunctional.FunctionType;
import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeed;
import org.nypl.simplified.opds.core.OPDSOpenSearch1_1;

import java.net.URI;
import java.util.AbstractList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

/**
 * A (mutable) feed with groups.
 */

public final class FeedWithGroups extends AbstractList<FeedGroup>
  implements FeedType
{
  private final Map<String, FeedGroup>     blocks;
  private final List<String>               blocks_order;
  private final String                     id;
  private final OptionType<FeedSearchType> search;
  private final String                     title;
  private final Calendar                   updated;
  private final URI                        uri;
  private final OptionType<URI>            terms_of_service;
  private final OptionType<URI>            privacy_policy;
  private final OptionType<URI>            about;
  private final OptionType<URI>            licenses;

  private FeedWithGroups(
    final URI in_uri,
    final String in_id,
    final Calendar in_updated,
    final String in_title,
    final OptionType<FeedSearchType> in_search,
    final List<String> in_blocks_order,
    final Map<String, FeedGroup> in_blocks,
    final OptionType<URI> in_terms_of_service,
    final OptionType<URI> in_about,
    final OptionType<URI> in_privacy_policy,
    final OptionType<URI> in_licenses)
  {
    this.uri = NullCheck.notNull(in_uri);
    this.id = NullCheck.notNull(in_id);
    this.updated = NullCheck.notNull(in_updated);
    this.title = NullCheck.notNull(in_title);
    this.search = NullCheck.notNull(in_search);
    this.blocks_order = NullCheck.notNull(in_blocks_order);
    this.blocks = NullCheck.notNull(in_blocks);
    this.terms_of_service = NullCheck.notNull(in_terms_of_service);
    this.about = NullCheck.notNull(in_about);
    this.privacy_policy = NullCheck.notNull(in_privacy_policy);
    this.licenses = NullCheck.notNull(in_licenses);
  }

  /**
   * Construct a feed from the given acquisition feed.
   *
   * @param f      The feed
   * @param search An optional link to a search document
   *
   * @return A new feed
   */

  public static FeedWithGroups fromAcquisitionFeed(
    final OPDSAcquisitionFeed f,
    final OptionType<OPDSOpenSearch1_1> search)
  {
    NullCheck.notNull(f);

    final Map<String, FeedGroup> blocks =
      FeedGroup.fromOPDSGroups(f.getFeedGroups());
    final List<String> order = f.getFeedGroupsOrder();

    final OptionType<FeedSearchType> actual_search = search.map(
      new FunctionType<OPDSOpenSearch1_1, FeedSearchType>()
      {
        @Override public FeedSearchType call(
          final OPDSOpenSearch1_1 s)
        {
          return new FeedSearchOpen1_1(s);
        }
      });

    return new FeedWithGroups(
      f.getFeedURI(),
      f.getFeedID(),
      f.getFeedUpdated(),
      f.getFeedTitle(),
      actual_search,
      order,
      blocks,
      f.getFeedTermsOfService(),
      f.getFeedAbout(),
      f.getFeedPrivacyPolicy(),
      f.getFeedLicenses()
      );
  }

  /**
   * @return A link to the about, if any
   */

  public OptionType<URI> getFeedAbout()
  {
    return this.about;
  }

  /**
   * @return A link to the licenses, if any
   */

  public OptionType<URI> getFeedLicenses()
  {
    return this.licenses;
  }

  /**
   * @return A link to the terms of service, if any
   */

  public OptionType<URI> getFeedTermsOfService()
  {
    return this.terms_of_service;
  }

  /**
   * @return A link to the privacy policy, if any
   */

  public OptionType<URI> getFeedPrivacyPolicy()
  {
    return this.privacy_policy;
  }

  @Override public void add(
    final int index,
    final @Nullable FeedGroup element)
  {
    final FeedGroup nn_element = NullCheck.notNull(element);
    final String name = nn_element.getGroupTitle();
    this.blocks_order.add(index, name);
    this.blocks.put(name, nn_element);
  }

  @Override public FeedGroup get(
    final int index)
  {
    final String name = NullCheck.notNull(this.blocks_order.get(index));
    return NullCheck.notNull(this.blocks.get(name));
  }

  /**
   * @return The feed groups
   */

  public Map<String, FeedGroup> getFeedGroups()
  {
    return this.blocks;
  }

  @Override public String getFeedID()
  {
    return this.id;
  }

  @Override public OptionType<FeedSearchType> getFeedSearch()
  {
    return this.search;
  }

  @Override public String getFeedTitle()
  {
    return this.title;
  }

  @Override public Calendar getFeedUpdated()
  {
    return this.updated;
  }

  @Override public URI getFeedURI()
  {
    return this.uri;
  }

  @Override public <A, E extends Exception> A matchFeed(
    final FeedMatcherType<A, E> m)
    throws E
  {
    return m.onFeedWithGroups(this);
  }

  @Override public FeedGroup remove(
    final int index)
  {
    final String name = NullCheck.notNull(this.blocks_order.get(index));
    final FeedGroup r = NullCheck.notNull(this.blocks.remove(name));
    this.blocks_order.remove(index);
    return r;
  }

  @Override public FeedGroup set(
    final int index,
    final @Nullable FeedGroup element)
  {
    final FeedGroup nn_element = NullCheck.notNull(element);
    final String name = NullCheck.notNull(this.blocks_order.get(index));
    final FeedGroup old = NullCheck.notNull(this.blocks.get(name));
    this.blocks.put(name, nn_element);
    return old;
  }

  @Override public int size()
  {
    return this.blocks_order.size();
  }
}
