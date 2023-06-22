package demo

import ba.sake.validation.*
import ba.sake.formson.*

case class CreateCustomerForm(
    name: String,
    file: java.nio.file.Path,
    address: CreateAddressForm,
    hobbies: List[String]
) derives FormDataRW {
  validate(
    check(name).is(!_.isBlank, "must not be blank")
  )
}

case class CreateAddressForm(
    street: String
) derives FormDataRW
