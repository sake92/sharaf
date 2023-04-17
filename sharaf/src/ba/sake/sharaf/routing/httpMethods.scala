package ba.sake.sharaf.routing

import io.undertow.util.Methods
import io.undertow.util.HttpString

object GET:
  def unapply[T](str: HttpString): Boolean =
    Methods.GET == str

object POST:
  def unapply[T](str: HttpString): Boolean =
    Methods.POST == str

object PUT:
  def unapply[T](str: HttpString): Boolean =
    Methods.PUT == str

object DELETE:
  def unapply[T](str: HttpString): Boolean =
    Methods.DELETE == str

object OPTIONS:
  def unapply[T](str: HttpString): Boolean =
    Methods.OPTIONS == str

object PATCH:
  def unapply[T](str: HttpString): Boolean =
    Methods.PATCH == str
