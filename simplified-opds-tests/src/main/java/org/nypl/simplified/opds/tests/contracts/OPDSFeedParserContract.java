package org.nypl.simplified.opds.tests.contracts;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.nypl.simplified.opds.core.OPDSAcquisition;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeed;
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;
import org.nypl.simplified.opds.core.OPDSFeedParseException;
import org.nypl.simplified.opds.core.OPDSFeedParser;
import org.nypl.simplified.opds.core.OPDSFeedParserType;
import org.nypl.simplified.opds.core.OPDSNavigationFeed;
import org.nypl.simplified.opds.core.OPDSNavigationFeedEntry;
import org.nypl.simplified.opds.tests.utilities.TestUtilities;
import org.w3c.dom.DOMException;

import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.PartialProcedureType;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;

public final class OPDSFeedParserContract implements
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
      URI.create("http://library-simplified.herokuapp.com/feed/Fiction");
    final OPDSFeedParserType p = OPDSFeedParser.newParser();
    final InputStream d =
      OPDSFeedParserContract.getResource("acquisition-fiction-0.xml");
    final OPDSAcquisitionFeed f = (OPDSAcquisitionFeed) p.parse(uri, d);
    d.close();

    TestUtilities.assertEquals(
      "http://library-simplified.herokuapp.com/feed/Fiction",
      f.getFeedID());
    TestUtilities.assertEquals("Fiction: featured", f.getFeedTitle());
    TestUtilities.assertEquals(20, f.getFeedEntries().size());
    final Calendar u = f.getFeedUpdated();

    final Set<String> ids = new HashSet<String>();
    final Set<String> titles = new HashSet<String>();

    for (final OPDSAcquisitionFeedEntry e : f.getFeedEntries()) {
      final String e_id = e.getID();
      final String e_title = e.getTitle();
      final Calendar e_u = e.getUpdated();
      final String e_subtitle = e.getSubtitle();
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
      System.out.println("subtitle: " + e_subtitle);
      System.out.println("update: " + e_u);
      System.out.println("thumbnail: " + e_thumb);
      System.out.println("cover: " + e_cover);

      TestUtilities.assertGreater(e_authors.size(), 0);
      TestUtilities.assertGreater(e_acq.size(), 0);
      TestUtilities.assertEquals(u, e.getUpdated());

      if (ids.contains(e_id)) {
        throw new AssertionError("Duplicate ID: " + e_id);
      }
      ids.add(e_id);

      if (titles.contains(e_title)) {
        throw new AssertionError("Duplicate title: " + e_title);
      }
      titles.add(e_title);

      System.out.println("--");
    }
  }

  @Override public void testDOMException()
    throws Exception
  {
    final URI uri =
      URI.create("http://library-simplified.herokuapp.com/feed/Fiction");

    TestUtilities.expectException(
      OPDSFeedParseException.class,
      new PartialProcedureType<Unit, Exception>() {
        @Override public void call(
          final Unit x)
          throws Exception
        {
          final OPDSFeedParserType p = OPDSFeedParser.newParser();
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

  @Override public void testNavigationFeed0()
    throws Exception
  {
    final URI uri =
      URI.create("http://library-simplified.herokuapp.com/feed/Fiction");

    final OPDSFeedParserType p = OPDSFeedParser.newParser();
    final InputStream d =
      OPDSFeedParserContract.getResource("navigation-0.xml");
    final OPDSNavigationFeed f = (OPDSNavigationFeed) p.parse(uri, d);
    d.close();

    TestUtilities.assertEquals(
      "http://library-simplified.herokuapp.com/lanes/",
      f.getFeedID());
    TestUtilities.assertEquals("Navigation feed", f.getFeedTitle());
    TestUtilities.assertEquals(28, f.getFeedEntries().size());

    final Set<String> ids = new HashSet<String>();
    final Set<String> titles = new HashSet<String>();
    final Set<URI> targets = new HashSet<URI>();

    final Calendar u = f.getFeedUpdated();
    for (final OPDSNavigationFeedEntry e : f.getFeedEntries()) {
      final String e_id = e.getID();
      final String e_title = e.getTitle();
      final Calendar e_u = e.getUpdated();
      final URI e_target = e.getTargetURI();
      final OptionType<URI> e_featured = e.getFeaturedURI();

      System.out.println("id: " + e_id);
      System.out.println("title: " + e_title);
      System.out.println("update: " + e_u);
      System.out.println("target: " + e_target);
      System.out.println("featured: " + e_featured);

      TestUtilities.assertEquals(u, e.getUpdated());

      if (targets.contains(e_target)) {
        throw new AssertionError("Duplicate target: " + e_target);
      }
      targets.add(e_target);

      if (ids.contains(e_id)) {
        throw new AssertionError("Duplicate ID: " + e_id);
      }
      ids.add(e_id);

      if (titles.contains(e_title)) {
        throw new AssertionError("Duplicate title: " + e_title);
      }
      titles.add(e_title);

      System.out.println("--");
    }
  }

  @Override public void testNavigationFeedBadEntryFeaturedLinkWithoutHref()
    throws Exception
  {
    final URI uri =
      URI.create("http://library-simplified.herokuapp.com/feed/Fiction");

    TestUtilities.expectException(
      OPDSFeedParseException.class,
      new PartialProcedureType<Unit, Exception>() {
        @Override public void call(
          final Unit x)
          throws Exception
        {
          final OPDSFeedParserType p = OPDSFeedParser.newParser();
          final InputStream d =
            OPDSFeedParserContract
              .getResource("navigation-bad-entry-featured-link-without-href.xml");
          p.parse(uri, d);
        }
      });
  }

  @Override public void testNavigationFeedBadEntryLinkWithoutHref()
    throws Exception
  {
    final URI uri =
      URI.create("http://library-simplified.herokuapp.com/feed/Fiction");

    TestUtilities.expectException(
      OPDSFeedParseException.class,
      new PartialProcedureType<Unit, Exception>() {
        @Override public void call(
          final Unit x)
          throws Exception
        {
          final OPDSFeedParserType p = OPDSFeedParser.newParser();
          final InputStream d =
            OPDSFeedParserContract
              .getResource("navigation-bad-entry-link-without-href.xml");
          p.parse(uri, d);
        }
      });
  }

  @Override public void testNavigationFeedBadEntryNoLinks()
    throws Exception
  {
    final URI uri =
      URI.create("http://library-simplified.herokuapp.com/feed/Fiction");

    TestUtilities.expectException(
      OPDSFeedParseException.class,
      new PartialProcedureType<Unit, Exception>() {
        @Override public void call(
          final Unit x)
          throws Exception
        {
          final OPDSFeedParserType p = OPDSFeedParser.newParser();
          final InputStream d =
            OPDSFeedParserContract
              .getResource("navigation-bad-entry-no-links.xml");
          p.parse(uri, d);
        }
      });
  }

  @Override public void testNavigationFeedBadEntrySubsectionLinkWithoutHref()
    throws Exception
  {
    final URI uri =
      URI.create("http://library-simplified.herokuapp.com/feed/Fiction");

    TestUtilities.expectException(
      OPDSFeedParseException.class,
      new PartialProcedureType<Unit, Exception>() {
        @Override public void call(
          final Unit x)
          throws Exception
        {
          final OPDSFeedParserType p = OPDSFeedParser.newParser();
          final InputStream d =
            OPDSFeedParserContract
              .getResource("navigation-bad-entry-subsection-link-without-href.xml");
          p.parse(uri, d);
        }
      });
  }

  @Override public void testNotXMLException()
    throws Exception
  {
    final URI uri =
      URI.create("http://library-simplified.herokuapp.com/feed/Fiction");

    TestUtilities.expectException(
      OPDSFeedParseException.class,
      new PartialProcedureType<Unit, Exception>() {
        @Override public void call(
          final Unit x)
          throws Exception
        {
          final OPDSFeedParserType p = OPDSFeedParser.newParser();
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
      OPDSFeedParseException.class,
      new PartialProcedureType<Unit, Exception>() {
        @Override public void call(
          final Unit x)
          throws Exception
        {
          final OPDSFeedParserType p = OPDSFeedParser.newParser();
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
      OPDSFeedParseException.class,
      new PartialProcedureType<Unit, Exception>() {
        @Override public void call(
          final Unit x)
          throws Exception
        {
          final OPDSFeedParserType p = OPDSFeedParser.newParser();
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
}
