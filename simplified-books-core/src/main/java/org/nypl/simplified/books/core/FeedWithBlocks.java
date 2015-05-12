package org.nypl.simplified.books.core;

import java.net.URI;
import java.util.AbstractList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.nypl.simplified.opds.core.OPDSAcquisitionFeed;
import org.nypl.simplified.opds.core.OPDSSearchLink;

import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

public final class FeedWithBlocks extends AbstractList<FeedBlock> implements
  FeedType
{
  public static FeedWithBlocks fromAcquisitionFeed(
    final OPDSAcquisitionFeed f)
  {
    NullCheck.notNull(f);

    final Map<String, FeedBlock> blocks =
      FeedBlock.fromOPDSBlocks(f.getFeedBlocks());
    final List<String> order = f.getFeedBlocksOrder();

    return new FeedWithBlocks(
      f.getFeedURI(),
      f.getFeedID(),
      f.getFeedUpdated(),
      f.getFeedTitle(),
      f.getFeedSearchURI(),
      order,
      blocks);
  }

  private final Map<String, FeedBlock>     blocks;
  private final List<String>               blocks_order;
  private final String                     id;
  private final OptionType<OPDSSearchLink> search;
  private final String                     title;
  private final Calendar                   updated;
  private final URI                        uri;

  private FeedWithBlocks(
    final URI in_uri,
    final String in_id,
    final Calendar in_updated,
    final String in_title,
    final OptionType<OPDSSearchLink> in_search,
    final List<String> in_blocks_order,
    final Map<String, FeedBlock> in_blocks)
  {
    this.uri = NullCheck.notNull(in_uri);
    this.id = NullCheck.notNull(in_id);
    this.updated = NullCheck.notNull(in_updated);
    this.title = NullCheck.notNull(in_title);
    this.search = NullCheck.notNull(in_search);
    this.blocks_order = NullCheck.notNull(in_blocks_order);
    this.blocks = NullCheck.notNull(in_blocks);
  }

  @Override public void add(
    final int index,
    final @Nullable FeedBlock element)
  {
    final FeedBlock nn_element = NullCheck.notNull(element);
    final String name = nn_element.getBlockTitle();
    this.blocks_order.add(index, name);
    this.blocks.put(name, nn_element);
  }

  @Override public FeedBlock get(
    final int index)
  {
    final String name = NullCheck.notNull(this.blocks_order.get(index));
    return NullCheck.notNull(this.blocks.get(name));
  }

  public Map<String, FeedBlock> getFeedBlocks()
  {
    return this.blocks;
  }

  @Override public String getFeedID()
  {
    return this.id;
  }

  @Override public OptionType<OPDSSearchLink> getFeedSearchURI()
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
    return m.onFeedWithBlocks(this);
  }

  @Override public FeedBlock remove(
    final int index)
  {
    final String name = NullCheck.notNull(this.blocks_order.get(index));
    final FeedBlock r = NullCheck.notNull(this.blocks.remove(name));
    this.blocks_order.remove(index);
    return r;
  }

  @Override public FeedBlock set(
    final int index,
    final @Nullable FeedBlock element)
  {
    final FeedBlock nn_element = NullCheck.notNull(element);
    final String name = NullCheck.notNull(this.blocks_order.get(index));
    final FeedBlock old = NullCheck.notNull(this.blocks.get(name));
    this.blocks.put(name, nn_element);
    return old;
  }

  @Override public int size()
  {
    return this.blocks_order.size();
  }
}
