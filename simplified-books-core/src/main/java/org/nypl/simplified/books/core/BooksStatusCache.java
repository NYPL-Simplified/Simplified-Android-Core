package org.nypl.simplified.books.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import org.nypl.simplified.books.core.BookStatus.RelativeImportance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;

/**
 * The default implementation of the {@link BooksStatusCacheType} interface.
 */

public final class BooksStatusCache extends Observable implements
  BooksStatusCacheType
{
  private static final Logger LOG;

  static {
    LOG = NullCheck.notNull(LoggerFactory.getLogger(BooksStatusCache.class));
  }

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

  @Override public void booksObservableAddObserver(
    final Observer o)
  {
    this.addObserver(o);
  }

  @Override public void booksObservableDeleteAllObservers()
  {
    this.deleteObservers();
  }

  @Override public void booksObservableDeleteObserver(
    final Observer o)
  {
    this.deleteObserver(o);
  }

  @Override public synchronized void booksObservableNotify(
    final BookID in_id)
  {
    this.broadcast(NullCheck.notNull(in_id));
  }

  @Override public synchronized OptionType<BookSnapshot> booksSnapshotGet(
    final BookID id)
  {
    final BookID nid = NullCheck.notNull(id);
    if (this.snapshots.containsKey(nid)) {
      return Option.some(NullCheck.notNull(this.snapshots.get(nid)));
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
    final BookStatusType s)
  {
    this.put(NullCheck.notNull(s));
  }

  @Override public synchronized void booksStatusUpdateIfMoreImportant(
    final BookStatusType update)
  {
    final BookID id = NullCheck.notNull(update).getID();
    if (this.status.containsKey(id)) {
      final BookStatusType current = NullCheck.notNull(this.status.get(id));
      final RelativeImportance compared =
        BookStatus.compareImportance(current, update);
      switch (compared) {
        case IMPORTANCE_LESS_THAN:
        {
          BooksStatusCache.LOG.debug(
            "current {} < {}, updating",
            current,
            update);
          this.put(update);
          break;
        }
        case IMPORTANCE_SAME:
        {
          BooksStatusCache.LOG.debug(
            "current {} == {}, updating",
            current,
            update);
          this.put(update);
          break;
        }
        case IMPORTANCE_MORE_THAN:
        {
          BooksStatusCache.LOG.debug(
            "current {} > {}, not updating",
            current,
            update);
          break;
        }
      }
    } else {
      this.booksStatusUpdate(update);
    }
  }

  private void broadcast(
    final BookID id)
  {
    this.setChanged();
    this.notifyObservers(id);
  }

  private <T extends BookStatusType> T put(
    final T s)
  {
    final BookID id = s.getID();
    BooksStatusCache.LOG.debug("put {}", s);
    this.status.put(id, s);
    this.broadcast(id);
    return s;
  }
}
