package org.nypl.simplified.books.core;

import org.nypl.simplified.downloader.core.DownloadSnapshot;

import com.io7m.jnull.NullCheck;

/**
 * The given book was cancelled during the download.
 */

public final class BookStatusCancelled implements BookStatusWithSnapshotType
{
  private final BookID           id;
  private final DownloadSnapshot snap;

  public BookStatusCancelled(
    final BookID in_id,
    final DownloadSnapshot in_snap)
  {
    this.id = NullCheck.notNull(in_id);
    this.snap = NullCheck.notNull(in_snap);
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
    return m.onBookStatusCancelled(this);
  }

  @Override public <A, E extends Exception> A matchBookStatus(
    final BookStatusMatcherType<A, E> m)
    throws E
  {
    return m.onBookStatusLoanedType(this);
  }
}
