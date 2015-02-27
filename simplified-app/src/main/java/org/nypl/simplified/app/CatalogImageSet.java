package org.nypl.simplified.app;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.common.util.concurrent.ListenableFuture;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

@SuppressWarnings({ "synthetic-access" }) public final class CatalogImageSet implements
  CancellableType
{
  private static final String                  TAG = "CImages";
  private final BitmapCacheScalingType         cache;
  private final AtomicInteger                  done;
  private final List<OPDSAcquisitionFeedEntry> entries;
  private final List<ImageView>                imageviews;
  private final List<ListenableFuture<Bitmap>> loading;
  private boolean                              cancelled;

  public CatalogImageSet(
    final BitmapCacheScalingType in_cache,
    final List<OPDSAcquisitionFeedEntry> in_entries)
  {
    this.entries = NullCheck.notNull(in_entries);
    this.imageviews = new ArrayList<ImageView>();
    this.loading = new ArrayList<ListenableFuture<Bitmap>>();
    this.done = new AtomicInteger();
    this.cache = NullCheck.notNull(in_cache);
    this.cancelled = false;
  }

  @Override public void cancel()
  {
    UIThread.checkIsUIThread();

    for (int index = 0; index < this.loading.size(); ++index) {
      final ListenableFuture<Bitmap> f = this.loading.get(index);
      f.cancel(true);
    }
    this.cancelled = true;
  }

  public void configureView(
    final Context context,
    final CatalogLaneView lane_view,
    final CatalogLaneViewListenerType listener,
    final ViewGroup container,
    final Runnable on_images_loaded)
  {
    NullCheck.notNull(context);
    NullCheck.notNull(lane_view);
    NullCheck.notNull(listener);
    NullCheck.notNull(container);
    NullCheck.notNull(on_images_loaded);

    UIThread.checkIsUIThread();

    this.done.set(0);
    this.imageviews.clear();

    container.removeAllViews();

    for (int index = 0; index < this.entries.size(); ++index) {
      final ImageView v = new ImageView(context);
      container.addView(v);
      this.imageviews.add(v);
    }

    for (int index = 0; index < this.entries.size(); ++index) {
      final OPDSAcquisitionFeedEntry e =
        NullCheck.notNull(this.entries.get(index));
      this.configureSingleView(
        lane_view,
        listener,
        on_images_loaded,
        index,
        e);
    }
  }

  private void configureSingleView(
    final CatalogLaneView lane_view,
    final CatalogLaneViewListenerType listener,
    final Runnable on_images_loaded,
    final int index,
    final OPDSAcquisitionFeedEntry e)
  {
    final ImageView view = this.imageviews.get(index);
    view.setAdjustViewBounds(true);
    view.setMaxHeight(lane_view.getScrollViewHeightInPixels());
    view.setOnClickListener(new OnClickListener() {
      @Override public void onClick(
        final @Nullable View actual)
      {
        listener.onSelectBook(lane_view, e);
      }
    });

    final OptionType<URI> thumb = e.getThumbnail();
    if (thumb.isNone()) {

      /**
       * TODO: Generate a cover.
       */

      this.imageDone(on_images_loaded);
      return;
    }

    final Some<URI> some = (Some<URI>) thumb;
    final URI thumb_uri = some.get();

    final BitmapScalingOptions options =
      BitmapScalingOptions.scaleSizeHint(
        lane_view.getScrollViewHeightInPixels(),
        lane_view.getScrollViewHeightInPixels());

    final ListenableFuture<Bitmap> f =
      CatalogImageSet.this.cache.get(
        thumb_uri,
        options,
        new BitmapCacheListenerType() {
          @Override public void onFailure(
            final Throwable ex)
          {
            UIThread.runOnUIThread(new Runnable() {
              @Override public void run()
              {
                CatalogImageSet.this.imageDone(on_images_loaded);
              }
            });
          }

          @Override public void onSuccess(
            final Bitmap b)
          {
            Log.d(CatalogImageSet.TAG, String.format(
              "returned image (%s) is (%d x %d) (%d bytes)",
              thumb_uri,
              b.getWidth(),
              b.getHeight(),
              b.getAllocationByteCount()));

            UIThread.runOnUIThread(new Runnable() {
              @Override public void run()
              {
                view.setImageBitmap(b);
                view.setVisibility(View.VISIBLE);
                CatalogImageSet.this.imageDone(on_images_loaded);
              }
            });
          }
        });

    this.loading.add(f);
  }

  private void imageDone(
    final Runnable on_images_loaded)
  {
    UIThread.checkIsUIThread();

    this.done.incrementAndGet();
    if (this.done.get() >= this.entries.size()) {
      Log.d(CatalogImageSet.TAG, "all images loaded");
      on_images_loaded.run();
    }
  }
}
