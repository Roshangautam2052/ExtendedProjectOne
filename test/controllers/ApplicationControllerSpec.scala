package controllers

import baseSpec.BaseSpecWithApplication
import models.DataModel
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.Future

class ApplicationControllerSpec extends BaseSpecWith                                                                Application {
  val TestApplicationController = new ApplicationController(
    component, repository, executionContext
  )
  private val dataModel: DataModel = DataModel(
    "abcd",
    "test name",
    "test description",
    100
  )
  "ApplicationController. index()" should {
    val result = TestApplicationController.index()(FakeRequest())
    "return Right" in {
      status(result) shouldBe OK
    }

  }
  "ApplicationController. create()" should {

    "create a book in a database" in {
      val request: FakeRequest[JsValue] = buildPost("/api").withBody[JsValue](Json.toJson(dataModel))
      val createdResult: Future[Result] = TestApplicationController.create()(request)
      status (createdResult) shouldBe Status.CREATED
    }

  }
  "ApplicationController. read(id:String)" should {
  }
  "ApplicationController. update(id:String)" should {
  }
  "ApplicationController. delete(id:String)" should {
  }


}
