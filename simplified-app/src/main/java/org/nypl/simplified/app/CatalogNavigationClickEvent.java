package org.nypl.simplified.app;

import java.io.Serializable;
import java.net.URI;

import com.io7m.jnull.NullCheck;

public final class CatalogNavigationClickEvent implements Serializable
{
  private static final long serialVersionUID = 8959538411228451109L;
  private final URI         from;
  private final URI         target;

  public CatalogNavigationClickEvent(
    final URI in_target,
    final URI in_from)
  {
    this.target = NullCheck.notNull(in_target);
    this.from = NullCheck.notNull(in_from);
  }

  public URI getFrom()
  {
    return this.from;
  }

  public URI getTarget()
  {
    return this.target;
  }
}
