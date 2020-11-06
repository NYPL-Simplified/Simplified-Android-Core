package org.librarysimplified.documents.internal

import android.content.res.AssetManager
import org.librarysimplified.documents.DocumentConfiguration
import org.librarysimplified.documents.DocumentConfigurationServiceType
import org.librarysimplified.documents.DocumentStoreType
import org.librarysimplified.documents.DocumentType
import org.librarysimplified.documents.EULAType
import org.librarysimplified.http.api.LSHTTPClientType
import java.io.File

internal class DocumentStore private constructor(
  override val about: DocumentType?,
  override val acknowledgements: DocumentType?,
  override val eula: EULAType?,
  override val licenses: DocumentType?,
  override val privacyPolicy: DocumentType?
) : DocumentStoreType {

  companion object {
    fun create(
      assetManager: AssetManager,
      http: LSHTTPClientType,
      baseDirectory: File,
      configuration: DocumentConfigurationServiceType
    ): DocumentStoreType {
      val about =
        this.documentForMaybe(
          assetManager = assetManager,
          http = http,
          baseDirectory = baseDirectory,
          config = configuration.about
        )

      val acknowledgements =
        this.documentForMaybe(
          assetManager = assetManager,
          http = http,
          baseDirectory = baseDirectory,
          config = configuration.acknowledgements
        )

      val eula =
        this.eulaForMaybe(
          assetManager = assetManager,
          http = http,
          baseDirectory = baseDirectory,
          config = configuration.eula
        )

      val licenses =
        this.documentForMaybe(
          assetManager = assetManager,
          http = http,
          baseDirectory = baseDirectory,
          config = configuration.licenses
        )

      val privacyPolicy =
        this.documentForMaybe(
          assetManager = assetManager,
          http = http,
          baseDirectory = baseDirectory,
          config = configuration.privacyPolicy
        )

      return DocumentStore(
        about = about,
        acknowledgements = acknowledgements,
        eula = eula,
        licenses = licenses,
        privacyPolicy = privacyPolicy
      )
    }

    private fun eulaForMaybe(
      assetManager: AssetManager,
      http: LSHTTPClientType,
      baseDirectory: File,
      config: DocumentConfiguration?
    ): EULAType? {
      return config?.let {
        this.eulaFor(assetManager, http, baseDirectory, config)
      }
    }

    private fun eulaFor(
      assetManager: AssetManager,
      http: LSHTTPClientType,
      baseDirectory: File,
      config: DocumentConfiguration
    ): EULAType {
      return EULA(
        http = http,
        initialStreams = {
          assetManager.open(config.name)
        },
        file = File(baseDirectory, config.name),
        fileTmp = File(baseDirectory, config.name + ".tmp"),
        fileAgreed = File(baseDirectory, "eula_agreed.dat"),
        remoteURL = config.remoteURI.toURL()
      )
    }

    private fun documentForMaybe(
      assetManager: AssetManager,
      http: LSHTTPClientType,
      baseDirectory: File,
      config: DocumentConfiguration?
    ): DocumentType? {
      return config?.let {
        this.documentFor(assetManager, http, baseDirectory, config)
      }
    }

    private fun documentFor(
      assetManager: AssetManager,
      http: LSHTTPClientType,
      baseDirectory: File,
      config: DocumentConfiguration
    ): DocumentType {
      return PlainDocument(
        http = http,
        initialStreams = {
          assetManager.open(config.name)
        },
        file = File(baseDirectory, config.name),
        fileTmp = File(baseDirectory, config.name + ".tmp"),
        remoteURL = config.remoteURI.toURL()
      )
    }
  }
}
