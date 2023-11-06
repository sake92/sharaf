package api

import ba.sake.tupson.JsonRW
import ba.sake.querson.QueryStringRW
import ba.sake.validson.*

case class CreateProductReq private (name: String, quantity: Int) derives JsonRW

object CreateProductReq:
  def of(name: String, quantity: Int): CreateProductReq =
    CreateProductReq(name, quantity).validateOrThrow

  given Validator[CreateProductReq] = Validator
    .derived[CreateProductReq]
    .and(_.name, !_.isBlank, "must not be blank")
    .and(_.quantity, _ >= 0, "must not be negative")

// query params
case class ProductsQuery(name: Set[String], minQuantity: Option[Int]) derives QueryStringRW
