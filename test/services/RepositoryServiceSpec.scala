package services

import models.{APIError, Book}
import org.mongodb.scala.result.{DeleteResult, UpdateResult}
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import repositories.{DataRepository, Repository}

import scala.concurrent.{ExecutionContext, Future}

class RepositoryServiceSpec extends AnyWordSpec with Matchers with MockFactory with ScalaFutures with GuiceOneServerPerSuite {

  val mockRepository: Repository = mock[Repository]
  implicit val executionContext: ExecutionContext = app.injector.instanceOf[ExecutionContext]
  val repositoryTestService = new RepositoryService(mockRepository)(executionContext)

  "RepositoryService.indexService" should {
    "return a sequence of books when repository returns books" in {
      val books = Seq(Book("123", "Test Book", "Author", 100))
      (mockRepository.index _).expects()
        .returning(Future.successful(Right(books)))

      whenReady(repositoryTestService.indexService()) { result =>
        result shouldBe Right(books)
      }
    }

    "return a DatabaseError when repository returns a DatabaseError" in {
      val error = APIError.DatabaseError(500, "Database error")
      (mockRepository.index _).expects().returning(Future.successful(Left(error)))

      whenReady(repositoryTestService.indexService()) { result =>
        result shouldBe Left(error)
      }
    }

  }
  "RepositoryService.createService" should {
    "return a Book" when {
      "a book is created" in {
        val book = Book("123", "Test Book", "Author", 100)
        (mockRepository.create _).expects(book)
          .returning(Future.successful(Right(book)))

        whenReady(repositoryTestService.createService(book)) {
          result => result shouldBe Right(book)
        }
      }
    }
    "return BadAPI response Error" when {
      " the result is not acknowledged " in {
        val book = Book("123", "Test Book", "Author", 100)
        val error = APIError.BadAPIResponse(500, "Failed to add book")
        (mockRepository.create _).expects(book)
          .returning(Future.successful(Left(error)))

        whenReady(repositoryTestService.createService(book)) {
          result => result shouldBe Left(error)
        }
      }
    }
    "return Database Error" when {
      "exception occurs due to database error " in {
        val book = Book("123", "Test Book", "Author", 100)
        val error = APIError.DatabaseError(500, "Failed to retrieve book due to Insertion failed ")
        (mockRepository.create _).expects(book)
          .returning(Future.successful(Left(error)))

        whenReady(repositoryTestService.createService(book)) {
          result => result shouldBe Left(error)
        }
      }
    }
  }
  "RepositoryService.readService" should {
    "return a Book" when {
      "a valid id is passed " in {
        val book = Book("123", "Test Book", "Author", 100)
        val id: String = "123"
        (mockRepository.read _).expects(id)
          .returning(Future.successful(Right(book)))

        whenReady(repositoryTestService.readService("123")) {
          result => result shouldBe Right(book)
        }
      }
    }
    "return NOT_FOUND error" when {
      " the book is not found in the database" in {
        val book = Book("123", "Test Book", "Author", 100)
        val invalidId: String = "124"
        val error = APIError.NotFoundError(400, "Book is not found.")
        (mockRepository.read _).expects(invalidId)
          .returning(Future.successful(Left(error)))

        whenReady(repositoryTestService.readService(invalidId)) {
          result => result shouldBe Left(error)
        }
      }
    }
    "return Database Error" when {
      "exception occurs due to database error " in {
        val book = Book("123", "Test Book", "Author", 100)
        val invalidId = "123"
        val error = APIError.DatabaseError(500, "Failed to retrieve book")
        (mockRepository.read _).expects(invalidId)
          .returning(Future.successful(Left(error)))

        whenReady(repositoryTestService.readService(invalidId)) {
          result => result shouldBe Left(error)
        }
      }
    }
  }
  "RepositoryService.findByNameService" should {
    "return a Book" when {
      "a valid name is passed " in {
        val book = Book("123", "Test Book", "Author", 100)
        val name: String = "testbook"
        (mockRepository.searchByName _).expects(name)
          .returning(Future.successful(Right(book)))

        whenReady(repositoryTestService.findByNameService(name)) {
          result => result shouldBe Right(book)
        }
      }
    }
    "return NOT_FOUND error" when {
      " the book with give name is not found in the database" in {
        val book = Book("123", "test book", "Author", 100)
        val invalidName = "abc"
        val error = APIError.NotFoundError(400, "Book is not found.")
        (mockRepository.searchByName _).expects(invalidName)
          .returning(Future.successful(Left(error)))

        whenReady(repositoryTestService.findByNameService(invalidName)) {
          result => result shouldBe Left(error)
        }
      }
    }
    "return Database Error" when {
      "exception occurs due to database error while finding the book " in {
        val book = Book("123", "Test Book", "Author", 100)
        val invalidName = "12334"
        val error = APIError.DatabaseError(500, "Failed to retrieve book")
        (mockRepository.searchByName _).expects(invalidName)
          .returning(Future.successful(Left(error)))

        whenReady(repositoryTestService.findByNameService(invalidName)) {
          result => result shouldBe Left(error)
        }
      }
    }
  }
  "RepositoryService.updateFieldValueService" should {
    "return the update result when repository successfully updates the field" in {
      val updateResult = mock[UpdateResult]
      (updateResult.getMatchedCount _).expects().returning(1L)
      (updateResult.getModifiedCount _).expects().returning(1L)
      (mockRepository.updateField _).expects("123", "title", "New Title").returning(Future.successful(Right(updateResult)))

      whenReady(repositoryTestService.updateFieldValueService("123", "title", "New Title")) { result =>
        result shouldBe Right(updateResult)
      }
    }

    "return a NotModified error when repository does not modify any document" in {
      val updateResult = mock[UpdateResult]
      (updateResult.getMatchedCount _).expects().returning(0L)
      (updateResult.getModifiedCount _).expects().returning(0L)
      (mockRepository.updateField _).expects("123", "title", "New Title").returning(Future.successful(Right(updateResult)))

      whenReady(repositoryTestService.updateFieldValueService("123", "title", "New Title")) { result =>
        result shouldBe Left(APIError.NotModified(304, "The field cannot be updated"))
      }
    }

    "return a DatabaseError when repository encounters a database error" in {
      val error = APIError.DatabaseError(500, "Database error")
      (mockRepository.updateField _).expects("123", "title", "New Title").returning(Future.successful(Left(error)))

      whenReady(repositoryTestService.updateFieldValueService("123", "title", "New Title")) { result =>
        result shouldBe Left(error)
      }
    }
  }

  "RepositoryService.updateService" should {
    "return the update result when repository successfully updates the book" in {
      val book = Book("123", "New Title", "Author", 100)
      val updateResult = mock[UpdateResult]
      (updateResult.getMatchedCount _).expects().returning(1L)
      (updateResult.getModifiedCount _).expects().returning(1L)
      (mockRepository.update _).expects("123", book).returning(Future.successful(Right(updateResult)))

      whenReady(repositoryTestService.updateService("123", book)) { result =>
        result shouldBe Right(updateResult)
      }
    }

    "return a NotModified error when repository does not modify any document" in {
      val book = Book("123", "New Title", "Author", 100)
      val updateResult = mock[UpdateResult]
      (updateResult.getMatchedCount _).expects().returning(0L)
      (updateResult.getModifiedCount _).expects().returning(0L)
      (mockRepository.update _).expects("123", book).returning(Future.successful(Right(updateResult)))

      whenReady(repositoryTestService.updateService("123", book)) { result =>
        result shouldBe Left(APIError.NotModified(304, "The field cannot be updated"))
      }
    }

    "return a DatabaseError when repository encounters a database error" in {
      val book = Book("123", "New Title", "Author", 100)
      val error = APIError.DatabaseError(500, "Database error")
      (mockRepository.update _).expects("123", book).returning(Future.successful(Left(error)))

      whenReady(repositoryTestService.updateService("123", book)) { result =>
        result shouldBe Left(error)
      }
    }
  }

  "RepositoryService.deleteService" should {
    "return the delete result when repository successfully deletes the book" in {
      val deleteResult = mock[DeleteResult]
      (deleteResult.getDeletedCount _).expects().returning(1L)
      (mockRepository.delete _).expects("123").returning(Future.successful(Right(deleteResult)))

      whenReady(repositoryTestService.deleteService("123")) { result =>
        result shouldBe Right(deleteResult)
      }
    }

    "return a NotModified error when repository does not delete any document" in {
      val deleteResult = mock[DeleteResult]
      (deleteResult.getDeletedCount _).expects().returning(0L)
    }
  }
}
