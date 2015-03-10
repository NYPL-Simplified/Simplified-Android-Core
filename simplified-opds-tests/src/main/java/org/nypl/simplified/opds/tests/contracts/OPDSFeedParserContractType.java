package org.nypl.simplified.opds.tests.contracts;

public interface OPDSFeedParserContractType
{
  void testAcquisitionFeedFiction0()
    throws Exception;

  void testDOMException()
    throws Exception;

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

  void testNotXMLException()
    throws Exception;

  void testParserURISyntaxException()
    throws Exception;

  void testStreamIOException()
    throws Exception;

  void testAcquisitionFeedPaginated0()
    throws Exception;
}
