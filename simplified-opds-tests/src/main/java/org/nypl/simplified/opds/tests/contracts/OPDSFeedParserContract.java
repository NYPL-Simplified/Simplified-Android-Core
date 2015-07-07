package org.nypl.simplified.opds.tests.contracts;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nypl.simplified.opds.core.OPDSAcquisition;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeed;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntryParser;
import org.nypl.simplified.opds.core.OPDSCategory;
import org.nypl.simplified.opds.core.OPDSFacet;
import org.nypl.simplified.opds.core.OPDSParseException;
import org.nypl.simplified.opds.core.OPDSFeedParser;
import org.nypl.simplified.opds.core.OPDSFeedParserType;
import org.nypl.simplified.opds.core.OPDSGroup;
import org.nypl.simplified.opds.core.OPDSSearchLink;
import org.nypl.simplified.test.utilities.TestUtilities;
import org.w3c.dom.DOMException;

import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.PartialProcedureType;
import com.io7m.jfunctional.Some;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;

@SuppressWarnings({ "boxing", "null", "resource" }) public final class OPDSFeedParserContract implements
  OPDSFeedParserContractType
{
  public static InputStream getResource(
    final String name)
    throws Exception
  {
    return NullCheck.notNull(OPDSFeedParserContract.class
      .getResourceAsStream(name));
  }

  public OPDSFeedParserContract()
  {
    // Nothing
  }

  @Override public void testAcquisitionFeedFiction0()
    throws Exception
  {
    final URI uri =
      URI
        .create("http://circulation.alpha.librarysimplified.org/feed/Picture%20Books");
    final OPDSFeedParserType p =
      OPDSFeedParser.newParser(OPDSAcquisitionFeedEntryParser.newParser());
    final InputStream d =
      OPDSFeedParserContract.getResource("acquisition-fiction-0.xml");
    final OPDSAcquisitionFeed f = p.parse(uri, d);
    d.close();

    TestUtilities.assertEquals(
      "http://circulation.alpha.librarysimplified.org/feed/Picture%20Books",
      f.getFeedID());
    TestUtilities.assertEquals("Picture Books: By author", f.getFeedTitle());
    TestUtilities.assertEquals(50, f.getFeedEntries().size());

    final Some<OPDSSearchLink> search_opt =
      (Some<OPDSSearchLink>) f.getFeedSearchURI();
    final OPDSSearchLink search = search_opt.get();
    TestUtilities
      .assertEquals(
        URI
          .create("http://circulation.alpha.librarysimplified.org/search/Picture%20Books"),
        search.getURI());
    TestUtilities.assertEquals(
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

      TestUtilities.assertTrue(e.getPublisher().isSome());
      TestUtilities.assertGreater(e_authors.size(), 0);
      TestUtilities.assertGreater(e_acq.size(), 0);

      if (ids.contains(e_id)) {
        throw new AssertionError("Duplicate ID: " + e_id);
      }
      ids.add(e_id);

      System.out.println("--");
    }
  }

  @Override public void testAcquisitionFeedGroups0()
    throws Exception
  {
    final URI uri =
      URI.create("http://circulation.alpha.librarysimplified.org/groups/");
    final OPDSFeedParserType p =
      OPDSFeedParser.newParser(OPDSAcquisitionFeedEntryParser.newParser());
    final InputStream d =
      OPDSFeedParserContract.getResource("acquisition-groups-0.xml");
    final OPDSAcquisitionFeed f = p.parse(uri, d);
    d.close();

    TestUtilities.assertTrue(f.getFeedEntries().isEmpty());

    final Map<String, OPDSGroup> groups = f.getFeedGroups();
    TestUtilities.assertEquals(7, groups.keySet().size());

    for (final String name : groups.keySet()) {
      System.out.println(name);
      final OPDSGroup group = groups.get(name);
      TestUtilities.assertEquals(group.getGroupTitle(), name);
      TestUtilities.assertTrue(group.getGroupEntries().isEmpty() == false);
    }
  }

  @Override public void testAcquisitionFeedPaginated0()
    throws Exception
  {
    final URI uri =
      URI
        .create("http://library-simplified.herokuapp.com/feed/Biography%20%26%20Memoir?order=author");
    final OPDSFeedParserType p =
      OPDSFeedParser.newParser(OPDSAcquisitionFeedEntryParser.newParser());
    final InputStream d =
      OPDSFeedParserContract.getResource("acquisition-paginated-0.xml");
    final OPDSAcquisitionFeed f = p.parse(uri, d);
    d.close();

    TestUtilities
      .assertEquals(
        "http://library-simplified.herokuapp.com/feed/Biography%20%26%20Memoir?order=author",
        f.getFeedID());
    TestUtilities.assertEquals(
      "Biography & Memoir: By author",
      f.getFeedTitle());
    TestUtilities.assertEquals(50, f.getFeedEntries().size());

    final Some<URI> next_opt = (Some<URI>) f.getFeedNext();

    TestUtilities
      .assertEquals(
        "http://library-simplified.herokuapp.com/feed/Biography%20%26%20Memoir?after=155057&order=author",
        next_opt.get().toString());
  }

  @Override public void testDOMException()
    throws Exception
  {
    final URI uri =
      URI.create("http://library-simplified.herokuapp.com/feed/Fiction");

    TestUtilities.expectException(
      OPDSParseException.class,
      new PartialProcedureType<Unit, Exception>() {
        @Override public void call(
          final Unit x)
          throws Exception
        {
          final OPDSFeedParserType p =
            OPDSFeedParser.newParser(OPDSAcquisitionFeedEntryParser
              .newParser());
          final InputStream d = new InputStream() {
            @Override public int read()
              throws IOException
            {
              throw new DOMException((short) 0, "Bad news");
            }
          };
          p.parse(uri, d);
        }
      });
  }

  @Override public void testEmpty0()
    throws Exception
  {
    final URI uri =
      URI.create("http://library-simplified.herokuapp.com/feed/Fiction");
    final OPDSFeedParserType p =
      OPDSFeedParser.newParser(OPDSAcquisitionFeedEntryParser.newParser());
    final InputStream d = OPDSFeedParserContract.getResource("empty-0.xml");
    final OPDSAcquisitionFeed f = p.parse(uri, d);
    NullCheck.notNull(f);
    d.close();
  }

  @Override public void testNotXMLException()
    throws Exception
  {
    final URI uri =
      URI.create("http://library-simplified.herokuapp.com/feed/Fiction");

    TestUtilities.expectException(
      OPDSParseException.class,
      new PartialProcedureType<Unit, Exception>() {
        @Override public void call(
          final Unit x)
          throws Exception
        {
          final OPDSFeedParserType p =
            OPDSFeedParser.newParser(OPDSAcquisitionFeedEntryParser
              .newParser());
          final InputStream d =
            OPDSFeedParserContract.getResource("bad-not-xml.xml");
          p.parse(uri, d);
        }
      });
  }

  @Override public void testParserURISyntaxException()
    throws Exception
  {
    final URI uri =
      URI.create("http://library-simplified.herokuapp.com/feed/Fiction");

    TestUtilities.expectException(
      OPDSParseException.class,
      new PartialProcedureType<Unit, Exception>() {
        @Override public void call(
          final Unit x)
          throws Exception
        {
          final OPDSFeedParserType p =
            OPDSFeedParser.newParser(OPDSAcquisitionFeedEntryParser
              .newParser());
          final InputStream d =
            OPDSFeedParserContract.getResource("bad-uri-syntax.xml");
          p.parse(uri, d);
        }
      });
  }

  @Override public void testStreamIOException()
    throws Exception
  {
    final URI uri =
      URI.create("http://library-simplified.herokuapp.com/feed/Fiction");

    TestUtilities.expectException(
      OPDSParseException.class,
      new PartialProcedureType<Unit, Exception>() {
        @Override public void call(
          final Unit x)
          throws Exception
        {
          final OPDSFeedParserType p =
            OPDSFeedParser.newParser(OPDSAcquisitionFeedEntryParser
              .newParser());
          final InputStream d = new InputStream() {
            @Override public int read()
              throws IOException
            {
              throw new IOException();
            }
          };
          p.parse(uri, d);
        }
      });
  }

  @Override public void testAcquisitionFeedCategories0()
    throws Exception
  {
    final URI uri =
      URI
        .create("http://circulation.alpha.librarysimplified.org/feed/Picture%20Books");
    final OPDSFeedParserType p =
      OPDSFeedParser.newParser(OPDSAcquisitionFeedEntryParser.newParser());
    final InputStream d =
      OPDSFeedParserContract.getResource("acquisition-categories-0.xml");
    final OPDSAcquisitionFeed f = p.parse(uri, d);
    d.close();

    final OPDSAcquisitionFeedEntry e = f.getFeedEntries().get(0);
    final List<OPDSCategory> ec = e.getCategories();

    TestUtilities.assertEquals(3, ec.size());

    final OPDSCategory ec0 = ec.get(0);
    TestUtilities.assertEquals(ec0.getTerm(), "Children");
    TestUtilities.assertEquals(ec0.getScheme(), "http://schema.org/audience");

    final OPDSCategory ec1 = ec.get(1);
    TestUtilities.assertEquals(ec1.getTerm(), "3");
    TestUtilities.assertEquals(
      ec1.getScheme(),
      "http://schema.org/typicalAgeRange");

    final OPDSCategory ec2 = ec.get(2);
    TestUtilities.assertEquals(ec2.getTerm(), "Nonfiction");
    TestUtilities.assertEquals(
      ec2.getScheme(),
      "http://librarysimplified.org/terms/genres/Simplified/");
  }

  @Override public void testAcquisitionFeedFacets0()
    throws Exception
  {
    final URI uri =
      URI
        .create("http://circulation.alpha.librarysimplified.org/feed/Picture%20Books");
    final OPDSFeedParserType p =
      OPDSFeedParser.newParser(OPDSAcquisitionFeedEntryParser.newParser());
    final InputStream d =
      OPDSFeedParserContract.getResource("acquisition-categories-0.xml");
    final OPDSAcquisitionFeed f = p.parse(uri, d);
    d.close();

    final Map<String, List<OPDSFacet>> fbg = f.getFeedFacetsByGroup();
    final List<OPDSFacet> fo = f.getFeedFacetsOrder();

    TestUtilities.assertEquals(2, fo.size());
    TestUtilities.assertEquals(1, fbg.size());
    TestUtilities.assertTrue(fbg.containsKey("Sort by"));

    final List<OPDSFacet> sorted = fbg.get("Sort by");
    TestUtilities.assertEquals(2, sorted.size());

    {
      final OPDSFacet fi = sorted.get(0);
      TestUtilities.assertEquals("Sort by", fi.getGroup());
      TestUtilities.assertEquals("Title", fi.getTitle());
      TestUtilities.assertTrue(!fi.isActive());
      TestUtilities
        .assertEquals(
          URI
            .create("http://circulation.alpha.librarysimplified.org/feed/Picture%20Books?order=title"),
          fi.getURI());
    }

    {
      final OPDSFacet fi = sorted.get(1);
      TestUtilities.assertEquals("Sort by", fi.getGroup());
      TestUtilities.assertEquals("Author", fi.getTitle());
      TestUtilities.assertTrue(fi.isActive());
      TestUtilities
        .assertEquals(
          URI
            .create("http://circulation.alpha.librarysimplified.org/feed/Picture%20Books?order=author"),
          fi.getURI());
    }
  }
}
