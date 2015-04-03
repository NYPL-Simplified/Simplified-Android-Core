package org.nypl.simplified.app;

import java.net.URI;
import java.util.SortedMap;
import java.util.TreeMap;

import org.nypl.simplified.http.core.URIQueryBuilder;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;

import android.widget.ImageView;

import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

public final class PicassoUtilities
{
  public static void loadCoverInto(
    final Picasso p,
    final OPDSAcquisitionFeedEntry e,
    final ImageView i,
    final int w,
    final int h)
  {
    NullCheck.notNull(p);
    NullCheck.notNull(e);
    NullCheck.notNull(i);

    final OptionType<URI> image_uri_opt = e.getCover();
    final URI image_uri = PicassoUtilities.makeURI(e, image_uri_opt);

    final RequestCreator r = p.load(image_uri.toString());
    r.resize(w, h);
    r.into(i);
  }

  public static void loadThumbnailInto(
    final Picasso p,
    final OPDSAcquisitionFeedEntry e,
    final ImageView i,
    final int w,
    final int h)
  {
    PicassoUtilities.loadThumbnailIntoWithCallback(
      p,
      e,
      i,
      w,
      h,
      new Callback() {
        @Override public void onError()
        {
          // Nothing
        }

        @Override public void onSuccess()
        {
          // Nothing
        }
      });
  }

  public static void loadThumbnailIntoWithCallback(
    final Picasso p,
    final OPDSAcquisitionFeedEntry e,
    final ImageView i,
    final int w,
    final int h,
    final Callback c)
  {
    NullCheck.notNull(p);
    NullCheck.notNull(e);
    NullCheck.notNull(i);
    NullCheck.notNull(c);

    final OptionType<URI> image_uri_opt = e.getThumbnail();
    final URI image_uri = PicassoUtilities.makeURI(e, image_uri_opt);

    final RequestCreator r = p.load(image_uri.toString());
    r.resize(w, h);
    r.into(i, c);
  }

  private static URI makeURI(
    final OPDSAcquisitionFeedEntry e,
    final OptionType<URI> image_uri_opt)
  {
    final URI image_uri;
    if (image_uri_opt.isSome()) {
      image_uri = ((Some<URI>) image_uri_opt).get();
    } else {
      final SortedMap<String, String> params = new TreeMap<String, String>();
      params.put("title", e.getTitle());
      params.put("author", e.getAuthors().get(0));
      image_uri =
        URIQueryBuilder.encodeQuery(
          URI.create("generated-cover://localhost/"),
          params);
    }
    return image_uri;
  }
}
