package org.nypl.simplified.books.core;

import org.nypl.simplified.downloader.core.DownloadSnapshot;

import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;

/**
 * The given book failed to download properly.
 */

public final class BookStatusDownloadFailed implements
  BookStatusDownloadingType
{
  private final OptionType<Throwable> error;
  private final BookID                id;
  private final DownloadSnapshot      snap;

  public BookStatusDownloadFailed(
    final BookID in_id,
    final DownloadSnapshot in_snap,
    final OptionType<Throwable> x)
  {
    this.id = NullCheck.notNull(in_id);
    this.snap = NullCheck.notNull(in_snap);
    this.error = NullCheck.notNull(x);
  }

  @Override public DownloadSnapshot getDownloadSnapshot()
  {
    return this.snap;
  }

  @Override public BookID getID()
  {
    return this.id;
  }

  @Override public <A, E extends Exception> A matchBookDownloadingStatus(
    final BookStatusDownloadingMatcherType<A, E> m)
    throws E
  {
    return m.onBookStatusDownloadFailed(this);
  }

  @Override public <A, E extends Exception> A matchBookLoanedStatus(
    final BookStatusLoanedMatcherType<A, E> m)
    throws E
  {
    return m.onBookStatusDownloading(this);
  }

  @Override public <A, E extends Exception> A matchBookStatus(
    final BookStatusMatcherType<A, E> m)
    throws E
  {
    return m.onBookStatusLoanedType(this);
  }

  @Override public String toString()
  {
    final StringBuilder b = new StringBuilder();
    b.append("[BookStatusDownloadFailed ");
    b.append(this.id);
    b.append(" [");
    b.append(this.snap);
    b.append("] ");
    b.append(this.error);
    b.append("]");
    return NullCheck.notNull(b.toString());
  }
}
