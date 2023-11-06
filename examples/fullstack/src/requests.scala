package fullstack

import java.nio.file.Path
import ba.sake.formson.*
import ba.sake.validson.*

case class CreateCustomerForm(
    name: String,
    file: Path,
    address: CreateAddressForm,
    hobbies: List[String]
) derives FormDataRW

object CreateCustomerForm:
  given Validator[CreateCustomerForm] = Validator
    .derived[CreateCustomerForm]
    .and(_.name, !_.isBlank, "must not be blank")
    .and(_.name, _.length >= 2, "must be >= 2")

case class CreateAddressForm(street: String) derives FormDataRW
