package org.nypl.simplified.opds.core;

import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;

//@formatter:off

/**
 * The type of Open Search 1.1 descriptions.
 *
 * @see <a href="http://www.opensearch.org/Specifications/OpenSearch/1.1">
 *   OpenSearch 1.1</a>
 */

//@formatter:on

public final class OPDSOpenSearch1_1
{
  private final String template;

  /**
   * Construct a search template.
   *
   * @param in_template The template
   */

  public OPDSOpenSearch1_1(
    final String in_template)
  {
    this.template = NullCheck.notNull(in_template);
  }

  /**
   * @param terms The search terms
   *
   * @return A query URI for searching with the given search terms
   */

  public URI getQueryURIForTerms(
    final String terms)
  {
    NullCheck.notNull(terms);
    try {
      final String encoded = URLEncoder.encode(terms, "UTF-8");
      final String raw = this.template.replace("{searchTerms}", encoded);
      return NullCheck.notNull(new URI(raw));
    } catch (final UnsupportedEncodingException e) {
      throw new UnreachableCodeException(e);
    } catch (final URISyntaxException e) {
      throw new UnreachableCodeException(e);
    }
  }
}
