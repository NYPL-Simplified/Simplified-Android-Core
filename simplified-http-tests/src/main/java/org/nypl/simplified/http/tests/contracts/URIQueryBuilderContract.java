package org.nypl.simplified.http.tests.contracts;

import java.net.URI;
import java.util.SortedMap;
import java.util.TreeMap;

import org.nypl.simplified.http.core.URIQueryBuilder;
import org.nypl.simplified.test.utilities.TestUtilities;

public final class URIQueryBuilderContract implements
  URIQueryBuilderContractType
{
  @Override public void testQueryEncode_0()
  {
    final URI expected =
      URI
        .create("http://circulation.alpha.librarysimplified.org/feed/Biography%20%26%20Memoir");

    final SortedMap<String, String> parameters =
      new TreeMap<String, String>();
    final URI result = URIQueryBuilder.encodeQuery(expected, parameters);
    TestUtilities.assertEquals(result, expected);
  }

  @Override public void testQueryEncode_1()
    throws Exception
  {
    final URI base =
      URI
        .create("http://circulation.alpha.librarysimplified.org/feed/Biography%20%26%20Memoir");
    final URI expected =
      URI
        .create("http://circulation.alpha.librarysimplified.org/feed/Biography%20%26%20Memoir?x=a+b");

    final SortedMap<String, String> parameters =
      new TreeMap<String, String>();
    parameters.put("x", "a b");

    final URI result = URIQueryBuilder.encodeQuery(base, parameters);
    TestUtilities.assertEquals(result, expected);
  }

  @Override public void testQueryEncode_2()
    throws Exception
  {
    final URI base =
      URI
        .create("http://circulation.alpha.librarysimplified.org/feed/Biography%20%26%20Memoir");
    final URI expected =
      URI
        .create("http://circulation.alpha.librarysimplified.org/feed/Biography%20%26%20Memoir?x=a+b&y=a%26b");

    final SortedMap<String, String> parameters =
      new TreeMap<String, String>();
    parameters.put("x", "a b");
    parameters.put("y", "a&b");

    final URI result = URIQueryBuilder.encodeQuery(base, parameters);
    TestUtilities.assertEquals(result, expected);
  }
}
