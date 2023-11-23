package fullstack.views

import java.nio.file.Files
import fullstack.CreateCustomerForm
import Bundle._, Tags.*

class SucessPage(formData: CreateCustomerForm) extends MyPage {

  private val fileAsString = Files.readString(formData.file)

  override def pageSettings = super.pageSettings.withTitle("Result")

  override def pageContent: Frag = Grid.row(
    Panel.panel(
      Panel.Companion.Type.Success,
      body = s"""
        You have successfully submitted these values:
        - name: ${formData.name}  
        - hobbies: ${formData.hobbies.mkString(",")}  
        - file: ${fileAsString}  
      """.md
    )
  )
}
