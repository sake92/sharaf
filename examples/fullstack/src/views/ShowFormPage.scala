package fullstack.views

import ba.sake.validson.ValidationError
import ba.sake.sharaf.*
import fullstack.CreateCustomerForm
import play.twirl.api.Html

def ShowFormPage(formData: CreateCustomerForm, errors: Seq[ValidationError] = Seq.empty) = {
  // errors are returned as JSON Path, hence the $. prefix below!
  def withInputErrors(fieldName: String, extract: CreateCustomerForm => String)(
      f: (String, String, Seq[String]) => Html
  ) = {
    val fieldErrors = errors.filter(_.path == s"$$.$fieldName").map(_.msg)
    f(fieldName, extract(formData), fieldErrors)
  }

  val message =
    if errors.isEmpty then html"Hello there!  Please fill in the following form:"
    else html"There were some errors in the form, please fix them:"

  val nameInput = withInputErrors("name", _.name) { (fieldName, fieldValue, fieldErrors) =>
    html"""
    <label>
    ${fieldName.capitalize}: 
    <input type="text" name="${fieldName}" value="${fieldValue}" autofocus>
    ${
        if fieldErrors.isEmpty then html""
        else html"""<div class="card warn">${fieldErrors.map(e => html"<div>${e}</div>")}</div>"""
      }
    </label>
    """
  }
  val hobbiesInputs = formData.hobbies.zipWithIndex.map { case (hobby, idx) =>
    withInputErrors(s"hobbies[${idx}]", _.hobbies.applyOrElse(idx, _ => "")) {
      case (fieldName, fieldValue, fieldErrors) =>
        html"""
        <label>
        ${fieldName.capitalize}: 
        <input type="text" name="${fieldName}" value="${fieldValue}" autofocus>
        ${
            if fieldErrors.isEmpty then html""
            else html"""<div class="card warn">${fieldErrors.map(e => html"<div>${e}</div>")}</div>"""
          }
        </label>
        """
    }
  }
  html"""
    <!DOCTYPE html>
    <html>
    <head>
        <title>Home</title>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <link rel="stylesheet" href="/styles/classless.css">
    </head>
    <body>
        <div>${message}</div>
        <form action="/form-submit" method="POST" enctype="multipart/form-data">
            ${nameInput}
            ${hobbiesInputs}
            <label>
                File: <input type="file" name="file" accept=".txt,.json,.xml" required>
            </label>
            <input type="submit" value="Submit">
        </form>
    </body>
    </html>
  """

}
