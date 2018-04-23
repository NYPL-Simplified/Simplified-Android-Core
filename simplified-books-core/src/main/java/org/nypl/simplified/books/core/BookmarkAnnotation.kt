package org.nypl.simplified.books.core

import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable

data class SelectorNode(val type: String,
                        val value: String) : Serializable

data class TargetNode(val source: String,
                      val selector: SelectorNode) : Serializable

data class BodyNode(@JsonProperty("http://librarysimplified.org/terms/time") val timestamp: String,
                    @JsonProperty("http://librarysimplified.org/terms/device") val device: String,
                    @JsonProperty("http://librarysimplified.org/terms/chapter") val chapterTitle: String?,
                    @JsonProperty("http://librarysimplified.org/terms/progressWithinChapter") val chapterProgress: Float?,
                    @JsonProperty("http://librarysimplified.org/terms/progressWithinBook") val bookProgress: Float?) : Serializable

data class BookmarkAnnotation(@JsonProperty("@context") val context: String?,
                              val body: BodyNode,
                              val id: String?,
                              val type: String,
                              val motivation: String,
                              val target: TargetNode) : Serializable

data class FirstNode(val items: List<BookmarkAnnotation>,
                     val type: String,
                     val id: String)

data class AnnotationResponse(@JsonProperty("@context") val context: List<String>,
                              val total: Int,
                              val type: List<String>,
                              val id: String,
                              val first: FirstNode)
