package org.nypl.simplified.http.core;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.SortedMap;

import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;

public final class URIQueryBuilder
{
  public static URI encodeQuery(
    final URI base,
    final SortedMap<String, String> parameters)
  {
    try {
      NullCheck.notNull(base);
      NullCheck.notNull(parameters);

      if (parameters.isEmpty() == false) {
        final Iterator<String> iter = parameters.keySet().iterator();
        final StringBuilder query = new StringBuilder();
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

  private URIQueryBuilder()
  {
    throw new UnreachableCodeException();
  }
}
