package demo

import java.util.UUID
import ba.sake.tupson.JsonRW

case class CustomerRes(id: UUID, name: String, address: AddressRes) derives JsonRW

case class AddressRes(street: String) derives JsonRW
