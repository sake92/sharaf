package demo

import ba.sake.tupson.JsonRW
import ba.sake.querson.QueryStringRW
import ba.sake.validson.*

case class CreateCustomerReq private (name: String, address: CreateAddressReq) derives JsonRW

object CreateCustomerReq:
  // smart constructor
  def of(name: String, address: CreateAddressReq): CreateCustomerReq =
    val res = new CreateCustomerReq(name, address)
    res.validateOrThrow

  given Validator[CreateCustomerReq] = Validator
    .derived[CreateCustomerReq]
    .and(_.name, !_.isBlank, "must not be blank")

//////
case class CreateAddressReq(street: String) derives JsonRW

object CreateAddressReq:
  given Validator[CreateAddressReq] = Validator
    .derived[CreateAddressReq]
    .and(_.street, !_.isBlank, "must not be blank")
    .and(_.street, _.length >= 3, "must be >= 3")

//////
case class UserQuery(name: Set[String]) derives QueryStringRW
