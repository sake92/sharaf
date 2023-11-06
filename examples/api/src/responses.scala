package api

import java.util.UUID
import ba.sake.tupson.JsonRW

case class ProductRes(id: UUID, name: String, quantity: Int) derives JsonRW
