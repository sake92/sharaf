package demo

import ba.sake.tupson.JsonRW
import java.util.UUID

case class CustomerRes(id: UUID, name: String, address: AddressRes) derives JsonRW

case class AddressRes(street: String) derives JsonRW
