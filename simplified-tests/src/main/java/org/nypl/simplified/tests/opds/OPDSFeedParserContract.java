package org.nypl.simplified.tests.opds;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.nypl.simplified.books.core.BookFormats;
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
import org.w3c.dom.DOMException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class OPDSFeedParserContract {

  @Rule
  public ExpectedException expected = ExpectedException.none();

  private static InputStream getResource(
      final String name)
      throws Exception {

    final String path = "/org/nypl/simplified/tests/opds/" + name;
    final URL url = OPDSFeedEntryParserContract.class.getResource(path);
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
        OPDSFeedParser.newParser(OPDSAcquisitionFeedEntryParser.newParser(
          BookFormats.Companion.supportedBookMimeTypes()));
    final InputStream d =
        OPDSFeedParserContract.getResource("acquisition-fiction-0.xml");
    final OPDSAcquisitionFeed f = p.parse(uri, d);
    d.close();

    Assert.assertEquals(
        "https://d5v0j5lesri7q.cloudfront.net/NYBKLYN/groups/",
        f.getFeedID());
    Assert.assertEquals("All Books", f.getFeedTitle());
    Assert.assertEquals(0, f.getFeedEntries().size());
    Assert.assertEquals(9, f.getFeedGroups().size());

    final Some<OPDSSearchLink> search_opt =
        (Some<OPDSSearchLink>) f.getFeedSearchURI();
    final OPDSSearchLink search = search_opt.get();
    Assert.assertEquals(
        URI.create("https://bplsimplye.bklynlibrary.org/NYBKLYN/search/"),
        search.getURI());
    Assert.assertEquals(
        "application/opensearchdescription+xml",
        search.getType());

    final Calendar u = f.getFeedUpdated();
    final Set<String> ids = new HashSet<String>();
    final Set<String> titles = new HashSet<String>();

    for (final OPDSAcquisitionFeedEntry e : f.getFeedEntries()) {
      final String e_id = e.getID();
      final String e_title = e.getTitle();
      final Calendar e_u = e.getUpdated();
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

      Assert.assertTrue(e.getPublisher().isSome());
      Assert.assertTrue(e_authors.size() > 0);
      Assert.assertTrue(e_acq.size() > 0);

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
        OPDSFeedParser.newParser(OPDSAcquisitionFeedEntryParser.newParser(
          BookFormats.Companion.supportedBookMimeTypes()));
    final InputStream d =
        OPDSFeedParserContract.getResource("acquisition-groups-0.xml");
    final OPDSAcquisitionFeed f = p.parse(uri, d);
    d.close();

    final List<OPDSAcquisitionFeedEntry> entries = f.getFeedEntries();
    for (int index = 0; index < entries.size(); ++index) {
      System.out.println(entries.get(index).getTitle());
    }

    final Map<String, OPDSGroup> groups = f.getFeedGroups();
    Assert.assertTrue(entries.isEmpty());
    Assert.assertEquals(7, groups.keySet().size());

    for (final String name : groups.keySet()) {
      System.out.println(name);
      final OPDSGroup group = groups.get(name);
      Assert.assertEquals(group.getGroupTitle(), name);
      Assert.assertTrue(group.getGroupEntries().isEmpty() == false);
    }
  }

  @Test
  public void testAcquisitionFeedPaginated0()
      throws Exception {
    final URI uri = URI.create(
        "http://library-simplified.herokuapp"
            + ".com/feed/Biography%20%26%20Memoir?order=author");
    final OPDSFeedParserType p =
        OPDSFeedParser.newParser(OPDSAcquisitionFeedEntryParser.newParser(
          BookFormats.Companion.supportedBookMimeTypes()));
    final InputStream d =
        OPDSFeedParserContract.getResource("acquisition-paginated-0.xml");
    final OPDSAcquisitionFeed f = p.parse(uri, d);
    d.close();

    Assert.assertEquals(
        "http://library-simplified.herokuapp"
            + ".com/feed/Biography%20%26%20Memoir?order=author", f.getFeedID());
    Assert.assertEquals(
        "Biography & Memoir: By author", f.getFeedTitle());
    Assert.assertEquals(50, f.getFeedEntries().size());

    final Some<URI> next_opt = (Some<URI>) f.getFeedNext();

    Assert.assertEquals(
        "http://library-simplified.herokuapp"
            + ".com/feed/Biography%20%26%20Memoir?after=155057&order=author",
        next_opt.get().toString());
  }

  @Test
  public void testDOMException()
      throws Exception {
    final URI uri =
        URI.create("http://library-simplified.herokuapp.com/feed/Fiction");

    expected.expect(OPDSParseException.class);

    final OPDSAcquisitionFeedEntryParserType ep =
        OPDSAcquisitionFeedEntryParser.newParser(BookFormats.Companion.supportedBookMimeTypes());
    final OPDSFeedParserType p = OPDSFeedParser.newParser(ep);
    final InputStream d =
        new InputStream() {
          @Override
          public int read()
              throws IOException {
            throw new DOMException((short) 0, "Bad news");
          }
        };
    p.parse(uri, d);
  }

  @Test
  public void testEmpty0()
      throws Exception {
    final URI uri =
        URI.create("http://library-simplified.herokuapp.com/feed/Fiction");
    final OPDSFeedParserType p =
        OPDSFeedParser.newParser(OPDSAcquisitionFeedEntryParser.newParser(
          BookFormats.Companion.supportedBookMimeTypes()));
    final InputStream d = OPDSFeedParserContract.getResource("empty-0.xml");
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
        OPDSFeedParser.newParser(OPDSAcquisitionFeedEntryParser.newParser(
          BookFormats.Companion.supportedBookMimeTypes()));
    final InputStream d = OPDSFeedParserContract.getResource("entry-0.xml");
    final OPDSAcquisitionFeed f = NullCheck.notNull(p.parse(uri, d));

    Assert.assertEquals(f.getFeedEntries().size(), 1);

    d.close();
  }

  @Test
  public void testNotXMLException()
      throws Exception {
    final URI uri =
        URI.create("http://library-simplified.herokuapp.com/feed/Fiction");

    expected.expect(OPDSParseException.class);

    final OPDSFeedParserType p = OPDSFeedParser.newParser(
        OPDSAcquisitionFeedEntryParser.newParser(BookFormats.Companion.supportedBookMimeTypes()));
    final InputStream d =
        OPDSFeedParserContract.getResource("bad-not-xml.xml");
    p.parse(uri, d);
  }

  @Test
  public void testParserURISyntaxException()
      throws Exception {
    final URI uri =
        URI.create("http://library-simplified.herokuapp.com/feed/Fiction");

    expected.expect(OPDSParseException.class);

    final OPDSFeedParserType p = OPDSFeedParser.newParser(
        OPDSAcquisitionFeedEntryParser.newParser(BookFormats.Companion.supportedBookMimeTypes()));
    final InputStream d =
        OPDSFeedParserContract.getResource("bad-uri-syntax.xml");
    p.parse(uri, d);
  }

  @Test
  public void testStreamIOException()
      throws Exception {
    final URI uri =
        URI.create("http://library-simplified.herokuapp.com/feed/Fiction");

    expected.expect(OPDSParseException.class);

    final OPDSFeedParserType p = OPDSFeedParser.newParser(
        OPDSAcquisitionFeedEntryParser.newParser(BookFormats.Companion.supportedBookMimeTypes()));
    final InputStream d = new InputStream() {
      @Override
      public int read()
          throws IOException {
        throw new IOException();
      }
    };
    p.parse(uri, d);
  }

  @Test
  public void testAcquisitionFeedCategories0()
      throws Exception {
    final URI uri = URI.create(
        "http://circulation.alpha.librarysimplified.org/feed/Picture%20Books");
    final OPDSFeedParserType p =
        OPDSFeedParser.newParser(OPDSAcquisitionFeedEntryParser.newParser(
          BookFormats.Companion.supportedBookMimeTypes()));
    final InputStream d =
        OPDSFeedParserContract.getResource("acquisition-categories-0.xml");
    final OPDSAcquisitionFeed f = p.parse(uri, d);
    d.close();

    final OPDSAcquisitionFeedEntry e = f.getFeedEntries().get(0);
    final List<OPDSCategory> ec = e.getCategories();

    Assert.assertEquals(3, ec.size());

    final OPDSCategory ec0 = ec.get(0);
    Assert.assertEquals(ec0.getTerm(), "Children");
    Assert.assertEquals(ec0.getScheme(), "http://schema.org/audience");

    final OPDSCategory ec1 = ec.get(1);
    Assert.assertEquals(ec1.getTerm(), "3");
    Assert.assertEquals(
        ec1.getScheme(), "http://schema.org/typicalAgeRange");

    final OPDSCategory ec2 = ec.get(2);
    Assert.assertEquals(ec2.getTerm(), "Nonfiction");
    Assert.assertEquals(
        ec2.getScheme(), "http://librarysimplified.org/terms/genres/Simplified/");
  }

  @Test
  public void testAcquisitionFeedFacets0()
      throws Exception {
    final URI uri = URI.create(
        "http://circulation.alpha.librarysimplified.org/feed/Picture%20Books");
    final OPDSFeedParserType p =
        OPDSFeedParser.newParser(OPDSAcquisitionFeedEntryParser.newParser(
          BookFormats.Companion.supportedBookMimeTypes()));
    final InputStream d =
        OPDSFeedParserContract.getResource("acquisition-facets-0.xml");
    final OPDSAcquisitionFeed f = p.parse(uri, d);
    d.close();

    final Map<String, List<OPDSFacet>> fbg = f.getFeedFacetsByGroup();
    final List<OPDSFacet> fo = f.getFeedFacetsOrder();

    Assert.assertEquals(2, fo.size());
    Assert.assertEquals(1, fbg.size());
    Assert.assertTrue(fbg.containsKey("Sort by"));

    final List<OPDSFacet> sorted = fbg.get("Sort by");
    Assert.assertEquals(2, sorted.size());

    {
      final OPDSFacet fi = sorted.get(0);
      Assert.assertEquals("Sort by", fi.getGroup());
      Assert.assertEquals("Title", fi.getTitle());
      Assert.assertTrue(!fi.isActive());
      Assert.assertEquals(
          URI.create(
              "http://circulation.alpha.librarysimplified"
                  + ".org/feed/Picture%20Books?order=title"), fi.getUri());
    }

    {
      final OPDSFacet fi = sorted.get(1);
      Assert.assertEquals("Sort by", fi.getGroup());
      Assert.assertEquals("Author", fi.getTitle());
      Assert.assertTrue(fi.isActive());
      Assert.assertEquals(
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
      OPDSFeedParser.newParser(OPDSAcquisitionFeedEntryParser.newParser(
        BookFormats.Companion.supportedBookMimeTypes()));
    final InputStream d =
      OPDSFeedParserContract.getResource("acquisition-facets-1.xml");
    final OPDSAcquisitionFeed f = p.parse(uri, d);
    d.close();

    final Map<String, List<OPDSFacet>> fbg = f.getFeedFacetsByGroup();
    final List<OPDSFacet> fo = f.getFeedFacetsOrder();

    Assert.assertEquals(2, fo.size());
    Assert.assertEquals(1, fbg.size());
    Assert.assertTrue(fbg.containsKey("Sort by"));

    final List<OPDSFacet> sorted = fbg.get("Sort by");
    Assert.assertEquals(2, sorted.size());

    {
      final OPDSFacet fi = sorted.get(0);
      Assert.assertEquals("Sort by", fi.getGroup());
      Assert.assertEquals(Option.some("Something"), fi.getGroupType());
      Assert.assertEquals("Title", fi.getTitle());
      Assert.assertTrue(!fi.isActive());
      Assert.assertEquals(
        URI.create(
          "http://circulation.alpha.librarysimplified"
            + ".org/feed/Picture%20Books?order=title"), fi.getUri());
    }

    {
      final OPDSFacet fi = sorted.get(1);
      Assert.assertEquals("Sort by", fi.getGroup());
      Assert.assertEquals(Option.some("Something"), fi.getGroupType());
      Assert.assertEquals("Author", fi.getTitle());
      Assert.assertTrue(fi.isActive());
      Assert.assertEquals(
        URI.create(
          "http://circulation.alpha.librarysimplified"
            + ".org/feed/Picture%20Books?order=author"), fi.getUri());
    }
  }
}
