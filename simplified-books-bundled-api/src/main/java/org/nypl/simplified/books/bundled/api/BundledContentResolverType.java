package org.nypl.simplified.books.bundled.api;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * A resolver of bundled content.
 */

public interface BundledContentResolverType {

  /**
   * Resolve content for the given URI.
   *
   * @param uri The URI
   * @return A stream referring to the content
   * @throws IOException On I/O errors or missing content
   */

  InputStream resolve(URI uri)
    throws IOException;
}
