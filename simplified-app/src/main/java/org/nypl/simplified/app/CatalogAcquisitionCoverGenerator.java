package org.nypl.simplified.app;

import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

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

  private static Map<String, String> getParameters(
    final URI u)
  {
    final Map<String, String> m = new HashMap<String, String>();
    final List<NameValuePair> p = URLEncodedUtils.parse(u, "UTF-8");
    final Iterator<NameValuePair> iter = p.iterator();
    while (iter.hasNext()) {
      final NameValuePair nvp = iter.next();
      m.put(nvp.getName(), nvp.getValue());
    }
    return m;
  }

  public CatalogAcquisitionCoverGenerator()
  {
    // Nothing
  }

  @Override public Bitmap generateImage(
    final URI u,
    final int width,
    final int height)
  {
    Log.d(
      CatalogAcquisitionCoverGenerator.TAG,
      String.format("generating %s", u));

    final Map<String, String> params =
      CatalogAcquisitionCoverGenerator.getParameters(u);
    final String title = NullCheck.notNull(params.get("title"));

    final Bitmap b =
      Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

    final Canvas c = new Canvas(b);
    final Paint p = new Paint();

    final int hash = title.hashCode();

    p.setStyle(Style.FILL);
    p.setColor(hash);
    p.setAlpha(0xff);
    c.drawRect(0, 0, width, height, p);

    p.setColor(Color.WHITE);
    p.setAlpha(0xff);
    c.drawRect(4, 4, width - 4, height / 4, p);

    p.setColor(Color.BLACK);
    p.setAlpha(0xff);
    p.setAntiAlias(true);
    c.drawText(TextUtilities.ellipsize(title, 16), 8, 16, p);

    return NullCheck.notNull(b);
  }
}
