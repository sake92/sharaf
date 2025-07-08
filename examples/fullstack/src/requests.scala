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

  val empty = CreateCustomerForm("", Paths.get(""), Seq(""))

  given Validator[CreateCustomerForm] = Validator
    .derived[CreateCustomerForm]
    .notBlank(_.name)
    .minLength(_.name, 2)
