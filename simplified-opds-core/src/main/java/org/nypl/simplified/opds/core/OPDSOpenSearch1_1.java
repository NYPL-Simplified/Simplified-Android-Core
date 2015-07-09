package org.nypl.simplified.opds.core;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;

import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;

/**
 * The type of Open Search 1.1 descriptions.
 *
 * @see <a href="http://www.opensearch.org/Specifications/OpenSearch/1.1">OpenSearch 1.1</a>
 */

public final class OPDSOpenSearch1_1
{
  private final String template;

  public OPDSOpenSearch1_1(
    final String in_template)
  {
    this.template = NullCheck.notNull(in_template);
  }

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
