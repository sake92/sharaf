package ba.sake.querson

import java.util.UUID

enum Color derives QueryStringRW:
  case Red
  case Blue

case class QuerySimple(str: String, int: Int, uuid: UUID) derives QueryStringRW
case class QuerySimpleReservedChars(`what%the&stu$f?@[]`: String) derives QueryStringRW

case class QueryEnum(color: Color) derives QueryStringRW

case class QuerySeq(a: Seq[String]) derives QueryStringRW
case class QuerySeqSeq(a: Seq[Seq[String]]) derives QueryStringRW

case class QueryNested(search: String, p: Page) derives QueryStringRW
case class Page(number: Int, size: Int) derives QueryStringRW

// Option and Seq have global defaults (in typeclass instance)
case class QueryDefaults(q: String = "default", opt: Option[String], seq: Seq[String]) derives QueryStringRW
