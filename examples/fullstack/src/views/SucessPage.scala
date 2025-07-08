package fullstack.views

import java.nio.file.Files
import ba.sake.sharaf.*
import fullstack.CreateCustomerForm

def SucessPage(formData: CreateCustomerForm) = {

  val fileAsString = Files.readString(formData.file)

  html"""
    <!DOCTYPE html>
    <html>
    <head>
        <title>Result</title>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <link rel="stylesheet" href="/styles/classless.css">
    </head>
    <body>
        <div class="card info">
            You have successfully submitted these values:
            <ul>
                <li><strong>Name:</strong> ${formData.name}</li>
                <li><strong>Hobbies:</strong> ${formData.hobbies.mkString(", ")}</li>
                <li><strong>File Content:</strong> ${fileAsString}</li>
            </ul>
        </div>
    </body>
    </html>
  """

}
