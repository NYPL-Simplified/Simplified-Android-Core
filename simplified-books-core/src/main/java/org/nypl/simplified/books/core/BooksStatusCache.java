package org.nypl.simplified.books.core;

import java.util.HashMap;
import java.util.Map;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;

public final class BooksStatusCache implements BooksStatusCacheType
{
  public static BooksStatusCacheType newStatusCache()
  {
    return new BooksStatusCache();
  }

  private final Map<BookID, BookSnapshot>   snapshots;
  private final Map<BookID, BookStatusType> status;

  public BooksStatusCache()
  {
    this.status = new HashMap<BookID, BookStatusType>();
    this.snapshots = new HashMap<BookID, BookSnapshot>();
  }

  @Override public synchronized OptionType<BookSnapshot> booksSnapshotGet(
    final BookID id)
  {
    final BookID nid = NullCheck.notNull(id);
    if (this.snapshots.containsKey(nid)) {
      return Option.some(this.snapshots.get(nid));
    }
    return Option.none();
  }

  @Override public synchronized void booksSnapshotUpdate(
    final BookID id,
    final BookSnapshot snap)
  {
    this.snapshots.put(id, snap);
  }

  @Override public synchronized void booksStatusClearAll()
  {
    this.status.clear();
    this.snapshots.clear();
  }

  @Override public synchronized OptionType<BookStatusType> booksStatusGet(
    final BookID id)
  {
    NullCheck.notNull(id);
    if (this.status.containsKey(id)) {
      return Option.some(NullCheck.notNull(this.status.get(id)));
    }
    return Option.none();
  }

  @Override public synchronized void booksStatusUpdate(
    final BookID id,
    final BookStatusLoanedType s)
  {
    final BookID nid = NullCheck.notNull(id);
    final BookStatusLoanedType ns = NullCheck.notNull(s);
    this.status.put(nid, ns);
  }

  @Override public synchronized void booksStatusUpdateLoaned(
    final BookID id)
  {
    final BookID nid = NullCheck.notNull(id);
    if (this.status.containsKey(nid)) {
      return;
    }
    this.status.put(nid, new BookStatusLoaned(nid));
  }

  @Override public synchronized void booksStatusUpdateRequesting(
    final BookID id)
  {
    final BookID nid = NullCheck.notNull(id);
    if (this.status.containsKey(nid)) {
      return;
    }
    this.status.put(nid, new BookStatusRequesting(nid));
  }
}
