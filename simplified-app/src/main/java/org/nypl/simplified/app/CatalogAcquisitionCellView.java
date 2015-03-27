package org.nypl.simplified.app;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicReference;

import org.nypl.simplified.opds.core.OPDSAcquisition;
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
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.common.util.concurrent.ListenableFuture;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

/**
 * A single cell in an acquisition list or grid.
 */

public final class CatalogAcquisitionCellView extends FrameLayout implements
  BitmapCacheListenerType<OPDSAcquisitionFeedEntry>
{
  private static final String TAG;

  static {
    TAG = "CACV";
  }

  private static String ellipsize(
    final String t,
    final int at)
  {
    if (t.length() > at) {
      return NullCheck.notNull(t.substring(0, at - 1) + "…");
    }
    return t;
  }

  private static String makeAuthorText(
    final OPDSAcquisitionFeedEntry in_e)
  {
    final StringBuilder sb = new StringBuilder();
    final List<String> as = in_e.getAuthors();
    final int max = as.size();
    for (int index = 0; index < max; ++index) {
      final String a = NullCheck.notNull(as.get(index));
      sb.append(a);
      if ((index + 1) < max) {
        sb.append(", ");
      }
    }
    return CatalogAcquisitionCellView.ellipsize(
      NullCheck.notNull(sb.toString()),
      48);
  }

  private static String makeTitleText(
    final OPDSAcquisitionFeedEntry in_e)
  {
    return CatalogAcquisitionCellView.ellipsize(in_e.getTitle(), 48);
  }

  private final TextView                                  cell_authors;
  private final ViewGroup                                 cell_buttons;
  private final ImageView                                 cell_cover_image;
  private final ViewGroup                                 cell_cover_layout;
  private final ProgressBar                               cell_cover_progress;
  private final ViewGroup                                 cell_text_layout;
  private final TextView                                  cell_title;
  private final AtomicReference<OPDSAcquisitionFeedEntry> entry;
  private @Nullable ListenableFuture<Bitmap>              loading;

  public CatalogAcquisitionCellView(
    final Context context)
  {
    super(context, null);

    final LayoutInflater inflater =
      (LayoutInflater) context
        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    inflater.inflate(R.layout.catalog_acquisition_cell, this, true);

    this.cell_text_layout =
      NullCheck.notNull((ViewGroup) this.findViewById(R.id.cell_text_layout));
    this.cell_title =
      NullCheck.notNull((TextView) this.cell_text_layout
        .findViewById(R.id.cell_title));
    this.cell_authors =
      NullCheck.notNull((TextView) this.cell_text_layout
        .findViewById(R.id.cell_authors));
    this.cell_buttons =
      NullCheck.notNull((ViewGroup) this.cell_text_layout
        .findViewById(R.id.cell_buttons));

    final ViewGroup ccl =
      NullCheck
        .notNull((ViewGroup) this.findViewById(R.id.cell_cover_layout));
    final ImageView cc =
      NullCheck.notNull((ImageView) ccl.findViewById(R.id.cell_cover_image));
    final ProgressBar pb =
      NullCheck.notNull((ProgressBar) ccl
        .findViewById(R.id.cell_cover_loading));

    /**
     * The height of the row is known, so assume a roughly 4:3 aspect ratio
     * for cover images and calculate the width of the cover layout in pixels.
     */

    final int cover_height = ccl.getLayoutParams().height;
    final int cover_width = (int) ((cover_height / 4.0) * 3.0);
    final LinearLayout.LayoutParams ccl_p =
      new LinearLayout.LayoutParams(cover_width, cover_height);
    ccl.setLayoutParams(ccl_p);

    this.cell_cover_layout = ccl;
    this.cell_cover_image = cc;
    this.cell_cover_progress = pb;
    this.entry = new AtomicReference<OPDSAcquisitionFeedEntry>();
  }

  @Override public void onBitmapLoadingFailure(
    final OPDSAcquisitionFeedEntry key,
    final Throwable x)
  {
    if (x instanceof CancellationException) {
      return;
    }

    Log.e(CatalogAcquisitionCellView.TAG, x.getMessage(), x);
  }

  @Override public void onBitmapLoadingSuccess(
    final OPDSAcquisitionFeedEntry key,
    final Bitmap b)
  {
    final OPDSAcquisitionFeedEntry current = this.entry.get();
    final String current_name;
    final String current_id;
    if (current != null) {
      current_name = current.getTitle();
      current_id = current.getID();
    } else {
      current_name = "(null)";
      current_id = "(null)";
    }

    /**
     * If the received acquisition entry ID matches that of the current ID,
     * then the cell is being reused for the same entry and so the bitmap
     * should not be replaced.
     */

    final Boolean should_set =
      Boolean.valueOf(current_id.equals(key.getID()));

    Log.d(CatalogAcquisitionCellView.TAG, String.format(
      "image received, setting: %s (current '%s' / received '%s')",
      should_set,
      current_name,
      key.getTitle()));

    if (should_set.booleanValue()) {
      final ImageView image_view = this.cell_cover_image;
      final ProgressBar progress = this.cell_cover_progress;

      UIThread.runOnUIThread(new Runnable() {
        @Override public void run()
        {
          image_view.setImageBitmap(b);
          image_view.setVisibility(View.VISIBLE);
          Fade.fadeIn(image_view, Fade.DEFAULT_FADE_DURATION);
          progress.setVisibility(View.INVISIBLE);
        }
      });
    }
  }

  public void viewConfigure(
    final OPDSAcquisitionFeedEntry in_e,
    final CatalogAcquisitionThumbnailCacheType in_image_loader,
    final CatalogAcquisitionFeedListenerType in_listener)
  {
    NullCheck.notNull(in_e);
    NullCheck.notNull(in_image_loader);
    NullCheck.notNull(in_listener);

    UIThread.checkIsUIThread();

    this.cell_title.setText(CatalogAcquisitionCellView.makeTitleText(in_e));
    this.cell_authors
      .setText(CatalogAcquisitionCellView.makeAuthorText(in_e));

    this.setOnClickListener(new OnClickListener() {
      @Override public void onClick(
        final @Nullable View v)
      {
        in_listener.onSelectBook(CatalogAcquisitionCellView.this, in_e);
      }
    });

    final BitmapDisplaySizeType size =
      new BitmapDisplayHeightPreserveAspect(
        this.cell_cover_layout.getLayoutParams().height);

    if (this.entry.get() == in_e) {
      Log.d(CatalogAcquisitionCellView.TAG, "cell same " + in_e.getTitle());
    } else {
      Log.d(CatalogAcquisitionCellView.TAG, "cell new  " + in_e.getTitle());
      this.entry.set(in_e);
      final ListenableFuture<Bitmap> l = this.loading;
      if (l != null) {
        l.cancel(true);
      }
      this.cell_cover_image.setVisibility(View.INVISIBLE);
      this.cell_cover_progress.setVisibility(View.VISIBLE);
      this.loading =
        in_image_loader.getThumbnailAsynchronous(in_e, size, this);

      final Context ctx = NullCheck.notNull(this.getContext());
      this.cell_buttons.removeAllViews();
      for (final OPDSAcquisition a : in_e.getAcquisitions()) {
        switch (a.getType()) {
          case ACQUISITION_BORROW:
          case ACQUISITION_OPEN_ACCESS:
          {
            final CatalogAcquisitionButton b =
              new CatalogAcquisitionButton(ctx, NullCheck.notNull(a));
            b.setTextSize(12.0f);
            this.cell_buttons.addView(b);
            break;
          }
          case ACQUISITION_SAMPLE:
          case ACQUISITION_SUBSCRIBE:
          case ACQUISITION_BUY:
          case ACQUISITION_GENERIC:
          {
            /**
             * TODO: Not yet supported.
             */

            break;
          }
        }
      }
    }
  }
}
