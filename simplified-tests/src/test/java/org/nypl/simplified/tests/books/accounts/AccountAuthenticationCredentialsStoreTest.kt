package org.nypl.simplified.tests.books.accounts

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.api.AccountPassword
import org.nypl.simplified.accounts.api.AccountUsername
import org.nypl.simplified.accounts.database.AccountAuthenticationCredentialsStore
import org.nypl.simplified.accounts.json.AccountAuthenticationCredentialsJSON
import org.nypl.simplified.json.core.JSONParseException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.util.UUID

class AccountAuthenticationCredentialsStoreTest {

  private val logger: Logger =
    LoggerFactory.getLogger(AccountAuthenticationCredentialsStoreTest::class.java)

  private lateinit var fileTemp: File
  private lateinit var file: File

  @BeforeEach
  fun testSetup() {
    this.file =
      File.createTempFile("test-simplified-auth-credentials-store", ".json")
    this.fileTemp =
      File(file.toString() + ".tmp")

    this.logger.debug("file:     {}", this.file)
    this.logger.debug("fileTemp: {}", this.fileTemp)
  }

  @Test
  fun testLoadEmpty() {
    val store =
      AccountAuthenticationCredentialsStore.open(this.file, this.fileTemp)

    Assertions.assertEquals(0, store.size())
  }

  @Test
  fun testLoadAfterSave() {
    val store0 =
      AccountAuthenticationCredentialsStore.open(this.file, this.fileTemp)

    val accountID = AccountID.generate()
    val credentials =
      AccountAuthenticationCredentials.Basic(
        userName = AccountUsername("abcd"),
        password = AccountPassword("1234"),
        adobeCredentials = null,
        authenticationDescription = null,
        annotationsURI = URI("https://www.example.com")
      )

    store0.put(accountID, credentials)
    Assertions.assertEquals(credentials, store0.get(accountID))

    val store1 =
      AccountAuthenticationCredentialsStore.open(this.file, this.fileTemp)

    Assertions.assertEquals(credentials, store1.get(accountID))
  }

  @Test
  fun testLoadNoVersion() {
    val mapper = ObjectMapper()
    val obj = mapper.createObjectNode()
    obj.set<ObjectNode>("credentials", mapper.createObjectNode())

    FileOutputStream(this.file)
      .use { stream ->
        stream.write(mapper.writeValueAsBytes(obj))
        stream.flush()
      }

    val store =
      AccountAuthenticationCredentialsStore.open(this.file, this.fileTemp)

    Assertions.assertEquals(0, store.size())
  }

  @Test
  fun testLoadUnsupportedVersion() {
    val mapper = ObjectMapper()
    val obj = mapper.createObjectNode()
    obj.put("@version", 20000101)
    obj.set<ObjectNode>("credentials", mapper.createObjectNode())

    FileOutputStream(this.file)
      .use { stream ->
        stream.write(mapper.writeValueAsBytes(obj))
        stream.flush()
      }

    Assertions.assertThrows(JSONParseException::class.java) {
      AccountAuthenticationCredentialsStore.open(this.file, this.fileTemp)
    }
  }

  @Test
  fun testLoadCorruptedCredentials() {
    val mapper = ObjectMapper()
    val obj = mapper.createObjectNode()
    val creds = mapper.createObjectNode()
    obj.set<ObjectNode>("credentials", creds)

    /*
     * Invalid credential value.
     */

    val cred0 = mapper.createObjectNode()
    creds.set<ObjectNode>("347ae11b-cb5c-4084-8954-8629fd971bda", cred0)

    /*
     * Invalid credential value.
     */

    val cred1 =
      AccountAuthenticationCredentialsJSON.serializeToJSON(
        AccountAuthenticationCredentials.Basic(
          userName = AccountUsername("abcd"),
          password = AccountPassword("1234"),
          adobeCredentials = null,
          authenticationDescription = null,
          annotationsURI = URI("https://www.example.com")
        )
      )
    cred1.remove("username")
    creds.set<ObjectNode>("8e058c17-6c59-490c-92c5-d950463c8632", cred1)

    /*
     * Valid credential value but invalid UUID.
     */

    val cred2 =
      AccountAuthenticationCredentialsJSON.serializeToJSON(
        AccountAuthenticationCredentials.Basic(
          userName = AccountUsername("abcd"),
          password = AccountPassword("1234"),
          adobeCredentials = null,
          authenticationDescription = null,
          annotationsURI = URI("https://www.example.com")
        )
      )
    creds.set<ObjectNode>("not a uuid", cred2)

    /*
     * Valid credential values.
     */

    val cred3 =
      AccountAuthenticationCredentialsJSON.serializeToJSON(
        AccountAuthenticationCredentials.Basic(
          userName = AccountUsername("abcd"),
          password = AccountPassword("1234"),
          adobeCredentials = null,
          authenticationDescription = null,
          annotationsURI = URI("https://www.example.com")
        )
      )
    creds.set<ObjectNode>("37452e48-2235-4098-ad67-e72bce45ccb6", cred3)

    FileOutputStream(this.file)
      .use { stream ->
        stream.write(mapper.writeValueAsBytes(obj))
        stream.flush()
      }

    val store =
      AccountAuthenticationCredentialsStore.open(this.file, this.fileTemp)

    Assertions.assertEquals(1, store.size())
    Assertions.assertNotNull(store.get(AccountID(UUID.fromString("37452e48-2235-4098-ad67-e72bce45ccb6"))))
  }

  @Test
  fun testPutRemove() {
    val store =
      AccountAuthenticationCredentialsStore.open(this.file, this.fileTemp)

    val accountID = AccountID.generate()
    val credentials =
      AccountAuthenticationCredentials.Basic(
        userName = AccountUsername("abcd"),
        password = AccountPassword("1234"),
        adobeCredentials = null,
        authenticationDescription = null,
        annotationsURI = URI("https://www.example.com")
      )

    store.put(accountID, credentials)
    Assertions.assertEquals(credentials, store.get(accountID))
    Assertions.assertEquals(1, store.size())
    store.delete(accountID)
    Assertions.assertEquals(null, store.get(accountID))
    Assertions.assertEquals(0, store.size())
  }
}
