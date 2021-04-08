package org.nypl.simplified.tests.opds;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.nypl.simplified.opds.core.OPDSAcquisition;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeed;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParser;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParserType;
import org.nypl.simplified.opds.core.OPDSCategory;
import org.nypl.simplified.opds.core.OPDSFacet;
import org.nypl.simplified.opds.core.OPDSFeedParser;
import org.nypl.simplified.opds.core.OPDSFeedParserType;
import org.nypl.simplified.opds.core.OPDSGroup;
import org.nypl.simplified.opds.core.OPDSParseException;
import org.nypl.simplified.opds.core.OPDSSearchLink;
import org.nypl.simplified.parser.api.ParseError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import one.irradia.mime.api.MIMEType;

public final class OPDSFeedParserTest {

  private final Logger logger = LoggerFactory.getLogger(OPDSFeedParserTest.class);

  private static InputStream getResource(
    final String name)
    throws Exception {

    final String path = "/org/nypl/simplified/tests/opds/" + name;
    final URL url = OPDSFeedEntryParserTest.class.getResource(path);
    if (url == null) {
      throw new FileNotFoundException(path);
    }
    return url.openStream();
  }

  @Test
  public void testAcquisitionFeedFiction0()
    throws Exception {
    final URI uri = URI.create(
      "http://circulation.alpha.librarysimplified.org/feed/Picture%20Books");
    final OPDSFeedParserType p =
      OPDSFeedParser.newParser(OPDSAcquisitionFeedEntryParser.newParser());
    final InputStream d =
      OPDSFeedParserTest.getResource("acquisition-fiction-0.xml");
    final OPDSAcquisitionFeed f = p.parse(uri, d);
    d.close();

    Assertions.assertEquals(
      "https://d5v0j5lesri7q.cloudfront.net/NYBKLYN/groups/",
      f.getFeedID());
    Assertions.assertEquals("All Books", f.getFeedTitle());
    Assertions.assertEquals(0, f.getFeedEntries().size());
    Assertions.assertEquals(9, f.getFeedGroups().size());

    final Some<OPDSSearchLink> search_opt =
      (Some<OPDSSearchLink>) f.getFeedSearchURI();
    final OPDSSearchLink search = search_opt.get();
    Assertions.assertEquals(
      URI.create("https://bplsimplye.bklynlibrary.org/NYBKLYN/search/"),
      search.getURI());
    Assertions.assertEquals(
      "application/opensearchdescription+xml",
      search.getType());

    final DateTime u = f.getFeedUpdated();
    final Set<String> ids = new HashSet<String>();
    final Set<String> titles = new HashSet<String>();

    for (final OPDSAcquisitionFeedEntry e : f.getFeedEntries()) {
      final String e_id = e.getID();
      final String e_title = e.getTitle();
      final DateTime e_u = e.getUpdated();
      final List<OPDSAcquisition> e_acq = e.getAcquisitions();
      final List<String> e_authors = e.getAuthors();
      final OptionType<URI> e_thumb = e.getThumbnail();
      final OptionType<URI> e_cover = e.getCover();

      System.out.print("authors: ");
      for (final String a : e_authors) {
        System.out.print(a);
      }
      System.out.println();

      System.out.print("acquisitions: ");
      for (final OPDSAcquisition ea : e_acq) {
        System.out.print(ea);
      }

      System.out.println();
      System.out.println("id: " + e_id);
      System.out.println("title: " + e_title);
      System.out.println("update: " + e_u);
      System.out.println("thumbnail: " + e_thumb);
      System.out.println("cover: " + e_cover);

      Assertions.assertTrue(e.getPublisher().isSome());
      Assertions.assertTrue(e_authors.size() > 0);
      Assertions.assertTrue(e_acq.size() > 0);

      if (ids.contains(e_id)) {
        throw new AssertionError("Duplicate ID: " + e_id);
      }
      ids.add(e_id);

      System.out.println("--");
    }
  }

  @Test
  public void testAcquisitionFeedGroups0()
    throws Exception {
    final URI uri =
      URI.create("http://circulation.alpha.librarysimplified.org/groups/");
    final OPDSFeedParserType p =
      OPDSFeedParser.newParser(OPDSAcquisitionFeedEntryParser.newParser());
    final InputStream d =
      OPDSFeedParserTest.getResource("acquisition-groups-0.xml");
    final OPDSAcquisitionFeed f = p.parse(uri, d);
    d.close();

    final List<OPDSAcquisitionFeedEntry> entries = f.getFeedEntries();
    for (int index = 0; index < entries.size(); ++index) {
      System.out.println(entries.get(index).getTitle());
    }

    final Map<String, OPDSGroup> groups = f.getFeedGroups();
    Assertions.assertTrue(entries.isEmpty());
    Assertions.assertEquals(24, groups.keySet().size());

    for (final String name : groups.keySet()) {
      System.out.println(name);
      final OPDSGroup group = groups.get(name);
      Assertions.assertEquals(group.getGroupTitle(), name);
      Assertions.assertTrue(group.getGroupEntries().isEmpty() == false);
    }
  }

  @Test
  public void testAcquisitionFeedPaginated0()
    throws Exception {
    final URI uri = URI.create(
      "http://library-simplified.herokuapp"
        + ".com/feed/Biography%20%26%20Memoir?order=author");
    final OPDSFeedParserType p =
      OPDSFeedParser.newParser(OPDSAcquisitionFeedEntryParser.newParser());
    final InputStream d =
      OPDSFeedParserTest.getResource("acquisition-paginated-0.xml");
    final OPDSAcquisitionFeed f = p.parse(uri, d);
    d.close();

    Assertions.assertEquals("/NYNYPL/feed/13", f.getFeedID());
    Assertions.assertEquals("Historical Fiction", f.getFeedTitle());
    Assertions.assertEquals(50, f.getFeedEntries().size());

    final Some<URI> next_opt = (Some<URI>) f.getFeedNext();

    Assertions.assertEquals(
      "https://d2txvnljjb5oij.cloudfront.net/NYNYPL/feed/13?available=now&collection=full&entrypoint=Book&key=%5B%22%5Cu1516%5Cu1693%5Cu49c7%5Cu2443%5Cu5152%5Cu1011%5Cu1098%5Cu4646%5Cu1a03%5Cu0114%5Cu0706%5Cu2011%5Cu480e%5Cu72f3%5Cu3981%5Cu5c06%5Cu0000%5Cu0001%22%2C+%22%5Cu2b1c%5Cu0e94%5Cu0640%5Cu6043%5Cu6192%5Cu1111%5Cu3402%5Cu1001%5Cu7060%5Cu3702%5Cu0000%5Cu0001%22%2C+264205%5D&order=title&size=50",
      next_opt.get().toString());
  }

  @Test
  public void testDOMException()
    throws Exception {
    final URI uri =
      URI.create("http://library-simplified.herokuapp.com/feed/Fiction");

    final OPDSAcquisitionFeedEntryParserType ep =
      OPDSAcquisitionFeedEntryParser.newParser();
    final OPDSFeedParserType p = OPDSFeedParser.newParser(ep);
    final InputStream d =
      new InputStream() {
        @Override
        public int read()
          throws IOException {
          throw new DOMException((short) 0, "Bad news");
        }
      };

    Assertions.assertThrows(OPDSParseException.class, () -> {
      p.parse(uri, d);
    });
  }

  @Test
  public void testEmpty0()
    throws Exception {
    final URI uri =
      URI.create("http://library-simplified.herokuapp.com/feed/Fiction");
    final OPDSFeedParserType p =
      OPDSFeedParser.newParser(OPDSAcquisitionFeedEntryParser.newParser());
    final InputStream d = OPDSFeedParserTest.getResource("empty-0.xml");
    final OPDSAcquisitionFeed f = p.parse(uri, d);
    NullCheck.notNull(f);
    d.close();
  }

  @Test
  public void testEntryAsFeed0()
    throws Exception {
    final URI uri =
      URI.create("http://library-simplified.herokuapp.com/feed/Fiction");
    final OPDSFeedParserType p =
      OPDSFeedParser.newParser(OPDSAcquisitionFeedEntryParser.newParser());
    final InputStream d = OPDSFeedParserTest.getResource("entry-0.xml");
    final OPDSAcquisitionFeed f = NullCheck.notNull(p.parse(uri, d));

    Assertions.assertEquals(f.getFeedEntries().size(), 1);

    d.close();
  }

  @Test
  public void testNotXMLException()
    throws Exception {
    final URI uri =
      URI.create("http://library-simplified.herokuapp.com/feed/Fiction");

    final OPDSFeedParserType p = OPDSFeedParser.newParser(
      OPDSAcquisitionFeedEntryParser.newParser());
    final InputStream d =
      OPDSFeedParserTest.getResource("bad-not-xml.xml");

    Assertions.assertThrows(OPDSParseException.class, () -> {
      p.parse(uri, d);
    });
  }

  @Test
  public void testParserURISyntaxException()
    throws Exception {
    final URI uri =
      URI.create("http://library-simplified.herokuapp.com/feed/Fiction");

    final OPDSFeedParserType p = OPDSFeedParser.newParser(
      OPDSAcquisitionFeedEntryParser.newParser());
    final InputStream d =
      OPDSFeedParserTest.getResource("bad-uri-syntax.xml");
    final OPDSAcquisitionFeed result =
      p.parse(uri, d);
    final List<ParseError> errors =
      result.getErrors();

    Assertions.assertEquals(1, errors.size());
    Assertions.assertEquals(URISyntaxException.class, errors.get(0).getException().getClass());
  }

  @Test
  public void testStreamIOException()
    throws Exception {
    final URI uri =
      URI.create("http://library-simplified.herokuapp.com/feed/Fiction");

    final OPDSFeedParserType p = OPDSFeedParser.newParser(
      OPDSAcquisitionFeedEntryParser.newParser());
    final InputStream d = new InputStream() {
      @Override
      public int read()
        throws IOException {
        throw new IOException();
      }
    };

    Assertions.assertThrows(OPDSParseException.class, () -> {
      p.parse(uri, d);
    });
  }

  @Test
  public void testAcquisitionFeedCategories0()
    throws Exception {
    final URI uri = URI.create(
      "http://circulation.alpha.librarysimplified.org/feed/Picture%20Books");
    final OPDSFeedParserType p =
      OPDSFeedParser.newParser(OPDSAcquisitionFeedEntryParser.newParser());
    final InputStream d =
      OPDSFeedParserTest.getResource("acquisition-categories-0.xml");
    final OPDSAcquisitionFeed f = p.parse(uri, d);
    d.close();

    final OPDSAcquisitionFeedEntry e = f.getFeedEntries().get(0);
    final List<OPDSCategory> ec = e.getCategories();

    Assertions.assertEquals(3, ec.size());

    final OPDSCategory ec0 = ec.get(0);
    Assertions.assertEquals(ec0.getTerm(), "Children");
    Assertions.assertEquals(ec0.getScheme(), "http://schema.org/audience");

    final OPDSCategory ec1 = ec.get(1);
    Assertions.assertEquals(ec1.getTerm(), "3");
    Assertions.assertEquals(
      ec1.getScheme(), "http://schema.org/typicalAgeRange");

    final OPDSCategory ec2 = ec.get(2);
    Assertions.assertEquals(ec2.getTerm(), "Nonfiction");
    Assertions.assertEquals(
      ec2.getScheme(), "http://librarysimplified.org/terms/genres/Simplified/");
  }

  @Test
  public void testAcquisitionFeedFacets0()
    throws Exception {
    final URI uri = URI.create(
      "http://circulation.alpha.librarysimplified.org/feed/Picture%20Books");
    final OPDSFeedParserType p =
      OPDSFeedParser.newParser(OPDSAcquisitionFeedEntryParser.newParser());
    final InputStream d =
      OPDSFeedParserTest.getResource("acquisition-facets-0.xml");
    final OPDSAcquisitionFeed f = p.parse(uri, d);
    d.close();

    final Map<String, List<OPDSFacet>> fbg = f.getFeedFacetsByGroup();
    final List<OPDSFacet> fo = f.getFeedFacetsOrder();

    Assertions.assertEquals(2, fo.size());
    Assertions.assertEquals(1, fbg.size());
    Assertions.assertTrue(fbg.containsKey("Sort by"));

    final List<OPDSFacet> sorted = fbg.get("Sort by");
    Assertions.assertEquals(2, sorted.size());

    {
      final OPDSFacet fi = sorted.get(0);
      Assertions.assertEquals("Sort by", fi.getGroup());
      Assertions.assertEquals("Title", fi.getTitle());
      Assertions.assertTrue(!fi.isActive());
      Assertions.assertEquals(
        URI.create(
          "http://circulation.alpha.librarysimplified"
            + ".org/feed/Picture%20Books?order=title"), fi.getUri());
    }

    {
      final OPDSFacet fi = sorted.get(1);
      Assertions.assertEquals("Sort by", fi.getGroup());
      Assertions.assertEquals("Author", fi.getTitle());
      Assertions.assertTrue(fi.isActive());
      Assertions.assertEquals(
        URI.create(
          "http://circulation.alpha.librarysimplified"
            + ".org/feed/Picture%20Books?order=author"), fi.getUri());
    }
  }

  @Test
  public void testAcquisitionFeedFacets1()
    throws Exception {
    final URI uri = URI.create(
      "http://circulation.alpha.librarysimplified.org/feed/Picture%20Books");
    final OPDSFeedParserType p =
      OPDSFeedParser.newParser(OPDSAcquisitionFeedEntryParser.newParser());
    final InputStream d =
      OPDSFeedParserTest.getResource("acquisition-facets-1.xml");
    final OPDSAcquisitionFeed f = p.parse(uri, d);
    d.close();

    final Map<String, List<OPDSFacet>> fbg = f.getFeedFacetsByGroup();
    final List<OPDSFacet> fo = f.getFeedFacetsOrder();

    Assertions.assertEquals(2, fo.size());
    Assertions.assertEquals(1, fbg.size());
    Assertions.assertTrue(fbg.containsKey("Sort by"));

    final List<OPDSFacet> sorted = fbg.get("Sort by");
    Assertions.assertEquals(2, sorted.size());

    {
      final OPDSFacet fi = sorted.get(0);
      Assertions.assertEquals("Sort by", fi.getGroup());
      Assertions.assertEquals(Option.some("Something"), fi.getGroupType());
      Assertions.assertEquals("Title", fi.getTitle());
      Assertions.assertTrue(!fi.isActive());
      Assertions.assertEquals(
        URI.create(
          "http://circulation.alpha.librarysimplified"
            + ".org/feed/Picture%20Books?order=title"), fi.getUri());
    }

    {
      final OPDSFacet fi = sorted.get(1);
      Assertions.assertEquals("Sort by", fi.getGroup());
      Assertions.assertEquals(Option.some("Something"), fi.getGroupType());
      Assertions.assertEquals("Author", fi.getTitle());
      Assertions.assertTrue(fi.isActive());
      Assertions.assertEquals(
        URI.create(
          "http://circulation.alpha.librarysimplified"
            + ".org/feed/Picture%20Books?order=author"), fi.getUri());
    }
  }

  @Test
  public void testAnalytics20190509()
    throws Exception {
    final URI uri = URI.create("urn:example");
    final OPDSFeedParserType p =
      OPDSFeedParser.newParser(OPDSAcquisitionFeedEntryParser.newParser());
    final InputStream d =
      OPDSFeedParserTest.getResource("analytics-20190509.xml");
    final OPDSAcquisitionFeed f = p.parse(uri, d);
    d.close();

    for (final OPDSAcquisitionFeedEntry e : f.getFeedEntries()) {
      final OptionType<URI> analytics = e.getAnalytics();
      this.logger.debug("analytics: {}", e.getAnalytics());
      Assertions.assertTrue(
        analytics.isSome(),
        "Analytics link must exist");
      final URI analyticsURI = ((Some<URI>) analytics).get();
      Assertions.assertTrue(
        analyticsURI.toString().contains("open_book"),
        "URI must contain 'open_book'");
    }
  }

  @Test
  public void testFeedBooks20190509()
    throws Exception {
    final URI uri = URI.create("http://www.example.com/");
    final OPDSFeedParserType p =
      OPDSFeedParser.newParser(OPDSAcquisitionFeedEntryParser.newParser());
    final InputStream d =
      OPDSFeedParserTest.getResource("feedbooks-20190808.xml");
    final OPDSAcquisitionFeed f = p.parse(uri, d);
    d.close();

    Assertions.assertEquals(
      Option.some(URI.create("http://www.example.com/opensearch.xml")),
      f.getFeedSearchURI().map(OPDSSearchLink::getURI));
  }

  @Test
  public void testDPLATestFeed()
    throws Exception {
    final URI uri = URI.create("http://www.example.com/");
    final OPDSFeedParserType p =
      OPDSFeedParser.newParser(OPDSAcquisitionFeedEntryParser.newParser());
    final InputStream d =
      OPDSFeedParserTest.getResource("dpla-test-feed.xml");
    final OPDSAcquisitionFeed f = p.parse(uri, d);

    final Map.Entry<String, OPDSGroup> groupEntry =
      f.getFeedGroups().entrySet().iterator().next();
    final OPDSGroup group =
      groupEntry.getValue();
    final OPDSAcquisitionFeedEntry entry =
      group.getGroupEntries().get(0);
    final List<OPDSAcquisition> acquisitions =
      entry.getAcquisitions();
    final OPDSAcquisition acquisition =
      acquisitions.get(0);

    Assertions.assertEquals(1, acquisition.availableFinalContentTypes().size());
    final MIMEType finalType = acquisition.availableFinalContentTypes().iterator().next();
    Assertions.assertEquals("application", finalType.getType());
    Assertions.assertEquals("audiobook+json", finalType.getSubtype());
    Assertions.assertEquals(
      "http://www.feedbooks.com/audiobooks/access-restriction",
      finalType.getParameters().get("profile"));

    Assertions.assertEquals(1, acquisitions.size());
    d.close();
  }
}
