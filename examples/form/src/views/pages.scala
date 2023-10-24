package demo
package views

import java.nio.file.Files
import scalatags.Text.all.*
import ba.sake.hepek.html.*
import Bundle.{Form, Grid, Panel}

val FormPage: HtmlPage = new MyPage {

  override def pageSettings = super.pageSettings.withTitle("Home")

  override def pageContent: Frag = Grid.row(
    Panel.panel(
      Panel.Companion.Type.Info,
      body = """
        Hello there!  
        Please fill in the following form:
      """.md
    ),
    Form.form(action := "/form-submit", method := "POST", enctype := "multipart/form-data")(
      Form.inputText(required)("name", "Name"),
      Form.inputText(required)("address.street", "Street"),
      Form.inputText(required)("hobbies[0]", "Hobby 1"),
      Form.inputText()("hobbies[1]", "Hobby 2"),
      Form.inputFile(required)("file", "Document"),
      Form.inputSubmit()("Submit")
    )
  )
}

def ResultPage(req: CreateCustomerForm): HtmlPage = new MyPage {

  private val fileAsString = Files.readString(req.file)

  override def pageSettings = super.pageSettings.withTitle("Result")

  override def pageContent: Frag = Grid.row(
    Panel.panel(
      Panel.Companion.Type.Success,
      body = s"""
        You have successfully submitted these values:
        - name: ${req.name}  
        - street: ${req.address.street}  
        - hobbies: ${req.hobbies.mkString(",")}  
        - file: ${fileAsString}  
      """.md
    )
  )
}
