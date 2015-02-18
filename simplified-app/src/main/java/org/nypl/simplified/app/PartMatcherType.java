package org.nypl.simplified.app;

public interface PartMatcherType<A, E extends Exception>
{
  A books(
    PartBooks p)
    throws E;

  A catalog(
    PartCatalog p)
    throws E;

  A holds(
    PartHolds p)
    throws E;

  A settings(
    PartSettings p)
    throws E;
}
