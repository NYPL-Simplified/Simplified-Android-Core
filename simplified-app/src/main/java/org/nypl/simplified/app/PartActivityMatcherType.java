package org.nypl.simplified.app;

public interface PartActivityMatcherType<A, E extends Exception>
{
  A books(
    final BooksActivity a)
    throws E;

  A catalog(
    final CatalogActivity a)
    throws E;

  A holds(
    HoldsActivity a);

  A settings(
    final SettingsActivity a)
    throws E;
}
