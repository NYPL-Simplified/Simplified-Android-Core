package org.nypl.simplified.books.book_database;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.io7m.jnull.NullCheck;

import org.nypl.drm.core.AdobeAdeptLoan;
import org.nypl.drm.core.AdobeLoanID;
import org.nypl.simplified.json.core.JSONParseException;
import org.nypl.simplified.json.core.JSONParserUtilities;
import org.nypl.simplified.json.core.JSONSerializerUtilities;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Functions to serialize/deserialize Adobe Adept loan information.
 */

public final class BookAdeptLoanJSON {

  public static ObjectNode serializeToNode(final AdobeAdeptLoan loan) {
    NullCheck.notNull(loan, "Loan");
    final ObjectMapper jom = new ObjectMapper();
    final ObjectNode o = jom.createObjectNode();
    o.put("loan-id", loan.getID().getValue());
    o.put("returnable", loan.isReturnable());
    return o;
  }

  public static String serializeToString(final AdobeAdeptLoan loan) throws IOException {
    NullCheck.notNull(loan, "Loan");
    final ByteArrayOutputStream bao = new ByteArrayOutputStream();
    JSONSerializerUtilities.serialize(serializeToNode(loan), bao);
    return bao.toString("UTF-8");
  }

  public static AdobeAdeptLoan deserializeFromNode(
    final ObjectNode node,
    final byte[] rights)
    throws JSONParseException {

    NullCheck.notNull(node, "Node");
    NullCheck.notNull(rights, "Rights");
    return new AdobeAdeptLoan(
      new AdobeLoanID(JSONParserUtilities.getString(node, "loan-id")),
      ByteBuffer.wrap(rights),
      JSONParserUtilities.getBoolean(node, "returnable"));
  }
}
