package demo

import io.undertow.Undertow
import ba.sake.sharaf.*
import ba.sake.sharaf.routing.*
import ba.sake.sharaf.handlers.*
import ba.sake.tupson.JsonRW

@main def main: Unit = {

  val todosRepo = new TodosRepo

  def todo2Resp(t: Todo): TodoResponse =
    TodoResponse(t.title, t.completed, t.url, t.order)

  val routes: Routes = {
    case GET() -> Path("") =>
      println("AAAAAAAAAAAA")
      Response.withBody(todosRepo.getTodos().map(todo2Resp))

    case GET() -> Path("todos", uuid(id)) =>
      val todo = todosRepo.getTodo(id)
      Response.withBody(todo2Resp(todo))

    case POST() -> Path("") =>
      val reqBody = Request.current.bodyJson[CreateTodo]
      val newTodo = todosRepo.add(reqBody)
      Response.withBody(todo2Resp(newTodo))

    case DELETE() -> Path("") =>
      todosRepo.deleteAll()
      Response.withBody(List.empty[TodoResponse])

    case DELETE() -> Path("todos", uuid(id)) =>
      todosRepo.delete(id)
      Response.withBody(todosRepo.getTodos().map(todo2Resp))

    case PATCH() -> Path("todos", uuid(id)) =>
      val reqBody = Request.current.bodyJson[PatchTodo]
      var todo = todosRepo.getTodo(id)
      reqBody.title.foreach(t => todo = todo.copy(title = t))
      reqBody.completed.foreach(c => todo = todo.copy(completed = c))
      reqBody.url.foreach(u => todo = todo.copy(url = u))
      reqBody.order.foreach(o => todo = todo.copy(order = Some(o)))
      todosRepo.set(todo)
      Response.withBody(todo2Resp(todo))

    case OPTIONS() -> _ =>
      Response
        .withBody("")
        // TODO ..
        // SAMO u response za PRAVI REQUEST
        // ako je missing, onda ta domena nema pravo pristupa
        // ako ima, mora bit == Origin headeru!
        .withHeader("Access-Control-Allow-Origin", "*") // može biti dinamički normala
        .withHeader("Access-Control-Allow-Headers", "*") // SAMO ZA OPTIONS
        .withHeader("Access-Control-Allow-Methods", "GET,HEAD,POST,OPTIONS,PATCH,DELETE") // SAMO ZA OPTIONS
  }

  val handler = RoutesHandler(routes)

  val server = Undertow
    .builder()
    .addHttpListener(8181, "localhost")
    .setHandler(
      handler
    )
    .build()
  server.start()

  val serverInfo = server.getListenerInfo().get(0)
  val url = s"${serverInfo.getProtcol}:/${serverInfo.getAddress}"
  println(s"Started HTTP server at $url")

}

case class CreateTodo(title: String, order: Option[Int]) derives JsonRW

case class PatchTodo(title: Option[String], completed: Option[Boolean], url: Option[String], order: Option[Int])
    derives JsonRW

case class TodoResponse(title: String, completed: Boolean, url: String, order: Option[Int]) derives JsonRW
