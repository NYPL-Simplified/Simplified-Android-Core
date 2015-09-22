package org.nypl.simplified.app.reader;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The default implementation of the {@link ReaderHTTPMimeMapType} type.
 */

public final class ReaderHTTPMimeMap implements ReaderHTTPMimeMapType
{
  private final String              default_type;
  private final Map<String, String> types;

  private ReaderHTTPMimeMap(
    final String in_default_type)
  {
    this.default_type = NullCheck.notNull(in_default_type);
    this.types = new ConcurrentHashMap<String, String>();
    this.types.put("asc", "text/plain");
    this.types.put("class", "application/octet-stream");
    this.types.put("css", "text/css");
    this.types.put("doc", "application/msword");
    this.types.put("exe", "application/octet-stream");
    this.types.put("flv", "video/x-flv");
    this.types.put("gif", "image/gif");
    this.types.put("htm", "text/html");
    this.types.put("html", "text/html");
    this.types.put("java", "text/x-java-source, text/java");
    this.types.put("jpeg", "image/jpeg");
    this.types.put("jpg", "image/jpeg");
    this.types.put("js", "application/javascript");
    this.types.put("m3u", "audio/mpeg-url");
    this.types.put("mov", "video/quicktime");
    this.types.put("mp3", "audio/mpeg");
    this.types.put("mp4", "video/mp4");
    this.types.put("ogg", "application/x-ogg");
    this.types.put("ogv", "video/ogg");
    this.types.put("pdf", "application/pdf");
    this.types.put("png", "image/png");
    this.types.put("swf", "application/x-shockwave-flash");
    this.types.put("txt", "text/plain");
    this.types.put("webm", "video/webm");
    this.types.put("xhtml", "application/xhtml+xml");
    this.types.put("xml", "application/xml");
    this.types.put("zip", "application/octet-stream");
  }

  /**
   * Construct a new MIME map
   *
   * @param in_default_type The default content type, if there is no known type
   *                        for the given suffix
   *
   * @return A new MIME map
   */

  public static ReaderHTTPMimeMapType newMap(
    final String in_default_type)
  {
    return new ReaderHTTPMimeMap(in_default_type);
  }

  private static OptionType<String> getSuffix(
    final String path)
  {
    final int i = path.lastIndexOf('.');
    if (i > 0) {
      return Option.some(NullCheck.notNull(path.substring(i + 1)));
    }
    return Option.none();
  }

  @Override public String getDefaultMimeType()
  {
    return this.default_type;
  }

  @Override public String guessMimeTypeForURI(final String u)
  {
    NullCheck.notNull(u);

    final OptionType<String> opt = ReaderHTTPMimeMap.getSuffix(u);
    if (opt.isSome()) {
      final String suffix = ((Some<String>) opt).get();
      return this.getMimeTypeForSuffix(suffix);
    }
    return this.getDefaultMimeType();
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
