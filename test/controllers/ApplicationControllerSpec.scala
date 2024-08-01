package controllers

import baseSpec.BaseSpecWithApplication
import models.{Book, InvalidDataModel}
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.Future

class ApplicationControllerSpec extends BaseSpecWithApplication {
  val TestApplicationController = new ApplicationController(
    component, repository, executionContext,service
  )
  private val dataModel: Book = Book(
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

  private val updatedDataModel: Book = dataModel.copy("abcd", "Lord of the rings", "Fictional", 200)
  private val invalidIdBody: Book = dataModel.copy("1234", "Lord of the rings", "Fictional", 200)

  "ApplicationController. index()" should {
    "return the list of books and return http status OK" when {
      "the request is valid and database is not empty" in {
        val result = TestApplicationController.index()(FakeRequest())
        status(result) shouldBe OK
      }
    }
    "return http status of 404" when {
      "the database is empty or no database exists" in {
        val invalidResult = TestApplicationController.index()(FakeRequest())
        status(invalidResult) shouldBe NOT_FOUND
      }
    }

  }

  "ApplicationController. create()" should {
    "add book to the database and return CREATED " when {
      "the data model is valid and inserted in the database" in {
        beforeEach()
        val request: FakeRequest[JsValue] = buildPost("/api").withBody[JsValue](Json.toJson(dataModel))
        val createdResult: Future[Result] = TestApplicationController.create()(request)
        status(createdResult) shouldBe Status.CREATED
        afterEach()
      }
    }
    "return BAD_REQUEST" when {
      "the data model is invalid" in {
        beforeEach()
        val request: FakeRequest[JsValue] = buildPost("/api").withBody[JsValue](Json.toJson(invalidDataModel))
        val createdResult: Future[Result] = TestApplicationController.create()(request)
        status(createdResult) shouldBe Status.BAD_REQUEST
        afterEach()
      }
    }
  }

  "ApplicationController. read(id:String)" should {
    " return the book with given id and return OK" when {
      "the id exits in the database " in {
        val request: FakeRequest[JsValue] = buildGet(s"/api/${dataModel._id}").withBody[JsValue](Json.toJson(dataModel))
        val createdResult: Future[Result] = TestApplicationController.create()(request)
        val readResult: Future[Result] = TestApplicationController.read("abcd")(FakeRequest())
        status(readResult) shouldBe OK
        contentAsJson(readResult).as[Book] shouldBe dataModel
      }
    }
    "return NOT_FOUND" when {
      "the book id is not found in the database" in {
        val request: FakeRequest[JsValue] = FakeRequest(POST, s"/api/${dataModel._id}").withBody[JsValue](Json.toJson(dataModel))
        val createdResult: Future[Result] = TestApplicationController.create()(request)
        status(createdResult) shouldBe Status.CREATED
        // Read a book id that does not exists
        val invalidId = "123"
        val invalidReadRequest: FakeRequest[AnyContent] = FakeRequest(GET, s"/api/$invalidId")
        val invalidRequest: Future[Result] = TestApplicationController.read(invalidId)(invalidReadRequest)
        status(invalidRequest) shouldBe NOT_FOUND
      }

    }
  }

  "ApplicationController. update(id:String)" should {
    "update the book with given id and return ACCEPTED" when {
      "the request body is valid and the id exists" in {
        val request: FakeRequest[JsValue] = buildPost(s"/api/${dataModel._id}").withBody[JsValue](Json.toJson(dataModel))
        val createdResult: Future[Result] = TestApplicationController.create()(request)

        // update the book with valid id
        val updatedRequest: FakeRequest[JsValue] = FakeRequest(PUT, s"/api/${dataModel._id}").withBody(Json.toJson(updatedDataModel))
        val updatedResult: Future[Result] = TestApplicationController.update("abcd")(updatedRequest)
        status(updatedResult) shouldBe ACCEPTED
      }
    }
    "return NOT_FOUND" when {
      "the request body is valid and the id does not exists" in {
        val request: FakeRequest[JsValue] = buildPost(s"/api/${dataModel._id}").withBody[JsValue](Json.toJson(dataModel))
        val createdResult: Future[Result] = TestApplicationController.create()(request)
        // update the book with valid body but with invalid id
        val invalidId: String = "1234"
        val updatedRequest: FakeRequest[JsValue] = FakeRequest(PUT, s"/api/${invalidId}").withBody(Json.toJson(invalidIdBody))
        val updatedResult: Future[Result] = TestApplicationController.update(invalidId)(updatedRequest)
        status(updatedResult) shouldBe NOT_FOUND
      }
    }
    "return Bad_Request" when {
      "the request body is invalid " in {
        val request: FakeRequest[JsValue] = buildPost(s"/api/${dataModel._id}").withBody[JsValue](Json.toJson(dataModel))
        val createdResult: Future[Result] = TestApplicationController.create()(request)
        // update the book with invalid body
        val updatedRequest: FakeRequest[JsValue] = FakeRequest(PUT, s"/api/${dataModel._id}").withBody(Json.toJson(invalidDataModel))
        val updatedResult: Future[Result] = TestApplicationController.update("abcd")(updatedRequest)
        status(updatedResult) shouldBe BAD_REQUEST
      }
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
