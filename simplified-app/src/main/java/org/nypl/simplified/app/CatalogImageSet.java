package org.nypl.simplified.app;

import java.net.URI;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;

import android.content.Context;
import android.util.Log;
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

  public CatalogImageSet(
    final List<OPDSAcquisitionFeedEntry> in_entries)
  {
    this.entries = NullCheck.notNull(in_entries);
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

    container.removeAllViews();
    for (final OPDSAcquisitionFeedEntry e : this.entries) {
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
          final Button b = new Button(context);
          container.addView(b);

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
