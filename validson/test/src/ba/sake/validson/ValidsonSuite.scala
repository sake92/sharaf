package ba.sake.validson

class ValidsonSuite extends munit.FunSuite {

  // this is just so people can use this anywhere..
  // see LowPriValidators.dummyValidator
  test("validate should not care about data with no validation") {
    assertEquals("stuff".validate, Seq.empty)
    assertEquals(NotValidatedData(1, "whatevs", Seq.empty).validate, Seq.empty)
  }

  test("validate should validate simple data") {

    assertEquals(
      SimpleData(1, "ab c", Seq("ab")).validate,
      Seq.empty
    )

    assertEquals(
      SimpleData(1, "ab c", Seq("abc")).validate,
      Seq(
        ValidationError("$.seq", "must have elements of size 2", Seq("abc"))
      )
    )

    assertEquals(
      SimpleData(0, " ", Seq.empty).validate,
      Seq(
        ValidationError("$.num", "must be positive", 0),
        ValidationError("$.str", "must not be blank", " "),
        ValidationError("$.seq", "must be >= 1", Seq.empty)
      )
    )
  }

  test("validate should validate complex data") {

    assertEquals(
      ComplexData("A5", Seq(), Seq(Seq())).validate,
      Seq.empty
    )

    assertEquals(
      ComplexData("", Seq(), Seq()).validate,
      Seq(
        ValidationError("$.password", "must contain A", ""),
        ValidationError("$.password", "must contain 5", ""),
        ValidationError("$.matrix", "must be >= 1", Seq.empty)
      )
    )

    assertEquals(
      ComplexData("A5", Seq(SimpleData(0, " ", Seq.empty)), Seq(Seq(SimpleData(-55, "   ", Seq.empty)))).validate,
      Seq(
        ValidationError("$.datas[0].num", "must be positive", 0),
        ValidationError("$.datas[0].str", "must not be blank", " "),
        ValidationError("$.datas[0].seq", "must be >= 1", Seq.empty),
        ValidationError("$.matrix[0][0].num", "must be positive", -55),
        ValidationError("$.matrix[0][0].str", "must not be blank", "   "),
        ValidationError("$.matrix[0][0].seq", "must be >= 1", Seq.empty)
      )
    )
  }

  test("validate should validate additional string and collection constraints") {
    assertEquals(
      AdditionalData("abc", "person@example.com", "ABC", Seq("one", "two")).validate,
      Seq.empty
    )

    assertEquals(
      AdditionalData("", "person@example", "AB", Seq("one", "")).validate,
      Seq(
        ValidationError("$.required", "must not be empty", ""),
        ValidationError("$.email", "must be a valid email", "person@example"),
        ValidationError("$.code", "must have length 3", "AB"),
        ValidationError("$.tags", "must not be empty", Seq("one", ""))
      )
    )
  }

  test("matches should describe a regular-expression mismatch") {
    assertEquals(
      RegexData("abc").validate,
      Seq(ValidationError("$.value", "must match [A-Z]+", "abc"))
    )
  }

  test("validate should recurse through optional nested sequences") {
    assertEquals(OptionalData(None).validate, Seq.empty)
    assertEquals(
      OptionalData(Some(Seq(SimpleData(0, " ", Seq.empty)))).validate,
      Seq(
        ValidationError("$.data[0].num", "must be positive", 0),
        ValidationError("$.data[0].str", "must not be blank", " "),
        ValidationError("$.data[0].seq", "must be >= 1", Seq.empty)
      )
    )
  }
}

// types
case class NotValidatedData(x: Int, str: String, vals: Seq[String])

case class SimpleData(num: Int, str: String, seq: Seq[String])
object SimpleData:
  given Validator[SimpleData] = Validator
    .derived[SimpleData]
    .positive(_.num)
    .notBlank(_.str)
    .minItems(_.seq, 1)
    .and(_.seq, _.forall(_.size == 2), "must have elements of size 2")

case class ComplexData(password: String, datas: Seq[SimpleData], matrix: Seq[Seq[SimpleData]])

object ComplexData:

  given Validator[ComplexData] = Validator
    .derived[ComplexData]
    .contains(_.password, "A")
    .contains(_.password, "5")
    .minItems(_.matrix, 1)

case class AdditionalData(required: String, email: String, code: String, tags: Seq[String])
object AdditionalData:
  given Validator[AdditionalData] = Validator
    .derived[AdditionalData]
    .notEmpty(_.required)
    .email(_.email)
    .exactLength(_.code, 3)
    .allItems(_.tags, _.nonEmpty, "must not be empty")

case class RegexData(value: String)
object RegexData:
  given Validator[RegexData] = Validator.derived[RegexData].matches(_.value, "[A-Z]+")

case class OptionalData(data: Option[Seq[SimpleData]])
object OptionalData:
  given Validator[OptionalData] = Validator.derived[OptionalData]
