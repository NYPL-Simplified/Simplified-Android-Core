package org.nypl.simplified.books.bundled_content;

import com.io7m.jnull.NullCheck;

import java.net.URI;

/**
 * Functions relating to URIs of bundled content.
 */

public final class BundledURIs {

  public static final String BUNDLED_CONTENT_SCHEME = "simplified-bundled";

  /**
   * @param uri The URI
   * @return {@code true} if the URI refers to bundled content
   */

  public static boolean isBundledURI(final URI uri) {
    NullCheck.notNull(uri, "uri");
    return BUNDLED_CONTENT_SCHEME.equals(uri.getScheme());
  }

  /**
   * Transform the given bundled content URI to an Android asset-based file URI.
   * @param uri The input URI
   * @return A file URI
   */

  public static URI toAndroidAssetFileURI(final URI uri) {
    NullCheck.notNull(uri, "uri");
    if (!isBundledURI(uri)) {
      throw new IllegalArgumentException("Not a bundled URI: " + uri);
    }
    final String path = uri.getSchemeSpecificPart().replaceFirst("^[/]+", "");
    return URI.create("file:///android_asset/" + path);
  }
}
