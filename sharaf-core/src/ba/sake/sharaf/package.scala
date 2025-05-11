package ba.sake.sharaf

import ba.sake.sharaf.routing.FromPathParam

type ExceptionMapper = exceptions.ExceptionMapper
val ExceptionMapper = exceptions.ExceptionMapper

val Path = ba.sake.sharaf.routing.Path

object param:
  def unapply[T](str: String)(using fp: FromPathParam[T]): Option[T] =
    fp.parse(str)

export HttpMethod.*
