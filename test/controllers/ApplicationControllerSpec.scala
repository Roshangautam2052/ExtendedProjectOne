package controllers

import akka.util.ByteString
import baseSpec.BaseSpecWithApplication
import models.{DataModel, InvalidDataModel}
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.libs.streams.Accumulator
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.Future

class ApplicationControllerSpec extends BaseSpecWithApplication {
  val TestApplicationController = new ApplicationController(
    component, repository, executionContext
  )
  private val dataModel: DataModel = DataModel(
    "abcd",
    "test name",
    "test description",
    100
  )
  private val invalidDataModel: InvalidDataModel = InvalidDataModel(
    "12",
    "invalid name",
    "invalid description"
  )
  private val updatedDataModel = dataModel.copy(name = "updated test name")
  "ApplicationController. index()" should {
    val result = TestApplicationController.index()(FakeRequest())
    "return Right" in {
      status(result) shouldBe OK
    }

  }

  "ApplicationController. create()" should {
    "create a book in a database when the data model is valid" in {
      beforeEach()
      val request: FakeRequest[JsValue] = buildPost("/api").withBody[JsValue](Json.toJson(dataModel))
      val createdResult: Future[Result] = TestApplicationController.create()(request)
      status(createdResult) shouldBe Status.CREATED
      afterEach()
    }
    "return BAD_REQUEST when the data model is invalid " in {
      beforeEach()
      val request: FakeRequest[JsValue] = buildPost("/api").withBody[JsValue](Json.toJson(invalidDataModel))
      val createdResult: Future[Result] = TestApplicationController.create()(request)
      status(createdResult) shouldBe Status.BAD_REQUEST
      afterEach()
    }

  }

  "ApplicationController. read(id:String)" should {
    "find a book in the database by id" in {
      val request: FakeRequest[JsValue] = buildGet(s"/api/${dataModel._id}").withBody[JsValue](Json.toJson(dataModel))
      val createdResult: Future[Result] = TestApplicationController.create()(request)
      val readResult: Future[Result] = TestApplicationController.read("abcd")(FakeRequest())
      status(readResult) shouldBe OK
      contentAsJson(readResult).as[DataModel] shouldBe dataModel
    }
    "return NOT_FOUND when the book id is not found" in {
      val request: FakeRequest[JsValue] = FakeRequest(POST, s"/api/${dataModel._id}").withBody[JsValue](Json.toJson(dataModel))
      val createdResult: Future[Result] = TestApplicationController.create()(request)
      val invalidId = "123"
      val invalidReadRequest: FakeRequest[AnyContent] = FakeRequest(GET, s"/api/$invalidId")
      val invalidRequest: Future[Result] = TestApplicationController.read(invalidId)(invalidReadRequest)
      status(invalidRequest) shouldBe NOT_FOUND

    }
  }

  "ApplicationController. update(id:String)" should {
    "update a book in the database when the book id is found" in {
      val request: FakeRequest[JsValue] = buildPost(s"/api/${dataModel._id}").withBody[JsValue](Json.toJson(dataModel))
      val createdResult: Future[Result] = TestApplicationController.create()(request)
      val updatedRequest: FakeRequest[JsValue] = FakeRequest(PUT, s"/api/${dataModel._id}").withBody(Json.toJson(updatedDataModel))
      val updatedResult: Future[Result] = TestApplicationController.update("abcd")(updatedRequest)
      status(updatedResult) shouldBe ACCEPTED
    }
  }

  "ApplicationController. delete(id:String)" should {
    "delete a book in the database by id" in {
      val request: FakeRequest[JsValue] = buildPost(s"/api/${dataModel._id}").withBody[JsValue](Json.toJson(dataModel))
      val createdResult: Future[Result] = TestApplicationController.create()(request)
      val deleteResult: Future[Result] = TestApplicationController.delete("abcd")(FakeRequest())
      status(deleteResult) shouldBe ACCEPTED
    }
    "return NOT_FOUND when the book id is not found in the database " in {
      val request: FakeRequest[JsValue] = buildPost(s"/api/${dataModel._id}").withBody[JsValue](Json.toJson(dataModel))
      val createdResult: Future[Result] = TestApplicationController.create()(request)
      val deleteResult: Future[Result] = TestApplicationController.delete("abc")(FakeRequest())
      status(deleteResult) shouldBe NOT_FOUND
    }
  }

  override def beforeEach(): Unit = await(repository.deleteAll())

  override def afterEach(): Unit = await(repository.deleteAll())


}
