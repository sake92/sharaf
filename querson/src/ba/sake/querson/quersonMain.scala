package ba.sake.querson

@main def quersonMain = {

  println(
    Map(
      "c" -> Seq("B"),
      "a" -> Seq("aaaaaaaaa"),
      "p[number]" -> Seq("1"),
      "p.size" -> Seq("44")
    ).parseQueryString[MyParams]
  )

}

case class MyParams(
    c: Color,
    a: String = "fdfdsf",
    // b: Int,
    // q: Seq[String],
    p: PageRequest
) derives QueryStringRW

case class PageRequest(
    number: Int,
    size: Int
) derives QueryStringRW

enum Color derives QueryStringParamRW:
  case A
  case B
