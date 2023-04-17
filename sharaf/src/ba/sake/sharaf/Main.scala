package ba.sake.sharaf

import ba.sake.validation.*
import ba.sake.tupson.*
import ba.sake.sharaf.routing.*
import io.undertow.util.HttpString
import io.undertow.util.Methods
import java.util.UUID
import org.typelevel.jawn.ast.JValue
import java.{util => ju}
import org.typelevel.jawn.ast.JString

class TodosRepo {

  private var todosRef = List.empty[Todo]

  def getTodos(): List[Todo] = todosRef.synchronized {
    todosRef
  }
  def getTodo(id: UUID): Todo = todosRef.synchronized {
    todosRef.find(_.id == id).get
  }
  def add(req: CreateTodo): Todo = todosRef.synchronized {
    val id = UUID.randomUUID()
    val newTodo = Todo(id, req.title, false, s"http://localhost:8181/todos/${id}", req.order)
    todosRef = todosRef.appended(newTodo)
    newTodo
  }
  def set(t: Todo): Unit = todosRef.synchronized {
    todosRef = todosRef.filterNot(_.id == t.id) :+ t
  }
  def delete(id: UUID): Unit = todosRef.synchronized {
    todosRef = todosRef.filterNot(_.id == id)
  }
  def deleteAll(): Unit = todosRef.synchronized {
    todosRef = List.empty
  }
}

@main def sharafMain: Unit = {

  val todosRepo = new TodosRepo

  def todo2Resp(t: Todo): TodoResponse =
    TodoResponse(t.title, t.completed, t.url, t.order)

  val todoRoutes: Routes = {
    case (GET(), Path(""), _) =>
      Response.json(todosRepo.getTodos().map(todo2Resp))

    case (GET(), Path("todos", uuid(id)), _) =>
      val todo = todosRepo.getTodo(id)
      Response.json(todo2Resp(todo))

    case (POST(), Path(""), _) =>
      val reqBody = Request.current.bodyJson[CreateTodo]
      val newTodo = todosRepo.add(reqBody)
      Response.json(todo2Resp(newTodo))

    case (DELETE(), Path(""), _) =>
      todosRepo.deleteAll()
      Response.json(List.empty[TodoResponse])

    case (DELETE(), Path("todos", uuid(id)), _) =>
      todosRepo.delete(id)
      Response.json(todosRepo.getTodos().map(todo2Resp))

    case (PATCH(), Path("todos", uuid(id)), _) =>
      val reqBody = Request.current.bodyJson[PatchTodo]
      var todo = todosRepo.getTodo(id)
      reqBody.title.foreach(t => todo = todo.copy(title = t))
      reqBody.completed.foreach(c => todo = todo.copy(completed = c))
      reqBody.url.foreach(u => todo = todo.copy(url = u))
      reqBody.order.foreach(o => todo = todo.copy(order = Some(o)))
      todosRepo.set(todo)
      Response.json(todo2Resp(todo))

    case (OPTIONS(), _, _) =>
      Response("")
        // SAMO u response za PRAVI REQUEST
        // ako je missing, onda ta domena nema pravo pristupa
        // ako ima, mora bit == Origin headeru!
        //.withHeader("Access-Control-Allow-Origin", "*") // može biti dinamički normala
        
        .withHeader("Access-Control-Allow-Headers", "*") // SAMO ZA OPTIONS
        .withHeader("Access-Control-Allow-Methods", "GET,HEAD,POST,OPTIONS,PATCH,DELETE") // SAMO ZA OPTIONS
  }

  HttpServer
    .of(todoRoutes)
    .withPort(8181)
    .start()

}

////////// Todo MVC backend
given JsonRW[UUID] = new {

  override def write(value: ju.UUID): JValue = JString(value.toString())

  override def parse(path: String, jValue: JValue): ju.UUID = jValue match
    case JString(s) => UUID.fromString(s)
    case _          => throw TupsonException("Expected a UUID string")

}
case class Todo(id: UUID, title: String, completed: Boolean, url: String, order: Option[Int]) derives JsonRW

case class CreateTodo(title: String, order: Option[Int]) derives JsonRW
case class PatchTodo(title: Option[String], completed: Option[Boolean], url: Option[String], order: Option[Int])
    derives JsonRW
case class TodoResponse(title: String, completed: Boolean, url: String, order: Option[Int]) derives JsonRW

//////////
case class CreateUser(name: String, address: CreateAddress) derives JsonRW {
  validate(
    check(name).is(!_.isBlank, "must not be blank"),
    check(name).is(_.length >= 3, "must be >= 3")
  )
}

case class CreateAddress(name: String) derives JsonRW {
  validate(
    check(name).is(!_.isBlank, "must not be blank"),
    check(name).is(_.length >= 3, "must be >= 3")
  )
}

case class UserQuery(uuid: UUID, age: Option[Int]) derives FromQueryString
