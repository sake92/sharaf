package demo
package views

import java.nio.file.Files
import ba.sake.validson.ValidationError
import scalatags.Text.all.*
import ba.sake.hepek.html.HtmlPage
import Bundle._

def ShowFormPage(formData: Option[CreateCustomerForm] = None, errors: Seq[ValidationError] = Seq.empty): HtmlPage =
  new MyPage {

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
        formData.map(_.hobbies).getOrElse(List("")).zipWithIndex.map { case (hobby, idx) =>
          withValueAndValidation(s"hobbies[${idx}]", _.hobbies.applyOrElse(idx, _ => "")) {
            case (fieldName, fieldValue, state, messages) =>
              Form.inputText(required, value := fieldValue)(
                fieldName,
                s"Hobby ${idx + 1}",
                _validationState = state,
                _messages = messages
              )
          }
        },
        Form.inputFile(required)("file", "Document"),
        Form.inputSubmit(Classes.btnPrimary)("Submit")
      )
    )

    // errors are returned as JSON Path, hence the $. prefix below!
    private def withValueAndValidation(fieldName: String, extract: CreateCustomerForm => String)(
        f: (String, String, Option[Form.ValidationState], Seq[String]) => Frag
    ) =
      val fieldErrors = errors.filter(_.path == s"$$.$fieldName")
      val (state, errMsgs) =
        if fieldErrors.isEmpty then None -> Seq.empty
        else Some(Form.ValidationState.Error) -> fieldErrors.map(_.msg)
      f(fieldName, formData.map(extract).getOrElse(""), state, errMsgs)

  }

def SucessPage(formData: CreateCustomerForm): HtmlPage = new MyPage {

  private val fileAsString = Files.readString(formData.file)

  override def pageSettings = super.pageSettings.withTitle("Result")

  override def pageContent: Frag = Grid.row(
    Panel.panel(
      Panel.Companion.Type.Success,
      body = s"""
        You have successfully submitted these values:
        - name: ${formData.name}  
        - street: ${formData.address.street}  
        - hobbies: ${formData.hobbies.mkString(",")}  
        - file: ${fileAsString}  
      """.md
    )
  )
}
