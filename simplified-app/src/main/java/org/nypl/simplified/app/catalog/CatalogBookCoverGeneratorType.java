package org.nypl.simplified.app.catalog;

import java.io.IOException;
import java.net.URI;

import android.graphics.Bitmap;

public interface CatalogBookCoverGeneratorType
{
  /**
   * Generate an image synchronously.
   *
   * @param u
   *          The image URI
   * @return A loaded bitmap
   * @throws IOException
   *           On errors
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
   * @param title
   *          The title
   * @param author
   *          The author
   * @return A URI
   */

  URI generateURIForTitleAuthor(
    String title,
    String author);
}
