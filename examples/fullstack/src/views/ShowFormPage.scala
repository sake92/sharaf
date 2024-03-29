package fullstack.views

import ba.sake.validson.ValidationError
import Bundle.*, Tags.*
import fullstack.CreateCustomerForm

class ShowFormPage(formData: CreateCustomerForm, errors: Seq[ValidationError] = Seq.empty) extends MyPage {

  override def pageSettings = super.pageSettings.withTitle("Home")

  override def pageContent: Frag = Grid.row(
    Panel.panel(
      Panel.Companion.Type.Info,
      body = Grid.row(
        Grid.half(
          if errors.isEmpty then """
            Hello there!  
            Please fill in the following form:
          """.md
          else """
            There were some errors in the form, please fix them:
          """.md
        ),
        Grid.half(img(src := "images/icons8-screw-100.png"))
      )
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
      formData.hobbies.zipWithIndex.map { case (hobby, idx) =>
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
    f(fieldName, extract(formData), state, errMsgs)

}
