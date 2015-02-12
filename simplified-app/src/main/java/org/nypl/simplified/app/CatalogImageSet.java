package org.nypl.simplified.app;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.io7m.jfunctional.None;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.OptionVisitorType;
import com.io7m.jfunctional.Some;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;

public final class CatalogImageSet
{
  private static final String                  TAG = "CatalogImageSet";
  private final List<OPDSAcquisitionFeedEntry> entries;
  private final AtomicInteger                  done;
  private final ArrayList<Button>              imageviews;

  public CatalogImageSet(
    final List<OPDSAcquisitionFeedEntry> in_entries)
  {
    this.entries = NullCheck.notNull(in_entries);
    this.imageviews = new ArrayList<Button>();
    this.done = new AtomicInteger();
  }

  void configureView(
    final Context context,
    final ViewGroup container,
    final Runnable on_images_loaded)
  {
    NullCheck.notNull(context);
    NullCheck.notNull(container);
    NullCheck.notNull(on_images_loaded);

    this.done.set(0);
    this.imageviews.clear();

    final int count = container.getChildCount();
    for (int index = 0; index < count; ++index) {
      final View v = container.getChildAt(index);
      if (v instanceof Button) {
        this.imageviews.add((Button) v);
      }
    }

    Log.d(CatalogImageSet.TAG, String.format(
      "views: %d, entries %d",
      this.imageviews.size(),
      this.entries.size()));

    if (this.imageviews.size() < this.entries.size()) {
      Log.d(
        CatalogImageSet.TAG,
        "too few imageviews for entries, creating new views");
      while (this.imageviews.size() < this.entries.size()) {
        final Button b = new Button(context);
        this.imageviews.add(b);
        container.addView(b);
      }
    }

    if (this.imageviews.size() > this.entries.size()) {
      Log.d(
        CatalogImageSet.TAG,
        "too many imageviews for entries, removing views");
      while (this.imageviews.size() > this.entries.size()) {
        final int last_index = this.imageviews.size() - 1;
        final Button last = this.imageviews.get(last_index);
        this.imageviews.remove(last_index);
        container.removeView(last);
      }
    }

    assert this.imageviews.size() == this.entries.size();

    for (int index = 0; index < this.entries.size(); ++index) {
      final OPDSAcquisitionFeedEntry e = this.entries.get(index);
      // final Button view = this.imageviews.get(index);
      final OptionType<URI> thumb = e.getThumbnail();
      thumb.accept(new OptionVisitorType<URI, Unit>() {
        @Override public Unit none(
          final None<URI> none)
        {
          /**
           * TODO: Generate a cover.
           */

          CatalogImageSet.this.imageDone(on_images_loaded);
          return Unit.unit();
        }

        @Override public Unit some(
          final Some<URI> some)
        {
          final URI thumb_uri = some.get();
          // view.setVisibility(View.VISIBLE);
          CatalogImageSet.this.imageDone(on_images_loaded);
          return Unit.unit();
        }
      });
    }
  }

  protected void imageDone(
    final Runnable on_images_loaded)
  {
    this.done.incrementAndGet();
    if (this.done.get() >= this.entries.size()) {
      Log.d(CatalogImageSet.TAG, "all images loaded");
      on_images_loaded.run();
    }
  }
}
