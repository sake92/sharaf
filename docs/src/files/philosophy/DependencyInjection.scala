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

    Yes in a direct, context functions (implicit functions) scala 3 style.  
    If you ever used PlayFramework, Slick 2 and similar you might be used to this pattern:
    ```scala
    someFunction { implicit ctx: Ctx =>
      // some code that needs an implicit Ctx
    }
    ```

    In Scala 3 there is a new concept called "context function" which represents the pattern above through a type:
    ```scala
    type ContextualAction = Ctx ?=> Unit
    ```
    Now, instead of manually writing `implicit ctx` we can skip it:
    ```scala
    someFunction {
      // some code that needs an implicit Ctx
    }
    ```
    and compiler will take care of it.


    ---
    As an example in Sharaf itself, the `Routes` type is defined as `Request ?=> PartialFunction[RequestParams, Response[?]]`.  
    This means, for example, that you can call `Request.current` only in a `Routes` definition body (because it requires a `given Request`).  

    As a concrete example, instead of making a `@RequestScoped @Bean` like in Spring, you would define a function that requires a `given Request`:
    ```scala
    def currentUser(using req: Request): User =
      // extract stuff from request
    ```

    Plus you avoid [banging your head against the wall](https://stackoverflow.com/questions/26305295/how-is-the-requestscoped-bean-instance-provided-to-sessionscoped-bean-in-runti)
    while trying to figure out how-the-hell can you inject a request-scoped-thing into a singleton/session-scoped thing...  
    Proxy to proxy to proxy, something, something.. ok.
    
    And you avoid reading yet-another-lousy-monad-tutorial, losing your brain-battle agains `State`, `RWS`, `Kleisli`, higher-kinded-types, weird macros, compile times and type inference...
    """.md
  )

}
