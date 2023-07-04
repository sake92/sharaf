
# Validson

A tiny validation library for scala 3.

Everything revolves around `Validator[T]`.  
You can start with `Validator.derived[T]` for any `case class`, and then chain additional checks with `and` clauses.

```scala

case class SimpleData(num: Int, str: String, seq: Seq[String])
object SimpleData:
  given Validator[SimpleData] = Validator
    .derived[SimpleData]
    .and(_.num, _ > 0, "must be positive")
    .and(_.str, !_.isBlank, "must not be blank")
    .and(_.seq, _.nonEmpty, "must not be empty")
    .and(_.seq, _.forall(_.size == 2), "must have elements of size 2")


case class ComplexData(password: String, datas: Seq[SimpleData], matrix: Seq[Seq[SimpleData]])

object ComplexData:
  given Validator[ComplexData] = Validator
    .derived[ComplexData]
    .and(_.password, _.contains("A"), "must contain A")
    .and(_.password, _.contains("5"), "must contain 5")
    .and(_.matrix, _.nonEmpty, "must not be empty")

val data = ComplexData("my_pwd", Seq(SimpleData(0, " ", Seq.empty)), Seq(Seq(SimpleData(-55, "   ", Seq.empty))))
data.validate
// returns a nice list of errors:
//  Seq(
//    ValidationError("$.password", "must contain A", "my_pwd"),
//    ValidationError("$.password", "must contain 5", "my_pwd"),
//    ValidationError("$.datas[0].num", "must be positive", 0),
//    ValidationError("$.datas[0].str", "must not be blank", " "),
//    ValidationError("$.datas[0].seq", "must not be empty", Seq.empty),
//    ValidationError("$.matrix[0][0].num", "must be positive", -55),
//    ValidationError("$.matrix[0][0].str", "must not be blank", "   "),
//    ValidationError("$.matrix[0][0].seq", "must not be empty", Seq.empty)
//  )
//)

// you can also use validateOrThrow if you want to throw an exception instead
data.validateOrThrow
// throws a ValidationException which contains errors like above
```


