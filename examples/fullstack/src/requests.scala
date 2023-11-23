package fullstack

import java.nio.file.Path
import ba.sake.formson.*
import ba.sake.validson.*
import java.nio.file.Paths

case class CreateCustomerForm(
    name: String,
    file: Path,
    hobbies: Seq[String]
) derives FormDataRW

object CreateCustomerForm:

  val empty = CreateCustomerForm("", Paths.get(""), Seq.empty)

  given Validator[CreateCustomerForm] = Validator
    .derived[CreateCustomerForm]
    .and(_.name, !_.isBlank, "must not be blank")
    .and(_.name, _.length >= 2, "must be >= 2")
