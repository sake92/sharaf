import java.util.UUID
import ba.sake.tupson.JsonRW

case class Todo(id: UUID, title: String, completed: Boolean, url: String, order: Option[Int]) derives JsonRW

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
