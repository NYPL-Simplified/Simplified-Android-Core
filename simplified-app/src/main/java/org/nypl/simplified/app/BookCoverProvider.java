package org.nypl.simplified.app;

import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.nypl.simplified.app.catalog.CatalogBookCoverGeneratorRequestHandler;
import org.nypl.simplified.app.catalog.CatalogBookCoverGeneratorType;
import org.nypl.simplified.app.utilities.LogUtilities;
import org.nypl.simplified.app.utilities.UIThread;
import org.nypl.simplified.books.core.BookID;
import org.nypl.simplified.books.core.BookSnapshot;
import org.nypl.simplified.books.core.BooksType;
import org.nypl.simplified.books.core.FeedEntryOPDS;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.slf4j.Logger;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.widget.ImageView;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

@SuppressWarnings("synthetic-access") public final class BookCoverProvider implements
  BookCoverProviderType
{
  private static final String COVER_TAG;
  private static final Logger LOG;
  private static final String THUMBNAIL_TAG;

  static {
    LOG = LogUtilities.getLog(BookCoverProvider.class);
    THUMBNAIL_TAG = "thumbnail";
    COVER_TAG = "cover";
  }

  private static URI generateCoverURI(
    final FeedEntryOPDS e,
    final CatalogBookCoverGeneratorType cg)
  {
    final OPDSAcquisitionFeedEntry eo = e.getFeedEntry();
    final String title = eo.getTitle();
    final String author;
    final List<String> authors = eo.getAuthors();
    if (authors.isEmpty()) {
      author = "";
    } else {
      author = NullCheck.notNull(authors.get(0));
    }
    return cg.generateURIForTitleAuthor(title, author);
  }

  private static void load(
    final FeedEntryOPDS e,
    final ImageView i,
    final int w,
    final int h,
    final @Nullable Callback c,
    final Picasso p,
    final CatalogBookCoverGeneratorType cg,
    final String tag,
    final OptionType<URI> uri_opt)
  {
    final URI uri_generated = BookCoverProvider.generateCoverURI(e, cg);
    if (uri_opt.isSome()) {
      final URI uri_specified = ((Some<URI>) uri_opt).get();

      BookCoverProvider.LOG.debug(
        "{}: {}: loading specified uri {}",
        tag,
        e.getBookID(),
        uri_specified);

      final RequestCreator r = p.load(uri_specified.toString());
      r.tag(tag);
      r.resize(w, h);
      r.into(i, new Callback() {
        @Override public void onError()
        {
          BookCoverProvider.LOG.debug(
            "{}: {}: failed to load uri {}, falling back to generation",
            tag,
            e.getBookID(),
            uri_specified);

          final RequestCreator fallback_r = p.load(uri_generated.toString());
          fallback_r.tag(tag);
          fallback_r.resize(w, h);
          fallback_r.into(i, c);
        }

        @Override public void onSuccess()
        {
          if (c != null) {
            c.onSuccess();
          }
        }
      });
    } else {
      BookCoverProvider.LOG.debug(
        "{}: {}: loading generated uri {}",
        tag,
        e.getBookID(),
        uri_generated);

      final RequestCreator r = p.load(uri_generated.toString());
      r.tag(tag);
      r.resize(w, h);
      r.into(i, c);
    }
  }

  public static BookCoverProviderType newCoverProvider(
    final Context in_c,
    final BooksType in_books,
    final CatalogBookCoverGeneratorType in_generator,
    final ExecutorService in_exec)
  {
    final Resources rr = in_c.getResources();
    final Picasso.Builder pb = new Picasso.Builder(in_c);
    pb.defaultBitmapConfig(Bitmap.Config.RGB_565);
    pb
      .indicatorsEnabled(rr.getBoolean(R.bool.debug_picasso_cache_indicators));
    pb.loggingEnabled(rr.getBoolean(R.bool.debug_picasso_logging));
    pb.addRequestHandler(new CatalogBookCoverGeneratorRequestHandler(
      in_generator));
    pb.executor(in_exec);

    final Picasso p = NullCheck.notNull(pb.build());
    return new BookCoverProvider(p, in_books, in_generator);
  }

  private final BooksType                     books;
  private final CatalogBookCoverGeneratorType cover_gen;
  private final Picasso                       picasso;

  private BookCoverProvider(
    final Picasso in_p,
    final BooksType in_books,
    final CatalogBookCoverGeneratorType in_cover_gen)
  {
    this.picasso = NullCheck.notNull(in_p);
    this.books = NullCheck.notNull(in_books);
    this.cover_gen = NullCheck.notNull(in_cover_gen);
  }

  private OptionType<URI> getCoverURI(
    final FeedEntryOPDS e)
  {
    final OPDSAcquisitionFeedEntry eo = e.getFeedEntry();
    final BookID id = e.getBookID();
    final OptionType<BookSnapshot> snap_opt = this.books.booksSnapshotGet(id);
    if (snap_opt.isSome()) {
      final BookSnapshot snap = ((Some<BookSnapshot>) snap_opt).get();
      final OptionType<File> cover_opt = snap.getCover();
      if (cover_opt.isSome()) {
        final Some<File> some = (Some<File>) cover_opt;
        return Option.some(NullCheck.notNull(some.get().toURI()));
      }
    }

    return eo.getCover();
  }

  private OptionType<URI> getThumbnailURI(
    final FeedEntryOPDS e)
  {
    final OPDSAcquisitionFeedEntry eo = e.getFeedEntry();
    final BookID id = e.getBookID();
    final OptionType<BookSnapshot> snap_opt = this.books.booksSnapshotGet(id);
    if (snap_opt.isSome()) {
      final BookSnapshot snap = ((Some<BookSnapshot>) snap_opt).get();
      final OptionType<File> cover_opt = snap.getCover();
      if (cover_opt.isSome()) {
        final Some<File> some = (Some<File>) cover_opt;
        return Option.some(NullCheck.notNull(some.get().toURI()));
      }
    }

    return eo.getThumbnail();
  }

  @Override public void loadCoverInto(
    final FeedEntryOPDS e,
    final ImageView i,
    final int w,
    final int h)
  {
    NullCheck.notNull(e);
    NullCheck.notNull(i);
    this.loadCoverIntoActual(e, i, w, h, null);
  }

  private void loadCoverIntoActual(
    final FeedEntryOPDS e,
    final ImageView i,
    final int w,
    final int h,
    final @Nullable Callback c)
  {
    final OPDSAcquisitionFeedEntry eo = e.getFeedEntry();

    BookCoverProvider.LOG.debug(
      "{}: loadCoverInto {}",
      e.getBookID(),
      eo.getID());

    UIThread.checkIsUIThread();

    final OptionType<URI> uri_opt = this.getCoverURI(e);
    BookCoverProvider.load(
      e,
      i,
      w,
      h,
      c,
      this.picasso,
      this.cover_gen,
      BookCoverProvider.COVER_TAG,
      uri_opt);
  }

  @Override public void loadCoverIntoWithCallback(
    final FeedEntryOPDS e,
    final ImageView i,
    final int w,
    final int h,
    final Callback c)
  {
    NullCheck.notNull(e);
    NullCheck.notNull(i);
    NullCheck.notNull(c);
    this.loadCoverIntoActual(e, i, w, h, c);
  }

  @Override public void loadingThumbailsPause()
  {
    this.picasso.pauseTag(BookCoverProvider.THUMBNAIL_TAG);
  }

  @Override public void loadingThumbnailsContinue()
  {
    this.picasso.resumeTag(BookCoverProvider.THUMBNAIL_TAG);
  }

  @Override public void loadThumbnailInto(
    final FeedEntryOPDS e,
    final ImageView i,
    final int w,
    final int h)
  {
    NullCheck.notNull(e);
    NullCheck.notNull(i);
    this.loadThumbnailIntoActual(e, i, w, h, null);
  }

  private void loadThumbnailIntoActual(
    final FeedEntryOPDS e,
    final ImageView i,
    final int w,
    final int h,
    final @Nullable Callback c)
  {
    final OPDSAcquisitionFeedEntry eo = e.getFeedEntry();

    BookCoverProvider.LOG.debug(
      "{}: loadThumbnailInto {}",
      e.getBookID(),
      eo.getID());

    UIThread.checkIsUIThread();

    final OptionType<URI> uri_opt = this.getThumbnailURI(e);
    BookCoverProvider.load(
      e,
      i,
      w,
      h,
      c,
      this.picasso,
      this.cover_gen,
      BookCoverProvider.THUMBNAIL_TAG,
      uri_opt);
  }

  @Override public void loadThumbnailIntoWithCallback(
    final FeedEntryOPDS e,
    final ImageView i,
    final int w,
    final int h,
    final Callback c)
  {
    NullCheck.notNull(e);
    NullCheck.notNull(i);
    NullCheck.notNull(c);
    this.loadThumbnailIntoActual(e, i, w, h, c);
  }
}
