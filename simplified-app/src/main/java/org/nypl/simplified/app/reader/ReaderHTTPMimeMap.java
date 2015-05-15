package org.nypl.simplified.app.reader;

import java.util.concurrent.ConcurrentHashMap;

import com.io7m.jnull.NullCheck;

/**
 * The default implementation of the {@link ReaderHTTPMimeMapType} type.
 */

public final class ReaderHTTPMimeMap implements ReaderHTTPMimeMapType
{
  public static ReaderHTTPMimeMapType newMap(
    final String in_default_type)
  {
    return new ReaderHTTPMimeMap(in_default_type);
  }

  private final String                            default_type;
  private final ConcurrentHashMap<String, String> types;

  private ReaderHTTPMimeMap(
    final String in_default_type)
  {
    this.default_type = NullCheck.notNull(in_default_type);
    this.types = new ConcurrentHashMap<String, String>();
    this.types.put("css", "text/css");
    this.types.put("html", "text/html");
    this.types.put("js", "application/javascript");
    this.types.put("png", "image/png");
    this.types.put("xhtml", "application/xhtml+xml");
  }

  @Override public String getDefaultMimeType()
  {
    return this.default_type;
  }

  @Override public String getMimeTypeForSuffix(
    final String suffix)
  {
    NullCheck.notNull(suffix);
    if (this.types.containsKey(suffix)) {
      return NullCheck.notNull(this.types.get(suffix));
    }
    return this.getDefaultMimeType();
  }
}
