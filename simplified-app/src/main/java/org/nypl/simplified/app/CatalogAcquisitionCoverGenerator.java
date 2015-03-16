package org.nypl.simplified.app;

import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.Log;

import com.io7m.jnull.NullCheck;

public final class CatalogAcquisitionCoverGenerator implements
  CatalogAcquisitionCoverGeneratorType
{
  private static final String TAG;

  static {
    TAG = "CAIG";
  }

  public CatalogAcquisitionCoverGenerator()
  {
    // Nothing
  }

  @Override public Bitmap generateImage(
    final OPDSAcquisitionFeedEntry e,
    final int height)
  {
    Log.d(
      CatalogAcquisitionCoverGenerator.TAG,
      String.format("generating %s", e.getID()));

    final int width = (height / 4) * 3;
    final Bitmap b =
      Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

    final Canvas c = new Canvas(b);
    final Paint p = new Paint();

    final int c0 =
      Color.argb(
        0xff,
        (int) (Math.random() * 0xff),
        (int) (Math.random() * 0xff),
        (int) (Math.random() * 0xff));
    final int c1 = 0xffffffff ^ c0;

    p.setStyle(Style.FILL);
    p.setColor(c0);
    p.setAlpha(0xff);
    c.drawRect(0, 0, width, height, p);

    p.setColor(c1);
    p.setAlpha(0xff);
    c.drawText(e.getTitle(), 8, 16, p);

    return NullCheck.notNull(b);
  }
}
