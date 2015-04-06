package org.nypl.simplified.app;

import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;

import android.widget.ImageView;

import com.squareup.picasso.Callback;

/**
 * The type of cover providers.
 */

public interface CoverProviderType
{
  void loadCoverInto(
    final OPDSAcquisitionFeedEntry e,
    final ImageView i,
    final int w,
    final int h);

  void loadCoverIntoWithCallback(
    final OPDSAcquisitionFeedEntry e,
    final ImageView i,
    final int w,
    final int h,
    final Callback c);

  void loadThumbnailInto(
    final OPDSAcquisitionFeedEntry e,
    final ImageView i,
    final int w,
    final int h);

  void loadThumbnailIntoWithCallback(
    final OPDSAcquisitionFeedEntry e,
    final ImageView i,
    final int w,
    final int h,
    final Callback c);
}
