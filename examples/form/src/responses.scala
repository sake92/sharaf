package demo

import ba.sake.tupson.JsonRW

case class CreateCustomerResponse(
    fileContents: String
) derives JsonRW
