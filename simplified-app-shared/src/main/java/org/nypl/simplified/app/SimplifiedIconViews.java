package org.nypl.simplified.app;

import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.widget.ImageView;

import com.io7m.jnull.NullCheck;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * A class to work around the fact that most of the Android API can't open its own assets
 * via URIs. This allows URIs of the form {@code simplified-asset:file.png} to load {@code file.png}
 * from the application's assets.
 *
 * @see <a href="https://stackoverflow.com/a/7533725">https://stackoverflow.com/a/7533725</a>
 */

public final class SimplifiedIconViews {

  private static final Logger LOG = LoggerFactory.getLogger(SimplifiedIconViews.class);

  /**
   * Load the image at the given URI into the given icon view.
   *
   * @param assets    The current asset manager
   * @param icon_view The icon view
   * @param image     The image URI
   */

  public static void configureIconViewFromURI(
      final AssetManager assets,
      final ImageView icon_view,
      final URI image) {

    NullCheck.notNull(assets, "Assets");
    NullCheck.notNull(icon_view, "Icon view");
    NullCheck.notNull(image, "Image");

    if ("simplified-asset".equals(image.getScheme())) {
      final String path = image.getSchemeSpecificPart();
      LOG.debug("opening image asset: {}", path);
      try (InputStream stream = assets.open(path)) {
        icon_view.setImageDrawable(Drawable.createFromStream(stream, path));
      } catch (final IOException e) {
        LOG.error("could not open image asset: {}: ", image, e);
      }
    } else {
      icon_view.setImageURI(Uri.parse(image.toString()));
    }
  }
}
