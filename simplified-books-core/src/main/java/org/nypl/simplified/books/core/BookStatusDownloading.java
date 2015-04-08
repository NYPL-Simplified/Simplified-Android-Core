package org.nypl.simplified.books.core;

import org.nypl.simplified.downloader.core.DownloadSnapshot;

import com.io7m.jnull.NullCheck;

/**
 * The given book is currently downloading.
 */

public final class BookStatusDownloading implements
  BookStatusWithSnapshotType
{
  private final BookID           id;
  private final DownloadSnapshot snap;

  public BookStatusDownloading(
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
    b.append("[BookStatusDownloading ");
    b.append(this.id);
    b.append(" [");
    b.append(this.snap);
    b.append("]]");
    return NullCheck.notNull(b.toString());
  }
}
