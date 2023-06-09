package ba.sake.querson

@main def quersonMain = {

  // params
  val rw1 = QueryStringParamRW[Seq[Int]]
  println(rw1.parse("q", Seq()))
  // println(rw1.write("q", Seq(1, 2, 3)))

  // case classes
  val rw2 = QueryStringRW[MyParams]
  /* println(
    rw2.write("", MyParams("oppp", 5, Seq("sta", "ba"), PageRequest(1, 10)))
  )*/

  val qs = parseQueryString(
    Map(
      "a" -> Seq("a1", "a2"),
      "b" -> Seq("1", "b2"),
      "q" -> Seq("q1", "q2"),
      "p.number" -> Seq("1"),
      "p.size" -> Seq("44"),
    )
  )
  println(
    rw2.parse(
      "",
      qs
    )
  )

}

case class MyParams(
    a: String,
    b: Int,
    q: Seq[String],
    p: PageRequest
) derives QueryStringRW

case class PageRequest(
    number: Int,
    size: Int
) derives QueryStringRW
