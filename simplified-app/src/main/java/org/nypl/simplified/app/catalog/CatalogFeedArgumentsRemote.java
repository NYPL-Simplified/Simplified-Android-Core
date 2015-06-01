package org.nypl.simplified.app.catalog;

import java.net.URI;

import org.nypl.simplified.stack.ImmutableStack;

import com.io7m.jnull.NullCheck;

public final class CatalogFeedArgumentsRemote implements
  CatalogFeedArgumentsType
{
  private static final long                              serialVersionUID;

  static {
    serialVersionUID = 1L;
  }

  private final boolean                                  drawer_open;
  private final String                                   title;
  private final ImmutableStack<CatalogFeedArgumentsType> up_stack;
  private final URI                                      uri;

  public CatalogFeedArgumentsRemote(
    final boolean in_drawer_open,
    final ImmutableStack<CatalogFeedArgumentsType> in_up_stack,
    final String in_title,
    final URI in_uri)
  {
    this.drawer_open = in_drawer_open;
    this.up_stack = NullCheck.notNull(in_up_stack);
    this.title = NullCheck.notNull(in_title);
    this.uri = NullCheck.notNull(in_uri);
  }

  @Override public String getTitle()
  {
    return this.title;
  }

  @Override public ImmutableStack<CatalogFeedArgumentsType> getUpStack()
  {
    return this.up_stack;
  }

  public URI getURI()
  {
    return this.uri;
  }

  public boolean isDrawerOpen()
  {
    return this.drawer_open;
  }

  @Override public <A, E extends Exception> A matchArguments(
    final CatalogFeedArgumentsMatcherType<A, E> m)
    throws E
  {
    return m.onFeedArgumentsRemote(this);
  }

  @Override public String toString()
  {
    final StringBuilder b = new StringBuilder();
    b.append("[CatalogFeedArgumentsRemote drawer_open=");
    b.append(this.drawer_open);
    b.append(" title=");
    b.append(this.title);
    b.append(" up_stack=");
    b.append(this.up_stack);
    b.append(" uri=");
    b.append(this.uri);
    b.append("]");
    return NullCheck.notNull(b.toString());
  }
}
