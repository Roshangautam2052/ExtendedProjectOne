package services

import cats.data.EitherT
import connectors.LibraryConnector
import models.{APIError, Book}
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.{JsValue, Json, OFormat}

import scala.concurrent.ExecutionContext


class LibraryServiceSpec extends AnyWordSpec with Matchers with MockFactory with ScalaFutures with GuiceOneServerPerSuite {

  val mockConnector: LibraryConnector = mock[LibraryConnector]
  implicit val executionContext: ExecutionContext = app.injector.instanceOf[ExecutionContext]
  val testService = new LibraryService(mockConnector)

  val gameOfThrones: JsValue = Json.obj(
    "_id" -> "someId",
    "name" -> "A Game of Thrones",
    "description" -> "The best book!!!",
    "pageCount" -> 100
  )

  "getGoogleBook" should {
    val url: String = "testUrl"
    "return a book" in {
      (mockConnector.get[Book](_:String)(_: OFormat[Book], _: ExecutionContext))
        .expects(url, *, *)
        .returning(EitherT.rightT(gameOfThrones.as[Book]))
        .once()

       whenReady(testService.getGoogleBook(urlOverride = Some(url), search = "", term = "").value) {
         result =>
           result shouldBe Right(gameOfThrones.as[Book])
      }
    }
    "return an error" in {
      (mockConnector.get[Book](_: String) (_: OFormat[Book], _: ExecutionContext))
        .expects(url, *, *)
        .returning(EitherT.leftT(APIError.BadAPIResponse(500, "Could not connect")))
        .once()

      whenReady(testService.getGoogleBook(urlOverride = Some(url), search = "", term = "").value) { result =>
        result shouldBe Left(APIError.BadAPIResponse(500, "Could not connect"))
      }
    }
  }
}


