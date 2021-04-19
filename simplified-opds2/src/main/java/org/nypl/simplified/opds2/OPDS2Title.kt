package org.nypl.simplified.opds2

data class OPDS2Title(
  val title: String = "",
  val byLanguage: Map<String, String> = mapOf()
) : OPDS2ElementType {

  fun ofLanguageOrDefault(language: String): String =
    if (this.byLanguage.containsKey(language)) {
      this.byLanguage[language]!!
    } else {
      this.title
    }
}
