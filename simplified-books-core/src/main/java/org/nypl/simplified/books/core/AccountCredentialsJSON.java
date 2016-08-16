package org.nypl.simplified.books.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.io7m.jfunctional.FunctionType;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.ProcedureType;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;

import org.nypl.drm.core.AdobeDeviceID;
import org.nypl.drm.core.AdobeUserID;
import org.nypl.drm.core.AdobeVendorID;
import org.nypl.simplified.json.core.JSONParseException;
import org.nypl.simplified.json.core.JSONParserUtilities;
import org.nypl.simplified.json.core.JSONSerializerUtilities;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Functions for serializing/deserializing account credentials.
 */

public final class AccountCredentialsJSON
{
  private AccountCredentialsJSON()
  {
    throw new UnreachableCodeException();
  }

  /**
   * Serialize the given credentials to a JSON object, and serialize that to a
   * UTF-8 string.
   *
   * @param credentials The credentials.
   *
   * @return A string of JSON
   *
   * @throws IOException On I/O or serialization errors
   */

  public static String serializeToText(final AccountCredentials credentials)
    throws IOException
  {
    NullCheck.notNull(credentials);

    final ObjectNode jo = AccountCredentialsJSON.serializeToJSON(credentials);
    final ByteArrayOutputStream bao = new ByteArrayOutputStream(1024);
    JSONSerializerUtilities.serialize(jo, bao);
    return bao.toString("UTF-8");
  }

  /**
   * Serialize the given credentials to a JSON object.
   *
   * @param credentials The credentials.
   *
   * @return A JSON object
   */

  public static ObjectNode serializeToJSON(final AccountCredentials credentials)
  {
    NullCheck.notNull(credentials);

    final ObjectMapper jom = new ObjectMapper();
    final ObjectNode jo = jom.createObjectNode();
    jo.put("username", credentials.getBarcode().toString());
    jo.put("password", credentials.getPin().toString());
    jo.put("provider", credentials.getProvider().toString());
//    jo.put("user_id", credentials.getAdobeUserID().toString());
//    jo.put("device_id", credentials.getAdobeDeviceID().toString());
//    jo.put("patron", credentials.getPatron().toString());
//    jo.put("auth_token", credentials.getAuthToken().toString());
//    jo.put("adobe_token", credentials.getAdobeToken().toString());

    credentials.getAdobeVendor().map_(
      new ProcedureType<AdobeVendorID>()
      {
        @Override public void call(final AdobeVendorID x)
        {
          jo.put("adobe-vendor", x.toString());
        }
      });

    return jo;
  }

  /**
   * Deserialize the given text, which is assumed to be a JSON object
   * representing account credentials.
   *
   * @param text The credentials text.
   *
   * @return Account credentials
   *
   * @throws IOException On I/O or serialization errors
   */

  public static AccountCredentials deserializeFromText(final String text)
    throws IOException
  {
    NullCheck.notNull(text);
    final ObjectMapper jom = new ObjectMapper();
    return AccountCredentialsJSON.deserializeFromJSON(jom.readTree(text));
  }

  /**
   * Deserialize the given JSON node, which is assumed to be a JSON object
   * representing account credentials.
   *
   * @param node Credentials as a JSON node.
   *
   * @return Account credentials
   *
   * @throws JSONParseException On parse errors
   */

  public static AccountCredentials deserializeFromJSON(final JsonNode node)
    throws JSONParseException
  {
    NullCheck.notNull(node);
    final ObjectNode obj = JSONParserUtilities.checkObject(null, node);
    final AccountBarcode user =
      new AccountBarcode(JSONParserUtilities.getString(obj, "username"));
    final AccountPIN pass =
      new AccountPIN(JSONParserUtilities.getString(obj, "password"));

    final AccountAuthProvider provider =
      new AccountAuthProvider(JSONParserUtilities.getString(obj, "provider"));

    final OptionType<AccountPatron> patron =
      JSONParserUtilities.getStringOptional(obj, "patron").map(
      new FunctionType<String, AccountPatron>()
      {
        @Override public AccountPatron call(final String x)
        {
          return new AccountPatron(x);
        }
      });
    final OptionType<AccountAuthToken> auth_token =
      JSONParserUtilities.getStringOptional(obj, "auth_token").map(
      new FunctionType<String, AccountAuthToken>()
      {
        @Override public AccountAuthToken call(final String x)
        {
          return new AccountAuthToken(x);
        }
      });
    final OptionType<AccountAdobeToken> adobe_token =
      JSONParserUtilities.getStringOptional(obj, "adobe_token").map(
      new FunctionType<String, AccountAdobeToken>()
      {
        @Override public AccountAdobeToken call(final String x)
        {
          return new AccountAdobeToken(x);
        }
      });

    final OptionType<AdobeUserID> adobe_user =
      JSONParserUtilities.getStringOptional(obj, "user_id").map(
      new FunctionType<String, AdobeUserID>()
      {
        @Override public AdobeUserID call(final String x)
        {
          return new AdobeUserID(x);
        }
      });
    final OptionType<AdobeDeviceID> adobe_device = JSONParserUtilities.getStringOptional(obj, "device_id").map(
      new FunctionType<String, AdobeDeviceID>()
      {
        @Override public AdobeDeviceID call(final String x)
        {
          return new AdobeDeviceID(x);
        }
      });

    final OptionType<AdobeVendorID> vendor =
      JSONParserUtilities.getStringOptional(obj, "adobe-vendor").map(
        new FunctionType<String, AdobeVendorID>()
        {
          @Override public AdobeVendorID call(final String x)
          {
            return new AdobeVendorID(x);
          }
        });



    creds.setAdobeUserID(adobe_user);
    creds.setAdobeDeviceID(adobe_device);
//    creds.setAdobeToken(adobe_token);
//    creds.setAuthToken(auth_token);

    return creds;
  }
}
