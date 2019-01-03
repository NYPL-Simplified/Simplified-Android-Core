package org.nypl.simplified.books.accounts;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.io7m.jfunctional.FunctionType;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.PartialFunctionType;
import com.io7m.jfunctional.ProcedureType;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;

import org.nypl.drm.core.AdobeDeviceID;
import org.nypl.drm.core.AdobeUserID;
import org.nypl.drm.core.AdobeVendorID;
import org.nypl.simplified.http.core.HTTPOAuthToken;
import org.nypl.simplified.json.core.JSONParseException;
import org.nypl.simplified.json.core.JSONParserUtilities;
import org.nypl.simplified.json.core.JSONSerializerUtilities;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Functions for serializing/deserializing account credentials.
 */

public final class AccountAuthenticationCredentialsJSON {
  private AccountAuthenticationCredentialsJSON() {
    throw new UnreachableCodeException();
  }

  /**
   * Serialize the given credentials to a JSON object, and serialize that to a
   * UTF-8 string.
   *
   * @param credentials The credentials.
   * @return A string of JSON
   * @throws IOException On I/O or serialization errors
   */

  public static String serializeToText(
      final AccountAuthenticationCredentials credentials)
      throws IOException {
    NullCheck.notNull(credentials, "Credentials");

    final ObjectNode jo = AccountAuthenticationCredentialsJSON.serializeToJSON(credentials);
    final ByteArrayOutputStream bao = new ByteArrayOutputStream(1024);
    JSONSerializerUtilities.serialize(jo, bao);
    return bao.toString("UTF-8");
  }

  /**
   * Serialize the given credentials to a JSON object.
   *
   * @param credentials The credentials.
   * @return A JSON object
   */

  public static ObjectNode serializeToJSON(
      final AccountAuthenticationCredentials credentials) {
    NullCheck.notNull(credentials, "Credentials");

    final ObjectMapper jom = new ObjectMapper();
    final ObjectNode jo = jom.createObjectNode();
    jo.put("username", credentials.barcode().value());
    jo.put("password", credentials.pin().value());

    credentials.oAuthToken().map_(
        new ProcedureType<HTTPOAuthToken>() {
          @Override
          public void call(final HTTPOAuthToken x) {
            jo.put("oauth_token", x.value());
          }
        });

    credentials.adobeCredentials().map_(
        new ProcedureType<AccountAuthenticationAdobePreActivationCredentials>() {
          @Override
          public void call(AccountAuthenticationAdobePreActivationCredentials creds) {
            final ObjectNode adobe_pre_jo = jom.createObjectNode();

            adobe_pre_jo.put("client_token", creds.clientToken().tokenRaw());
            adobe_pre_jo.put("device_manager_uri", creds.deviceManagerURI().toString());
            adobe_pre_jo.put("vendor_id", creds.vendorID().getValue());

            creds.postActivationCredentials().map_(
                new ProcedureType<AccountAuthenticationAdobePostActivationCredentials>() {
                  @Override
                  public void call(AccountAuthenticationAdobePostActivationCredentials post_creds) {
                    final ObjectNode adobe_post_jo = jom.createObjectNode();
                    adobe_post_jo.put("device_id", post_creds.deviceID().getValue());
                    adobe_post_jo.put("user_id", post_creds.userID().getValue());
                    adobe_pre_jo.set("activation", adobe_post_jo);
                  }
                });

            jo.set("adobe_credentials", adobe_pre_jo);
          }
        });

    credentials.authenticationProvider().map_(new ProcedureType<AccountAuthenticationProvider>() {
      @Override
      public void call(AccountAuthenticationProvider x) {
        jo.put("auth_provider", x.value());
      }
    });

    credentials.patron().map_(new ProcedureType<AccountPatron>() {
      @Override
      public void call(AccountPatron x) {
        jo.put("patron", x.value());
      }
    });

    return jo;
  }

  /**
   * Deserialize the given text, which is assumed to be a JSON object
   * representing account credentials.
   *
   * @param text The credentials text.
   * @return Account credentials
   * @throws IOException On I/O or serialization errors
   */

  public static AccountAuthenticationCredentials deserializeFromText(final String text)
      throws IOException {
    NullCheck.notNull(text);
    final ObjectMapper jom = new ObjectMapper();
    return AccountAuthenticationCredentialsJSON.deserializeFromJSON(jom.readTree(text));
  }

  /**
   * Deserialize the given JSON node, which is assumed to be a JSON object
   * representing account credentials.
   *
   * @param node Credentials as a JSON node.
   * @return Account credentials
   * @throws JSONParseException On parse errors
   */

  public static AccountAuthenticationCredentials deserializeFromJSON(
      final JsonNode node)
      throws JSONParseException {
    NullCheck.notNull(node, "Node");

    final ObjectNode obj = JSONParserUtilities.checkObject(null, node);

    final AccountBarcode user =
        AccountBarcode.create(JSONParserUtilities.getString(obj, "username"));
    final AccountPIN pass =
        AccountPIN.create(JSONParserUtilities.getString(obj, "password"));
    final AccountAuthenticationCredentials.Builder builder =
        AccountAuthenticationCredentials.builder(pass, user);

    builder.setPatron(
        JSONParserUtilities.getStringOptional(obj, "patron")
            .map(new FunctionType<String, AccountPatron>() {
              @Override
              public AccountPatron call(String x) {
                return AccountPatron.create(x);
              }
            }));

    builder.setAuthenticationProvider(
        JSONParserUtilities.getStringOptional(obj, "auth_provider")
            .map(new FunctionType<String, AccountAuthenticationProvider>() {
              @Override
              public AccountAuthenticationProvider call(String x) {
                return AccountAuthenticationProvider.create(x);
              }
            }));

    builder.setOAuthToken(
        JSONParserUtilities.getStringOptional(obj, "oauth_token")
            .map(new FunctionType<String, HTTPOAuthToken>() {
              @Override
              public HTTPOAuthToken call(String x) {
                return HTTPOAuthToken.create(x);
              }
            }));

    builder.setAdobeCredentials(
        JSONParserUtilities.getObjectOptional(obj, "adobe_credentials")
            .mapPartial(new PartialFunctionType<ObjectNode, AccountAuthenticationAdobePreActivationCredentials, JSONParseException>() {
              @Override
              public AccountAuthenticationAdobePreActivationCredentials call(
                  final ObjectNode jo_creds)
                  throws JSONParseException {

                final OptionType<AccountAuthenticationAdobePostActivationCredentials> creds_post =
                    JSONParserUtilities.getObjectOptional(jo_creds, "activation")
                        .mapPartial(new PartialFunctionType<ObjectNode, AccountAuthenticationAdobePostActivationCredentials, JSONParseException>() {
                          @Override
                          public AccountAuthenticationAdobePostActivationCredentials call(ObjectNode act)
                              throws JSONParseException {
                            return AccountAuthenticationAdobePostActivationCredentials.create(
                                new AdobeDeviceID(JSONParserUtilities.getString(act, "device_id")),
                                new AdobeUserID(JSONParserUtilities.getString(act, "user_id")));
                          }
                        });

                return AccountAuthenticationAdobePreActivationCredentials.create(
                    new AdobeVendorID(JSONParserUtilities.getString(jo_creds, "vendor_id")),
                    AccountAuthenticationAdobeClientToken.create(JSONParserUtilities.getString(jo_creds, "client_token")),
                    JSONParserUtilities.getURI(jo_creds, "device_manager_uri"),
                    creds_post);
              }
            }));

    return builder.build();
  }
}
