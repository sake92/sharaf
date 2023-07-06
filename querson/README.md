
# Querson

Represent query string as a case class:
```scala

case class QuerySimple(str: String, int: Int, seq: Seq[Double]) derives QueryStringRW

val q = QuerySimple("my text", 5, Seq(3.14, 2.71))

/* writing */
q.toQueryString()
// str=my+text&seq[0]=3.14&seq[1]=2.71&int=5

q.toQueryStringMap()
// Map(str -> List(my text), seq[0] -> List(3.14), seq[1] -> List(2.71), int -> List(5))

/* parsing */
Map(
    "str" -> Seq("my text"),
    "int" -> Seq("5"),
    "seq" -> Seq("3.14", "2.71")
).parseQueryStringMap[QuerySimple]
// QuerySimple(my text,5,List(3.14, 2.71))
```

---

Singleton-cases enums are supported, nesting etc:
```scala
// these can be reused everywhere via composition/nesting
enum SortOrderQS derives QueryStringRW:
  case asc, desc

case class PageQS() derives QueryStringRW

// these are specific for users for example
enum SortByQS derives QueryStringRW:
  case name, email

case class UserSortQS(by: SortByQS, order: SortOrderQS)

case class UsersSearchQS(search: String, sort: UserSortQS, p: PageQS) derives QueryStringRW

/* writing */
val q = UsersSearchQS("Bob", UserSortQS(SortByQS.name, SortOrderQS.desc), PageQS(3, 42))
q.toQueryString()
// p[num]=3&p[size]=42&sort[by]=name&sort[order]=desc&search=Bob

/* parsing */
Map(
    "p[num]" -> Seq("3"),
    "p[size]" -> Seq("42"),
    "sort[by]" -> Seq("name"),
    "sort[order]" -> Seq("desc"),
    "search" -> Seq("Bob")
).parseQueryStringMap[UsersSearchQS]
// UsersSearchQS(Bob,UserSortQS(name,desc),PageQS(3,42))
```

## Configuration

APIs and web framework differ in parsing query params with respect to sequences and nested objects:
- some accept multiple repeating keys for a sequence: `a=5&a=6`
- some accept multiple repeating keys *with brackets* for a sequence: `a[]=5&a[]=6`
- some accept array-like keys for a sequence: `a[0]=5&a[1]=6`
- some accept object-like keys for a nested field: `a.b=5&a.c=6`
- some accept array-like keys for a nested field: `a[b]=5&a[c]=6`

Querson is *very forgiving* when *parsing* these keys, so in most cases it will parse the key/values correctly.

When you need to write the values, you can provide a configuration object:
```scala
// use no brackets for sequences, and use dots for objects
val config = DefaultFormsonConfig.withSeqNoBrackets.withObjDots
q.toQueryString(config)
// seq=1&seq=2&obj.x=4&obj.y=6
```

### TODO

- revisit instanceof calls

