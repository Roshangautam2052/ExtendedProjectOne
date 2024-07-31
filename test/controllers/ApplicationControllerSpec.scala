package controllers
import akka.http.scaladsl.model.HttpHeader.ParsingResult.Ok
import play.api.test.FakeRequest
import play.api.http.Status
import play.api.test.Helpers._
import baseSpec.BaseSpecWithApplication
import repositories.DataRepository

class ApplicationControllerSpec extends BaseSpecWithApplication {
  val TestApplicationController = new ApplicationController(
    component, repository, executionContext
  )
  "ApplicationController. index()" should {
    val result = TestApplicationController.index()(FakeRequest())
    "return Right" in {
      status(result) shouldBe OK
    }

  }
  "ApplicationController. create()" should {

  }
  "ApplicationController. read(id:String)" should {
  }
  "ApplicationController. update(id:String)" should {
  }
  "ApplicationController. delete(id:String)" should {
  }


}
