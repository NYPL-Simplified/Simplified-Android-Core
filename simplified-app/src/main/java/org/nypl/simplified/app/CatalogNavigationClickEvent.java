package org.nypl.simplified.app;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayDeque;

import com.io7m.jnull.NullCheck;

public final class CatalogNavigationClickEvent implements Serializable
{
  private static final long     serialVersionUID = 8959538411228451109L;
  private final ArrayDeque<URI> stack;

  public CatalogNavigationClickEvent(
    final ArrayDeque<URI> in_stack)
  {
    this.stack = NullCheck.notNull(in_stack);
  }

  public ArrayDeque<URI> getStack()
  {
    return this.stack;
  }
}
