package org.nypl.simplified.tests.http;

import org.junit.Assert;
import org.junit.Test;
import org.nypl.simplified.http.core.URIQueryBuilder;

import java.net.URI;
import java.util.SortedMap;
import java.util.TreeMap;

public abstract class URIQueryBuilderContract {

  @Test
  public void testQueryEncode_0() {
    final URI expected =
        URI.create("http://circulation.alpha.librarysimplified.org/feed/Biography%20%26%20Memoir");

    final SortedMap<String, String> parameters =
        new TreeMap<String, String>();
    final URI result = URIQueryBuilder.encodeQuery(expected, parameters);
    Assert.assertEquals(result, expected);
  }

  @Test
  public void testQueryEncode_1()
      throws Exception {
    final URI base =
        URI.create("http://circulation.alpha.librarysimplified.org/feed/Biography%20%26%20Memoir");
    final URI expected =
        URI
            .create("http://circulation.alpha.librarysimplified.org/feed/Biography%20%26%20Memoir?x=a+b");

    final SortedMap<String, String> parameters =
        new TreeMap<String, String>();
    parameters.put("x", "a b");

    final URI result = URIQueryBuilder.encodeQuery(base, parameters);
    Assert.assertEquals(result, expected);
  }

  @Test
  public void testQueryEncode_2()
      throws Exception {
    final URI base =
        URI.create("http://circulation.alpha.librarysimplified.org/feed/Biography%20%26%20Memoir");
    final URI expected =
        URI.create("http://circulation.alpha.librarysimplified.org/feed/Biography%20%26%20Memoir?x=a+b&y=a%26b");

    final SortedMap<String, String> parameters =
        new TreeMap<String, String>();
    parameters.put("x", "a b");
    parameters.put("y", "a&b");

    final URI result = URIQueryBuilder.encodeQuery(base, parameters);
    Assert.assertEquals(result, expected);
  }
}
