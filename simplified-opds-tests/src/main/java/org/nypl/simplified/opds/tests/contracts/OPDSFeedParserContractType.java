package org.nypl.simplified.opds.tests.contracts;

public interface OPDSFeedParserContractType
{
  void testNavigationFeed0()
    throws Exception;

  void testNavigationFeedBadEntryFeaturedLinkWithoutHref()
    throws Exception;

  void testNavigationFeedBadEntryLinkWithoutHref()
    throws Exception;

  void testNavigationFeedBadEntryNoLinks()
    throws Exception;

  void testNavigationFeedBadEntrySubsectionLinkWithoutHref()
    throws Exception;
}
