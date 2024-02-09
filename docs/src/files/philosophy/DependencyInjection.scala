package files.philosophy

import utils.Bundle.*

object DependencyInjection extends PhilosophyPage {

  override def pageSettings =
    super.pageSettings.withTitle("Dependency Injection")

  override def blogSettings =
    super.blogSettings.withSections(firstSection)

  val firstSection = Section(
    "Do you even Dependency Injection?",
    s"""
    Not in a classical / "dependency container" / Spring / JEE style.  
    
    Not in a purely-functional-monadic style.

    Yes in a direct style.
    For singletons:
      - just instantiate a class and pass the object around
      - for request/session-scoped instances use scala 3 context functions (implicit functions)

    If you ever used PlayFramework, Slick 2 and similar, you might have used this pattern:
    ```scala
    someFunction { implicit ctx =>
      // some code that needs an implicit Ctx
    }
    ```

    In Scala 3 there is a new concept called "context function" which represents the pattern from above with a type:
    ```scala
    type ContextualAction = Ctx ?=> Unit
    ```
    Now, instead of manually writing `implicit ctx` we can skip it:
    ```scala
    someFunction {
      // some code that needs an implicit Ctx
    }
    ```
    and compiler will fill it in for us.


    ---
    Sharaf has the `Routes` type that is defined as `Request ?=> PartialFunction[RequestParams, Response[?]]`.  
    This means that you can call `Request.current` only in a `Routes` definition body (because it requires a `given Request`).  

    If you need a request-scoped instance (`@RequestScoped @Bean` in Spring),  
    you need to define a function that is `using Request`:
    ```scala
    def currentUser(using req: Request): User =
      // extract stuff from request
    ```
    Same as `Request.current`, you can only use the `currentUser` function in a context of a request!

    ---

    By using context functions, you avoid [banging your head against the wall](https://stackoverflow.com/questions/26305295/how-is-the-requestscoped-bean-instance-provided-to-sessionscoped-bean-in-runti)
    while trying to figure out how-the-hell can you inject a request-scoped-thing into a singleton/session-scoped thing...  
    Proxy to proxy to proxy, something, something.. ok.
    
    You also avoid reading yet-another-lousy-monad-tutorial, losing your brain-battle agains `State`, `RWS`, `Kleisli`, higher-kinded-types, weird macros, compile times and type inference...
    """.md
  )

}
