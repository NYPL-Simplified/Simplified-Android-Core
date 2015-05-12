package org.nypl.simplified.app;

import org.nypl.simplified.books.core.FeedEntryOPDS;

import android.widget.ImageView;

import com.squareup.picasso.Callback;

/**
 * The type of cover providers.
 */

public interface BookCoverProviderType
{
  void loadCoverInto(
    final FeedEntryOPDS e,
    final ImageView i,
    final int w,
    final int h);

  void loadCoverIntoWithCallback(
    final FeedEntryOPDS e,
    final ImageView i,
    final int w,
    final int h,
    final Callback c);

  void loadThumbnailInto(
    final FeedEntryOPDS e,
    final ImageView i,
    final int w,
    final int h);

  void loadThumbnailIntoWithCallback(
    final FeedEntryOPDS e,
    final ImageView i,
    final int w,
    final int h,
    final Callback c);
}
