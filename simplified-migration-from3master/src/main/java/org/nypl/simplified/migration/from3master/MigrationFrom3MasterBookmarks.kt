package org.nypl.simplified.migration.from3master

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import java.io.Serializable

@JsonDeserialize
class SelectorNode(
  @JvmField
  @JsonProperty("type")
  val type: String,

  @JvmField
  @JsonProperty("value")
  val value: String)
  : Serializable

@JsonDeserialize
class TargetNode(
  @JvmField
  @JsonProperty("source")
  val source: String,

  @JvmField
  @JsonProperty("selector")
  val selector: SelectorNode)
  : Serializable

@JsonDeserialize
class BodyNode(
  @JvmField
  @JsonProperty("http://librarysimplified.org/terms/time")
  val timestamp: String,

  @JvmField
  @JsonProperty("http://librarysimplified.org/terms/device")
  val device: String,

  @JvmField
  @JsonProperty("http://librarysimplified.org/terms/chapter")
  val chapterTitle: String?,

  @JvmField
  @JsonProperty("http://librarysimplified.org/terms/progressWithinChapter")
  val chapterProgress: Float?,

  @JvmField
  @JsonProperty("http://librarysimplified.org/terms/progressWithinBook")
  val bookProgress: Float?)
  : Serializable

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_EMPTY)
class BookmarkAnnotation(
  @JvmField
  @JsonProperty("@context")
  val context: String?,

  @JvmField
  @JsonProperty("body")
  val body: BodyNode,

  @JvmField
  @JsonProperty("id")
  val id: String?,

  @JvmField
  @JsonProperty("type")
  val type: String,

  @JvmField
  @JsonProperty("motivation")
  val motivation: String,

  @JvmField
  @JsonProperty("target")
  val target: TargetNode)
  : Serializable {

  override fun equals(other: Any?): Boolean =
    this.target.selector.value == (other as BookmarkAnnotation).target.selector.value
  override fun hashCode(): Int =
    this.target.selector.value.hashCode()
}

@JsonDeserialize
class FirstNode(
  @JvmField
  @JsonProperty("items")
  val items: List<BookmarkAnnotation>,

  @JvmField
  @JsonProperty("type")
  val type: String,

  @JvmField
  @JsonProperty("id")
  val id: String)
