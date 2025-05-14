package files.tutorials

import utils.*


object HTML extends TutorialPage {

  override def pageSettings = super.pageSettings
    .withTitle("HTML")

  override def blogSettings =
    super.blogSettings.withSections(htmlSection)

  val htmlSection = Section(
    "Serving HTML",
    s"""
    ### Scalatags
    You can return a scalatags `doctype` directly in the `Response.withBody()`.  
    Let's make a simple HTML page that greets the user.  
    Create a file `html.sc` and paste this code into it:
    ```scala
    ${ScalaCliFiles.html_scalatags.indent(4)}
    ```

    and run it like this:
    ```sh
    scala-cli html.sc 
    ```

    Go to [http://localhost:8181](http://localhost:8181)  
    to see how it works.

    ### Hepek Components
    Sharaf supports the [hepek-components](https://sake92.github.io/hepek/hepek/components/reference/bundle-reference.html) too.  
    Hepek wraps scalatags with helpful utilities  like Bootstrap 5 templates, form helpers etc. so you can focus on the important stuff.    
    It is *plain scala code* as a "template engine", so there is no separate language you need to learn.  

    ---
    
    Let's make a simple HTML page that greets the user.  
    Create a file `html.sc` and paste this code into it:
    ```scala
    ${ScalaCliFiles.html_hepek.indent(4)}
    ```

    and run it like this:
    ```sh
    scala-cli html.sc 
    ```

    Go to [http://localhost:8181](http://localhost:8181)  
    to see how it works.

    """.md
  )
}
