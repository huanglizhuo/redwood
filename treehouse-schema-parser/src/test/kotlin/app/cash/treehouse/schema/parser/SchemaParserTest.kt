package app.cash.treehouse.schema.parser

import app.cash.treehouse.schema.Children
import app.cash.treehouse.schema.Property
import app.cash.treehouse.schema.Schema
import app.cash.treehouse.schema.Widget
import org.junit.Test

class SchemaParserTest {
  interface NonAnnotationSchema

  @Test fun nonAnnotatedSchemaThrows() {
    assertThrows<IllegalArgumentException> {
      parseSchema(NonAnnotationSchema::class)
    }.hasMessageThat().isEqualTo(
      "Schema app.cash.treehouse.schema.parser.SchemaParserTest.NonAnnotationSchema missing @Schema annotation")
  }

  @Schema([
    NonAnnotatedWidget::class,
  ])
  interface NonAnnotatedWidgetSchema
  data class NonAnnotatedWidget(
    @Property(1) val name: String,
  )

  @Test fun nonAnnotatedWidgetThrows() {
    assertThrows<IllegalArgumentException> {
      parseSchema(NonAnnotatedWidgetSchema::class)
    }.hasMessageThat().isEqualTo(
      "app.cash.treehouse.schema.parser.SchemaParserTest.NonAnnotatedWidget missing @Widget annotation")
  }

  @Schema([
    DuplicateWidgetTagA::class,
    NonDuplicateWidgetTag::class,
    DuplicateWidgetTagB::class,
  ])
  interface DuplicateWidgetTagSchema
  @Widget(1)
  data class DuplicateWidgetTagA(
    @Property(1) val name: String,
  )
  @Widget(2)
  data class NonDuplicateWidgetTag(
    @Property(1) val name: String,
  )
  @Widget(1)
  data class DuplicateWidgetTagB(
    @Property(1) val name: String,
  )

  @Test fun duplicateWidgetTagThrows() {
    assertThrows<IllegalArgumentException> {
      parseSchema(DuplicateWidgetTagSchema::class)
    }.hasMessageThat().isEqualTo(
      """
      |Schema @Widget tags must be unique
      |
      |- @Widget(1): app.cash.treehouse.schema.parser.SchemaParserTest.DuplicateWidgetTagA, app.cash.treehouse.schema.parser.SchemaParserTest.DuplicateWidgetTagB
      """.trimMargin())
  }

  @Schema([
    RepeatedWidget::class,
    RepeatedWidget::class,
  ])
  interface RepeatedWidgetTypeSchema
  @Widget(1)
  data class RepeatedWidget(
    @Property(1) val name: String,
  )

  @Test fun repeatedWidgetTypeThrows() {
    assertThrows<IllegalArgumentException> {
      parseSchema(RepeatedWidgetTypeSchema::class)
    }.hasMessageThat().isEqualTo(
      """
      |Schema contains repeated widget
      |
      |- app.cash.treehouse.schema.parser.SchemaParserTest.RepeatedWidget
      """.trimMargin())
  }

  @Schema([
    DuplicatePropertyTagWidget::class,
  ])
  interface DuplicatePropertyTagSchema

  @Widget(1)
  data class DuplicatePropertyTagWidget(
    @Property(1) val name: String,
    @Property(2) val age: Int,
    @Property(1) val nickname: String,
  )

  @Test fun duplicatePropertyTagThrows() {
    assertThrows<IllegalArgumentException> {
      parseSchema(DuplicatePropertyTagSchema::class)
    }.hasMessageThat().isEqualTo(
      """
      |Widget app.cash.treehouse.schema.parser.SchemaParserTest.DuplicatePropertyTagWidget's @Property tags must be unique
      |
      |- @Property(1): name, nickname
      """.trimMargin())
  }

  @Schema([
    DuplicateChildrenTagWidget::class,
  ])
  interface DuplicateChildrenTagSchema
  @Widget(1)
  data class DuplicateChildrenTagWidget(
    @Children(1) val childrenA: List<Any>,
    @Property(1) val name: String,
    @Children(1) val childrenB: List<Any>,
  )

  @Test fun duplicateChildrenTagThrows() {
    assertThrows<IllegalArgumentException> {
      parseSchema(DuplicateChildrenTagSchema::class)
    }.hasMessageThat().isEqualTo(
      """
      |Widget app.cash.treehouse.schema.parser.SchemaParserTest.DuplicateChildrenTagWidget's @Children tags must be unique
      |
      |- @Children(1): childrenA, childrenB
      """.trimMargin())
  }

  @Schema([
    UnannotatedPrimaryParameterWidget::class,
  ])
  interface UnannotatedPrimaryParameterSchema
  @Widget(1)
  data class UnannotatedPrimaryParameterWidget(
    @Property(1) val name: String,
    @Children(1) val children: List<Any>,
    val unannotated: String,
  )

  @Test fun unannotatedPrimaryParameterThrows() {
    assertThrows<IllegalArgumentException> {
      parseSchema(UnannotatedPrimaryParameterSchema::class)
    }.hasMessageThat().isEqualTo(
      "Unannotated parameter \"unannotated\" on app.cash.treehouse.schema.parser.SchemaParserTest.UnannotatedPrimaryParameterWidget")
  }

  @Schema([
    NonDataClassWidget::class,
  ])
  interface NonDataClassSchema
  @Widget(1)
  class NonDataClassWidget(
    @Property(1) val name: String,
  )

  @Test fun nonDataClassThrows() {
    assertThrows<IllegalArgumentException> {
      parseSchema(NonDataClassSchema::class)
    }.hasMessageThat().isEqualTo(
      "@Widget app.cash.treehouse.schema.parser.SchemaParserTest.NonDataClassWidget must be 'data' class")
  }

  @Schema([
    InvalidChildrenTypeWidget::class,
  ])
  interface InvalidChildrenTypeSchema
  @Widget(1)
  data class InvalidChildrenTypeWidget(
    @Children(1) val children: List<String>,
  )

  @Test fun invalidChildrenTypeThrows() {
    assertThrows<IllegalArgumentException> {
      parseSchema(InvalidChildrenTypeSchema::class)
    }.hasMessageThat().isEqualTo(
      "@Children app.cash.treehouse.schema.parser.SchemaParserTest.InvalidChildrenTypeWidget#children must be of type 'List<Any>'")
  }
}
