package org.nypl.simplified.app;

import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.nypl.simplified.books.core.BookID;
import org.nypl.simplified.books.core.BookSnapshot;
import org.nypl.simplified.books.core.BooksType;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.ImageView;

import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

@SuppressWarnings("synthetic-access") public final class CoverProvider implements
  CoverProviderType
{
  private static final String                        TAG;
  private static final Callback                      EMPTY_CALLBACK;

  static {
    EMPTY_CALLBACK = new Callback() {
      @Override public void onSuccess()
      {
        // Nothing
      }

      @Override public void onError()
      {
        Log.e(CoverProvider.TAG, "failed to load image");
      }
    };

    TAG = "CProvider";
  }

  private final Picasso                              picasso;
  private final ExecutorService                      exec;
  private final BooksType                            books;
  private final CatalogAcquisitionCoverGeneratorType cover_gen;

  private CoverProvider(
    final Picasso in_p,
    final BooksType in_books,
    final CatalogAcquisitionCoverGeneratorType in_cover_gen,
    final ExecutorService in_exec)
  {
    this.picasso = NullCheck.notNull(in_p);
    this.books = NullCheck.notNull(in_books);
    this.cover_gen = NullCheck.notNull(in_cover_gen);
    this.exec = NullCheck.notNull(in_exec);
  }

  public static CoverProviderType newCoverProvider(
    final Context in_c,
    final BooksType in_books,
    final ExecutorService in_exec)
  {
    final CatalogAcquisitionCoverGenerator cover_gen =
      new CatalogAcquisitionCoverGenerator();

    final Picasso.Builder pb = new Picasso.Builder(in_c);
    pb.defaultBitmapConfig(Bitmap.Config.RGB_565);
    pb.indicatorsEnabled(true);
    pb.loggingEnabled(true);
    pb.addRequestHandler(new CatalogAcquisitionCoverGeneratorRequestHandler(
      cover_gen));
    pb.executor(in_exec);
    final Picasso p = NullCheck.notNull(pb.build());
    return new CoverProvider(p, in_books, cover_gen, in_exec);
  }

  @Override public void loadCoverInto(
    final OPDSAcquisitionFeedEntry e,
    final ImageView i,
    final int w,
    final int h)
  {
    this.loadCoverIntoWithCallback(e, i, w, h, CoverProvider.EMPTY_CALLBACK);
  }

  @Override public void loadCoverIntoWithCallback(
    final OPDSAcquisitionFeedEntry e,
    final ImageView i,
    final int w,
    final int h,
    final Callback c)
  {
    Log.d(CoverProvider.TAG, String.format("%s: load", e.getID()));

    final URI uri;
    final BookID id = BookID.newIDFromEntry(e);
    final OptionType<BookSnapshot> snap_opt = this.books.booksSnapshotGet(id);
    if (snap_opt.isSome()) {
      final BookSnapshot snap = ((Some<BookSnapshot>) snap_opt).get();

      /**
       * On receipt of a book snapshot, construct a URI from the location of
       * the cover image, if any. If there is no cover, generate one as
       * normal.
       */

      final OptionType<File> cover_opt = snap.getCover();
      if (cover_opt.isSome()) {
        final Some<File> some = (Some<File>) cover_opt;
        uri = some.get().toURI();
      } else {
        uri = CoverProvider.generateCoverURI(e, this.cover_gen);
      }

    } else {

      /**
       * If no snapshot is received, either fetch or generate a cover as with
       * ordinary books.
       */

      final OptionType<URI> cover_opt = e.getCover();
      if (cover_opt.isSome()) {
        final Some<URI> some = (Some<URI>) cover_opt;
        uri = some.get();
      } else {
        uri = CoverProvider.generateCoverURI(e, this.cover_gen);
      }
    }

    Log.d(CoverProvider.TAG, String.format("%s: uri %s", e.getID(), uri));

    final Picasso p = this.picasso;
    UIThread.runOnUIThread(new Runnable() {
      @Override public void run()
      {
        final RequestCreator r = p.load(uri.toString());
        r.resize(w, h);
        r.into(i, c);
      }
    });
  }

  @Override public void loadThumbnailInto(
    final OPDSAcquisitionFeedEntry e,
    final ImageView i,
    final int w,
    final int h)
  {
    this.loadThumbnailIntoWithCallback(
      e,
      i,
      w,
      h,
      CoverProvider.EMPTY_CALLBACK);
  }

  @Override public void loadThumbnailIntoWithCallback(
    final OPDSAcquisitionFeedEntry e,
    final ImageView i,
    final int w,
    final int h,
    final Callback c)
  {
    Log.d(CoverProvider.TAG, String.format("%s: load", e.getID()));

    final URI uri;
    final BookID id = BookID.newIDFromEntry(e);
    final OptionType<BookSnapshot> snap_opt = this.books.booksSnapshotGet(id);
    if (snap_opt.isSome()) {
      final BookSnapshot snap = ((Some<BookSnapshot>) snap_opt).get();

      /**
       * On receipt of a book snapshot, construct a URI from the location of
       * the cover image, if any. If there is no cover, generate one as
       * normal.
       */

      final OptionType<File> cover_opt = snap.getCover();
      if (cover_opt.isSome()) {
        final Some<File> some = (Some<File>) cover_opt;
        uri = some.get().toURI();
      } else {
        uri = CoverProvider.generateCoverURI(e, this.cover_gen);
      }

    } else {

      /**
       * If no snapshot is received, either fetch or generate a cover as with
       * ordinary books.
       */

      final OptionType<URI> thumb_opt = e.getThumbnail();
      if (thumb_opt.isSome()) {
        final Some<URI> some = (Some<URI>) thumb_opt;
        uri = some.get();
      } else {
        uri = CoverProvider.generateCoverURI(e, this.cover_gen);
      }
    }

    Log.d(CoverProvider.TAG, String.format("%s: uri %s", e.getID(), uri));

    final Picasso p = this.picasso;
    UIThread.runOnUIThread(new Runnable() {
      @Override public void run()
      {
        final RequestCreator r = p.load(uri.toString());
        r.resize(w, h);
        r.into(i, c);
      }
    });
  }

  private static URI generateCoverURI(
    final OPDSAcquisitionFeedEntry e,
    final CatalogAcquisitionCoverGeneratorType cg)
  {
    final String title = e.getTitle();
    final String author;
    final List<String> authors = e.getAuthors();
    if (authors.isEmpty()) {
      author = "";
    } else {
      author = NullCheck.notNull(authors.get(0));
    }
    return cg.generateURIForTitleAuthor(title, author);
  }
}
