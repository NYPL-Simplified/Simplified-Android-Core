package org.nypl.simplified.http.core;

import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.SortedMap;

/**
 * Functions for producing URIs.
 */

public final class URIQueryBuilder
{
  private URIQueryBuilder()
  {
    throw new UnreachableCodeException();
  }

  /**
   * Encode a query using the given base URI and set of parameters.
   *
   * @param base       The base URI
   * @param parameters The parameters
   *
   * @return An encoded query
   */

  public static URI encodeQuery(
    final URI base,
    final SortedMap<String, String> parameters)
  {
    try {
      NullCheck.notNull(base);
      NullCheck.notNull(parameters);

      if (parameters.isEmpty() == false) {
        final Iterator<String> iter = parameters.keySet().iterator();
        final StringBuilder query = new StringBuilder(128);
        while (iter.hasNext()) {
          final String name = NullCheck.notNull(iter.next());
          final String value = NullCheck.notNull(parameters.get(name));
          query.append(URLEncoder.encode(name, "UTF-8"));
          query.append("=");
          query.append(URLEncoder.encode(value, "UTF-8"));
          if (iter.hasNext()) {
            query.append("&");
          }
        }

        final String qs = query.toString();
        final String uri_text = base.toASCIIString();
        return NullCheck.notNull(URI.create(uri_text + "?" + qs));
      }

      return NullCheck.notNull(base);
    } catch (final UnsupportedEncodingException e) {
      throw new UnreachableCodeException(e);
    }
  }
}
