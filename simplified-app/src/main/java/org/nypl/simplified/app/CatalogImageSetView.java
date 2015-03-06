package org.nypl.simplified.app;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

@SuppressWarnings("synthetic-access") public final class CatalogImageSetView extends
  LinearLayout implements CancellableType
{
  private static final String                        TAG;

  static {
    TAG = "CImagesView";
  }

  private final CatalogAcquisitionThumbnailCacheType cache;
  private final Runnable                             done_proc;
  private final List<OPDSAcquisitionFeedEntry>       entries;
  private final ListeningExecutorService             exec;
  private int                                        image_height;
  private final BitmapDisplaySizeType                image_opts;
  private final List<ImageView>                      imageviews;
  private final CatalogLaneViewListenerType          listener;
  private final ListenableFuture<Unit>               loading;
  private final AtomicBoolean                        want_cancel;
  private final CatalogLaneView                      lane;
  private final String                               id;

  public CatalogImageSetView(
    final Context in_context,
    final CatalogLaneView in_lane,
    final ListeningExecutorService in_exec,
    final CatalogAcquisitionThumbnailCacheType in_cache,
    final List<OPDSAcquisitionFeedEntry> in_entries,
    final CatalogLaneViewListenerType in_listener,
    final int in_image_height,
    final String in_id,
    final Runnable in_done)
  {
    super(NullCheck.notNull(in_context));

    UIThread.checkIsUIThread();

    this.exec = NullCheck.notNull(in_exec);
    this.cache = NullCheck.notNull(in_cache);
    this.entries = NullCheck.notNull(in_entries);
    this.done_proc = NullCheck.notNull(in_done);
    this.lane = NullCheck.notNull(in_lane);
    this.listener = NullCheck.notNull(in_listener);
    this.id = NullCheck.notNull(in_id);

    this.imageviews = new ArrayList<ImageView>();
    this.want_cancel = new AtomicBoolean(false);
    this.image_height = in_image_height;
    this.image_opts =
      new BitmapDisplayHeightPreserveAspect(this.image_height);
    this.setOrientation(LinearLayout.HORIZONTAL);

    final LayoutParams p =
      new LayoutParams(
        android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
        android.view.ViewGroup.LayoutParams.MATCH_PARENT);
    this.setLayoutParams(p);
    this.setVisibility(View.INVISIBLE);

    for (int index = 0; index < in_entries.size(); ++index) {
      final ImageView i = new ImageView(in_context);
      this.imageviews.add(i);
      this.addView(i);
    }

    this.loading = NullCheck.notNull(in_exec.submit(new Callable<Unit>() {
      @Override public Unit call()
        throws Exception
      {
        CatalogImageSetView.this.load();
        return Unit.unit();
      }
    }));
  }

  @Override public void cancel()
  {
    Log.d(CatalogImageSetView.TAG, this.id + ": images cancelled");
    this.want_cancel.set(true);
    this.loading.cancel(true);
  }

  private void done()
  {
    Log.d(CatalogImageSetView.TAG, this.id + ": images done");

    final CatalogImageSetView sv = this;
    UIThread.runOnUIThread(new Runnable() {
      @Override public void run()
      {
        Fade.fadeIn(sv, 100);
        sv.done_proc.run();
      }
    });
  }

  private void load()
  {
    for (int index = 0; index < this.entries.size(); ++index) {
      if (this.want_cancel.get()) {
        Log.d(CatalogImageSetView.TAG, this.id + ": noticed cancellation");
        return;
      }

      final OPDSAcquisitionFeedEntry e =
        NullCheck.notNull(this.entries.get(index));
      final CatalogLaneViewListenerType closure_listener = this.listener;
      final CatalogLaneView closure_lane = this.lane;

      final ImageView i = NullCheck.notNull(this.imageviews.get(index));
      i.setOnClickListener(new OnClickListener() {
        @Override public void onClick(
          final @Nullable View v)
        {
          closure_listener.onSelectBook(closure_lane, e);
        }
      });

      final int height = this.image_height;
      final Bitmap bi =
        this.cache.getThumbnailSynchronous(e, this.image_opts);

      UIThread.runOnUIThread(new Runnable() {
        @Override public void run()
        {
          final double ratio =
            (double) bi.getHeight() / (double) bi.getWidth();
          final int w = (int) (height / ratio);

          final android.view.ViewGroup.LayoutParams p =
            new LayoutParams(w, height);
          i.setLayoutParams(p);
          i.setScaleType(ScaleType.FIT_CENTER);
          i.setImageBitmap(bi);
        }
      });
    }

    this.done();
  }
}
