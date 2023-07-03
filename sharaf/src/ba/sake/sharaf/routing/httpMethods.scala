package ba.sake.sharaf.routing

import io.undertow.util.Methods
import io.undertow.util.HttpString

object GET:
  def unapply(str: HttpString): Boolean =
    Methods.GET == str

object POST:
  def unapply(str: HttpString): Boolean =
    Methods.POST == str

object PUT:
  def unapply(str: HttpString): Boolean =
    Methods.PUT == str

object DELETE:
  def unapply(str: HttpString): Boolean =
    Methods.DELETE == str

object OPTIONS:
  def unapply(str: HttpString): Boolean =
    Methods.OPTIONS == str

object PATCH:
  def unapply(str: HttpString): Boolean =
    Methods.PATCH == str
