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
        ValidationError("$.seq", "must not be empty", Seq.empty)
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
        ValidationError("$.matrix", "must not be empty", Seq.empty)
      )
    )

    assertEquals(
      ComplexData("A5", Seq(SimpleData(0, " ", Seq.empty)), Seq(Seq(SimpleData(-55, "   ", Seq.empty)))).validate,
      Seq(
        ValidationError("$.datas[0].num", "must be positive", 0),
        ValidationError("$.datas[0].str", "must not be blank", " "),
        ValidationError("$.datas[0].seq", "must not be empty", Seq.empty),
        ValidationError("$.matrix[0][0].num", "must be positive", -55),
        ValidationError("$.matrix[0][0].str", "must not be blank", "   "),
        ValidationError("$.matrix[0][0].seq", "must not be empty", Seq.empty)
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
    .notEmptySeq(_.seq)
    .and(_.seq, _.forall(_.size == 2), "must have elements of size 2")

case class ComplexData(password: String, datas: Seq[SimpleData], matrix: Seq[Seq[SimpleData]])

object ComplexData:

  given Validator[ComplexData] = Validator
    .derived[ComplexData]
    .contains(_.password, "A")
    .contains(_.password, "5")
    .notEmptySeq(_.matrix)
