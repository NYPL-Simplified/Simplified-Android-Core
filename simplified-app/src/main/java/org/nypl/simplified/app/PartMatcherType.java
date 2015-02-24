package org.nypl.simplified.app;

public interface PartMatcherType<A, E extends Exception>
{
  A matchBooks(
    final PartBooks p)
    throws E;

  A matchCatalog(
    final PartCatalog p)
    throws E;

  A matchHolds(
    final PartHolds p)
    throws E;

  A matchSettings(
    final PartSettings p)
    throws E;
}
