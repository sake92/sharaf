package demo

import ba.sake.formson.*
import ba.sake.validson.*

case class CreateCustomerForm(
    name: String,
    file: java.nio.file.Path,
    address: CreateAddressForm,
    hobbies: List[String]
) derives FormDataRW

object CreateCustomerForm:
  given Validator[CreateCustomerForm] = Validator
    .derived[CreateCustomerForm]
    .and(_.name, !_.isBlank, "must not be blank")

case class CreateAddressForm(
    street: String
) derives FormDataRW
