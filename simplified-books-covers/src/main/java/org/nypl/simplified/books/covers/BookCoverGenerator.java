package org.nypl.simplified.books.covers;

import android.graphics.Bitmap;

import com.io7m.jnull.NullCheck;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.librarysimplified.http.uri_builder.LSHTTPURIQueryBuilder;
import org.nypl.simplified.tenprint.TenPrintGeneratorType;
import org.nypl.simplified.tenprint.TenPrintInput;
import org.nypl.simplified.tenprint.TenPrintInputBuilderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * The default implementation of the {@link BookCoverGeneratorType}
 * interface.
 *
 * This implementation uses the provided {@link TenPrintGeneratorType} to
 * generate covers when a cover is unavailable or not specified.
 */

public final class BookCoverGenerator implements BookCoverGeneratorType
{
  private static final Logger LOG = LoggerFactory.getLogger(BookCoverGenerator.class);

  private final TenPrintGeneratorType generator;

  /**
   * Construct a new cover generator.
   *
   * @param in_generator The cover generator
   */

  public BookCoverGenerator(
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
    int width,
    int height)
    throws IOException
  {
    try {
      LOG.debug("generating: {}", u);

      final Map<String, String> params = getParameters(u);

      final String title_maybe = params.get("title");
      final String title = title_maybe == null ? "" : NullCheck.notNull(title_maybe);
      final String author_maybe = params.get("author");
      final String author = author_maybe == null ? "" : NullCheck.notNull(author_maybe);

      if (width == 0) {
        width = (int) Math.round(height * .75);
      }
      if (height == 0) {
        height = (int) Math.round(width / .75);
      }

      final TenPrintInputBuilderType ib = TenPrintInput.newBuilder();
      ib.setAuthor(author);
      ib.setTitle(title);
      ib.setCoverHeight(height);
      final TenPrintInput i = ib.build();
      final Bitmap cover = this.generator.generate(i);
      return NullCheck.notNull(cover);
    } catch (final Throwable e) {
      LOG.error("error generating image for {}: ", u, e);
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

    return LSHTTPURIQueryBuilder.INSTANCE.encodeQuery(
      URI.create("generated-cover://localhost/"),
      params
    );
  }
}
