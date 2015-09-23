package org.nypl.simplified.app.catalog;

import android.graphics.Bitmap;

import java.io.IOException;
import java.net.URI;

/**
 * The type of book cover generators.
 */

public interface CatalogBookCoverGeneratorType
{
  /**
   * Generate an image synchronously.
   *
   * @param u      The image URI
   * @param width  The image width
   * @param height The image height
   *
   * @return A loaded bitmap
   *
   * @throws IOException On errors
   */

  Bitmap generateImage(
    URI u,
    int width,
    int height)
    throws IOException;

  /**
   * Generate a URI from the given title and author, suitable for use in cover
   * generation.
   *
   * @param title  The title
   * @param author The author
   *
   * @return A URI
   */

  URI generateURIForTitleAuthor(
    String title,
    String author);
}
