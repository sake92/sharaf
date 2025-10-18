package ba.sake.sharaf.http4s

import cats.effect.*

type Http4sRequest = org.http4s.Request[IO]

type Http4sResponse = org.http4s.Response[IO]
val Http4sResponse = org.http4s.Response
