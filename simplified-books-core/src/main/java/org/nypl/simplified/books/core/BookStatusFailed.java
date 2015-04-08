package org.nypl.simplified.books.core;

import org.nypl.simplified.downloader.core.DownloadSnapshot;

import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;

/**
 * The given book failed to download properly.
 */

public final class BookStatusFailed implements BookStatusWithSnapshotType
{
  private final OptionType<Throwable> error;
  private final BookID                id;
  private final DownloadSnapshot      snap;

  public BookStatusFailed(
    final BookID in_id,
    final DownloadSnapshot in_snap,
    final OptionType<Throwable> x)
  {
    this.id = NullCheck.notNull(in_id);
    this.snap = NullCheck.notNull(in_snap);
    this.error = NullCheck.notNull(x);
  }

  @Override public BookID getID()
  {
    return this.id;
  }

  @Override public DownloadSnapshot getSnapshot()
  {
    return this.snap;
  }

  @Override public <A, E extends Exception> A matchBookLoanedStatus(
    final BookStatusLoanedMatcherType<A, E> m)
    throws E
  {
    return m.onBookStatusFailed(this);
  }

  @Override public <A, E extends Exception> A matchBookStatus(
    final BookStatusMatcherType<A, E> m)
    throws E
  {
    return m.onBookStatusLoanedType(this);
  }
}
