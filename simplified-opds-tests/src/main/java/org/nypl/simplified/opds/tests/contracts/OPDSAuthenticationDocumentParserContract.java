package org.nypl.simplified.opds.tests.contracts;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jnull.NullCheck;
import org.nypl.simplified.opds.core.OPDSAuthenticationDocument;
import org.nypl.simplified.opds.core.OPDSAuthenticationDocumentParser;
import org.nypl.simplified.opds.core.OPDSAuthenticationDocumentParserType;
import org.nypl.simplified.opds.core.OPDSLink;
import org.nypl.simplified.test.utilities.TestUtilities;

import java.io.InputStream;
import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Authentication document parser contract.
 */

public final class OPDSAuthenticationDocumentParserContract
  implements OPDSAuthenticationDocumentParserContractType
{
  /**
   * Construct a contract.
   */

  public OPDSAuthenticationDocumentParserContract()
  {

  }

  private InputStream getResource(
    final String name)
    throws Exception
  {
    return NullCheck.notNull(
      this.getClass().getResourceAsStream(name));
  }

  @Override public void testParseSpecific_0()
    throws Exception
  {
    final OPDSAuthenticationDocumentParserType parser = this.getParser();
    final OPDSAuthenticationDocument doc = parser.parseFromStream(
      this.getResource("auth-specific-0.json"));

    final String expected_id = "2ba6a1aa-963b-314a-97d1-bdad73bcd6f1";
    final String expected_title = "Library";
    final OptionType<String> no_text = Option.none();
    final OptionType<String> expected_text = no_text;
    final List<URI> expected_type = new ArrayList<URI>(1);
    expected_type.add(new URI("http://opds-spec.org/auth/basic"));
    final Map<String, String> expected_labels = new HashMap<String, String>(2);
    expected_labels.put("password", "PIN");
    expected_labels.put("login", "Barcode");

    final Map<String, OPDSLink> expected_links =
      new HashMap<String, OPDSLink>(3);

    final OptionType<BigInteger> no_length = Option.none();
    final OptionType<String> type_html = Option.some("text/html");

    final OPDSLink link_copyright = new OPDSLink(
      no_text,
      URI.create("http://www.librarysimplified.org/acknowledgments.html"),
      no_text,
      type_html,
      false,
      no_length);

    final OPDSLink link_privacy = new OPDSLink(
      no_text,
      URI.create("http://www.librarysimplified.org/privacypolicy.html"),
      no_text,
      type_html,
      false,
      no_length);

    final OPDSLink link_terms = new OPDSLink(
      no_text,
      URI.create("http://www.librarysimplified.org/EULA.html"),
      no_text,
      type_html,
      false,
      no_length);

    expected_links.put("copyright", link_copyright);
    expected_links.put("privacy-policy", link_privacy);
    expected_links.put("terms-of-service", link_terms);

    final OPDSAuthenticationDocument expected_doc =
      new OPDSAuthenticationDocument(
        expected_id,
        expected_type,
        expected_title,
        expected_text,
        expected_links,
        expected_labels);

    TestUtilities.assertEquals(expected_doc, doc);
  }

  private OPDSAuthenticationDocumentParserType getParser()
  {
    return OPDSAuthenticationDocumentParser.get();
  }
}
