package demo

import ba.sake.validation.*
import ba.sake.tupson.JsonRW

case class CreateCustomerReq(name: String, address: CreateAddressReq) derives JsonRW {
  validate(
    check(name).is(!_.isBlank, "must not be blank")
  )
}

case class CreateAddressReq(street: String) derives JsonRW {
  validate(
    check(street).is(!_.isBlank, "must not be blank"),
    check(street).is(_.length >= 3, "must be >= 3")
  )
}
