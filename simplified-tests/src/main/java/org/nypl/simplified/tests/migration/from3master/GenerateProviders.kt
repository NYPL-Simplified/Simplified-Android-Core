package org.nypl.simplified.tests.migration.from3master

import org.nypl.simplified.accounts.json.AccountProvidersJSON
import java.io.FileInputStream
import java.io.FileOutputStream

class GenerateProviders {
  companion object {

    @JvmStatic
    fun main(args: Array<String>) {
      val providers =
        FileInputStream(args[0]).use { stream ->
          AccountProvidersJSON.deserializeCollectionFromStream(stream)
        }

      FileOutputStream(args[1]).use { stream ->
        stream.bufferedWriter().use { writer ->
          val sorted =
            providers.values.sortedBy { provider -> provider.idNumeric }

          for (provider in sorted) {
            writer.append("Pair(${provider.idNumeric}, URI(\"${provider.id}\")),")
            writer.newLine()
          }
          writer.flush()
        }
      }
    }
  }
}
