package ba.sake.formson

@main def formsonMain = {

  println(
    UserSortQS(Seq(Stuff(SortByQS.name))).toFormDataMap()
  )

  println(
    Map(
      "by[0][x]" -> Seq(FormValue.Str("name")),
      "by[5][x]" -> Seq(FormValue.Str("email"))
    ).parseFormDataMap[UserSortQS]
  )
}

enum SortByQS derives FormDataRW:
  case name, email

case class Stuff(x: SortByQS) derives FormDataRW

case class UserSortQS(by: Seq[Stuff]) derives FormDataRW
