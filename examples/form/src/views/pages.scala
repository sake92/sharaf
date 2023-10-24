package demo
package views

import java.nio.file.Files
import scalatags.Text.all.*
import ba.sake.hepek.html.*
import ba.sake.validson.*
import Bundle.{Classes, Form, Grid, Panel}

def FormPage(req: Option[CreateCustomerForm] = None, errors: Seq[ValidationError] = Seq.empty): HtmlPage = new MyPage {

  override def pageSettings = super.pageSettings.withTitle("Home")

  override def pageContent: Frag = Grid.row(
    Panel.panel(
      Panel.Companion.Type.Info,
      body =
        if errors.isEmpty then """
          Hello there!  
          Please fill in the following form:
          """.md
        else """
          There were some errors in the form, please fix them:
          """.md
    ),
    Form.form(action := "/form-submit", method := "POST", enctype := "multipart/form-data")(
      withValueAndValidation("name", _.name) { case (fieldName, fieldValue, state, messages) =>
        Form.inputText(required, value := fieldValue)(
          fieldName,
          "Name",
          _validationState = state,
          _messages = messages
        )
      },
      withValueAndValidation("address.street", _.address.street) { case (fieldName, fieldValue, state, messages) =>
        Form.inputText(required, value := fieldValue)(
          fieldName,
          "Street",
          _validationState = state,
          _messages = messages
        )
      },
      withValueAndValidation("hobbies[0]", _.hobbies.headOption.getOrElse("")) {
        case (fieldName, fieldValue, state, messages) =>
          Form.inputText(required, value := fieldValue)(
            fieldName,
            "Hobby 1",
            _validationState = state,
            _messages = messages
          )
      },
      withValueAndValidation("hobbies[1]", _.hobbies.headOption.getOrElse("")) {
        case (fieldName, fieldValue, state, messages) =>
          Form.inputText(required, value := fieldValue)(
            fieldName,
            "Hobby 2",
            _validationState = state,
            _messages = messages
          )
      },
      Form.inputFile(required)("file", "Document"),
      Form.inputSubmit(Classes.btnPrimary)("Submit")
    )
  )

  private def withValueAndValidation(fieldName: String, extract: CreateCustomerForm => String)(
      f: (String, String, Option[Form.ValidationState], Seq[String]) => Frag
  ) =
    val (state, errMsgs) = validationStateAndMessages(fieldName)
    f(fieldName, req.map(extract).getOrElse(""), state, errMsgs)

  // errors are returned as JSON Path, hence the $. prefix below!
  private def validationStateAndMessages(fieldName: String): (Option[Form.ValidationState], Seq[String]) = {
    val fieldErrors = errors.filter(_.path == s"$$.$fieldName")
    if fieldErrors.isEmpty then None -> Seq.empty
    else Some(Form.ValidationState.Error) -> fieldErrors.map(_.msg)

  }
}

def SucessPage(req: CreateCustomerForm): HtmlPage = new MyPage {

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
