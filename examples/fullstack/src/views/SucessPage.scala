package fullstack.views

import java.nio.file.Files
import Bundle._, Tags.*
import fullstack.CreateCustomerForm

class SucessPage(formData: CreateCustomerForm) extends MyPage {

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
