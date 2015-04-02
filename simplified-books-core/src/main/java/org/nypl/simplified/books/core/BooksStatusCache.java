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

  private final Map<BookID, BookStatusType> status;

  public BooksStatusCache()
  {
    this.status = new HashMap<BookID, BookStatusType>();
  }

  @Override public synchronized void booksStatusClearAll()
  {
    this.status.clear();
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
    final BookStatusType s)
  {
    final BookID nid = NullCheck.notNull(id);
    final BookStatusType ns = NullCheck.notNull(s);
    this.status.put(nid, ns);
  }

  @Override public synchronized void booksStatusUpdateOwned(
    final BookID id)
  {
    final BookID nid = NullCheck.notNull(id);
    if (this.status.containsKey(nid)) {
      return;
    }
    this.status.put(nid, new BookStatusOwned(nid));
  }
}
