package demo

import ba.sake.tupson.JsonRW

case class CreateCustomerResponse(
    street: String,
    fileContents: String
) derives JsonRW
