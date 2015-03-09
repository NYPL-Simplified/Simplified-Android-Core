package org.nypl.simplified.app;

import java.util.concurrent.CancellationException;

import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.common.util.concurrent.ListenableFuture;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

public final class CatalogAcquisitionCellView extends FrameLayout implements
  ExpensiveDisplayableType
{
  private static final String                TAG;

  static {
    TAG = "CACV";
  }

  private final ImageView                    cell_cover;
  private final ViewGroup                    cell_cover_layout;
  private boolean                            displayed;
  private final OPDSAcquisitionFeedEntry     entry;
  private @Nullable ListenableFuture<Bitmap> loading;

  public CatalogAcquisitionCellView(
    final Context context,
    final ScreenSizeControllerType screen,
    final OPDSAcquisitionFeedEntry in_entry,
    final CatalogAcquisitionFeedListenerType in_listener)
  {
    super(context, null);

    NullCheck.notNull(screen);
    this.entry = NullCheck.notNull(in_entry);
    NullCheck.notNull(in_listener);

    final LayoutInflater inflater =
      (LayoutInflater) context
        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    inflater.inflate(R.layout.catalog_acquisition_cell, this, true);

    final TextView cell_title =
      NullCheck.notNull((TextView) this.findViewById(R.id.cell_title));
    final TextView cell_authors =
      NullCheck.notNull((TextView) this.findViewById(R.id.cell_authors));

    /**
     * The height of the row is known, so assume a roughly 4:3 aspect ratio
     * for cover images and calculate the width of the cover layout in pixels.
     */

    final ViewGroup ccl =
      NullCheck
        .notNull((ViewGroup) this.findViewById(R.id.cell_cover_layout));
    final int cover_height = ccl.getLayoutParams().height;
    final int cover_width = (int) ((cover_height / 4.0) * 3.0);
    final LinearLayout.LayoutParams ccl_p =
      new LinearLayout.LayoutParams(cover_width, cover_height);
    ccl.setLayoutParams(ccl_p);
    this.cell_cover_layout = ccl;
    this.cell_cover = new ImageView(this.getContext());

    /**
     * Set the cell texts.
     */

    cell_title.setText(in_entry.getTitle());
    CatalogBookDetail.configureViewTextAuthor(in_entry, cell_authors);

    this.setOnClickListener(new OnClickListener() {
      @Override public void onClick(
        final @Nullable View v)
      {
        in_listener.onSelectBook(CatalogAcquisitionCellView.this, in_entry);
      }
    });
  }

  @Override public void expensiveRequestDisplay()
  {
    UIThread.checkIsUIThread();

    /**
     * Protect against multiple calls: Due to the way <tt>GridView</tt> is
     * implemented, the <tt>ListAdapter</tt> used may call this function
     * multiple times.
     */

    if (this.displayed) {
      return;
    }
    this.displayed = true;

    Log.d(CatalogAcquisitionCellView.TAG, "request start displaying: "
      + this.entry.getTitle());

    final Simplified app = Simplified.get();
    final CatalogAcquisitionThumbnailCacheType loader =
      app.getCatalogThumbnailLoader();

    final ImageView image_view = this.cell_cover;
    final ViewGroup image_container = this.cell_cover_layout;

    final BitmapDisplaySizeType size =
      new BitmapDisplayHeightPreserveAspect(
        image_container.getLayoutParams().height);

    this.loading =
      loader.getThumbnailAsynchronous(
        this.entry,
        size,
        new BitmapCacheListenerType() {
          @Override public void onFailure(
            final Throwable x)
          {
            if (x instanceof CancellationException) {
              return;
            }

            Log.e(CatalogAcquisitionCellView.TAG, x.getMessage(), x);
          }

          @Override public void onSuccess(
            final Bitmap b)
          {
            UIThread.runOnUIThread(new Runnable() {
              @Override public void run()
              {
                image_container.removeAllViews();
                image_container.addView(image_view);
                image_view.setImageBitmap(b);
                Fade.fadeIn(image_view, Fade.DEFAULT_FADE_DURATION);
              }
            });
          }
        });
  }

  @Override public void expensiveRequestStopDisplaying()
  {
    Log.d(CatalogAcquisitionCellView.TAG, "request stop displaying: "
      + this.entry.getTitle());

    final ListenableFuture<Bitmap> lf = this.loading;
    if (lf != null) {
      lf.cancel(true);
    }
    this.loading = null;
    this.displayed = false;
    this.cell_cover_layout.removeAllViews();
  }
}
