package org.nypl.simplified.app.catalog;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import com.io7m.jnull.NullCheck;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.nypl.simplified.books.core.LogUtilities;
import org.nypl.simplified.http.core.URIQueryBuilder;
import org.nypl.simplified.tenprint.TenPrintGeneratorType;
import org.nypl.simplified.tenprint.TenPrintInput;
import org.nypl.simplified.tenprint.TenPrintInputBuilderType;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * The default implementation of the {@link CatalogBookCoverGeneratorType}
 * interface.
 *
 * This implementation uses the provided {@link TenPrintGeneratorType} to
 * generate covers when a cover is unavailable or not specified.
 */

public final class CatalogBookCoverGenerator
  implements CatalogBookCoverGeneratorType
{
  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(CatalogBookCoverGenerator.class);
  }

  private final TenPrintGeneratorType generator;

  /**
   * Construct a new cover generator.
   *
   * @param in_generator The cover generator
   */

  public CatalogBookCoverGenerator(
    final TenPrintGeneratorType in_generator)
  {
    this.generator = NullCheck.notNull(in_generator);
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

  @Override public Bitmap generateImage(
    final URI u,
    final int width,
    final int height)
    throws IOException
  {
    try {
      CatalogBookCoverGenerator.LOG.debug("generating: {}", u);

      final Map<String, String> params =
        CatalogBookCoverGenerator.getParameters(u);

      final String title_maybe = params.get("title");
      final String title =
        title_maybe == null ? "" : NullCheck.notNull(title_maybe);
      final String author_maybe = params.get("author");
      final String author =
        author_maybe == null ? "" : NullCheck.notNull(author_maybe);

      final TenPrintInputBuilderType ib = TenPrintInput.newBuilder();
      ib.setAuthor(author);
      ib.setTitle(title);
      ib.setCoverHeight(height);
      final TenPrintInput i = ib.build();
      final Bitmap cover = this.generator.generate(i);

      final Bitmap container =
        Bitmap.createBitmap(width, height, Config.RGB_565);
      final Canvas c = new Canvas(container);
      final Paint white = new Paint();
      white.setColor(Color.WHITE);
      c.drawRect(0.0F, 0.0F, (float) width, (float) height, white);
      c.drawBitmap(cover, (float) ((width - cover.getWidth()) / 2), 0.0F, null);
      return NullCheck.notNull(container);
    } catch (final Throwable e) {
      CatalogBookCoverGenerator.LOG.error(
        "error generating image for {}: ", u, e);
      throw new IOException(e);
    }
  }

  @Override public URI generateURIForTitleAuthor(
    final String title,
    final String author)
  {
    final SortedMap<String, String> params = new TreeMap<String, String>();
    params.put("title", NullCheck.notNull(title));
    params.put("author", NullCheck.notNull(author));
    return URIQueryBuilder.encodeQuery(
      NullCheck.notNull(URI.create("generated-cover://localhost/")), params);
  }
}
