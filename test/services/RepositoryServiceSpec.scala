package services

import models.{APIError, Book}
import org.mongodb.scala.result.{DeleteResult, UpdateResult}
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import repositories.DataRepository

import scala.concurrent.{ExecutionContext, Future}

class RepositoryServiceSpec extends AnyWordSpec with Matchers with MockFactory with ScalaFutures with GuiceOneServerPerSuite {

  val mockRepository: DataRepository = mock[DataRepository]
  implicit val executionContext: ExecutionContext = app.injector.instanceOf[ExecutionContext]
  val repositoryTestService = new RepositoryService(mockRepository)

  "RepositoryService.indexService" should {
    "return a sequence of books when repository returns books" in {
      val books = Seq(Book("123", "Test Book", "Author",100))
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
    "return the created book when repository successfully creates a book" in {
      val book = Book("123", "Test Book", "Author", 100)
      (mockRepository.create _).expects(book).returning(Future.successful(Right(book)))

      whenReady(repositoryTestService.createService(book)) { result =>
        result shouldBe Right(book)
      }
    }

    "return a DatabaseError when repository fails to create a book" in {
      val book = Book("123", "Test Book", "Author",200)
      val error = APIError.DatabaseError(500, "Database error")
      (mockRepository.create _).expects(book).returning(Future.successful(Left(error)))

      whenReady(repositoryTestService.createService(book)) { result =>
        result shouldBe Left(error)
      }
    }
  }

  "RepositoryService#readService" should {
    "return the book when repository successfully retrieves the book" in {
      val book = Book("123", "Test Book", "Author",300)
      (mockRepository.read _).expects("123").returning(Future.successful(Right(book)))

      whenReady(repositoryTestService.readService("123")) { result =>
        result shouldBe Right(book)
      }
    }

    "return a NotFoundError when the book is not found" in {
      val error = APIError.NotFoundError(404, "Book not found")
      (mockRepository.read _).expects("123").returning(Future.successful(Left(error)))

      whenReady(repositoryTestService.readService("123")) { result =>
        result shouldBe Left(error)
      }
    }

    "return a DatabaseError when repository encounters a database error" in {
      val error = APIError.DatabaseError(500, "Database error")
      (mockRepository.read _).expects("123").returning(Future.successful(Left(error)))

      whenReady(repositoryTestService.readService("123")) { result =>
        result shouldBe Left(error)
      }
    }
  }

  "RepositoryService#findByNameService" should {
    "return the book when repository successfully finds a book by name" in {
      val book = Book("123", "Test Book", "Author",150)
      (mockRepository.searchByName _).expects("Test Book").returning(Future.successful(Right(book)))

      whenReady(repositoryTestService.findByNameService("Test Book")) { result =>
        result shouldBe Right(book)
      }
    }

    "return a NotFoundError when no book is found by the given name" in {
      val error = APIError.NotFoundError(404, "Book not found")
      (mockRepository.searchByName _).expects("Test Book").returning(Future.successful(Left(error)))

      whenReady(repositoryTestService.findByNameService("Test Book")) { result =>
        result shouldBe Left(error)
      }
    }

    "return a DatabaseError when repository encounters a database error" in {
      val error = APIError.DatabaseError(500, "Database error")
      (mockRepository.searchByName _).expects("Test Book").returning(Future.successful(Left(error)))

      whenReady(repositoryTestService.findByNameService("Test Book")) { result =>
        result shouldBe Left(error)
      }
    }
  }

  "RepositoryService#updateFieldValueService" should {
    "return UpdateResult when repository successfully updates the field" in {
      val updateResult = mock[UpdateResult]
      (updateResult.getMatchedCount _).expects().returning(1)
      (updateResult.getModifiedCount _).expects().returning(1)
      (mockRepository.updateField _).expects("123", "title", "New Title").returning(Future.successful(Right(updateResult)))

      whenReady(repositoryTestService.updateFieldValueService("123", "title", "New Title")) { result =>
        result shouldBe Right(updateResult)
      }
    }

    "return NotModified error when the field was not updated" in {
      val updateResult = mock[UpdateResult]
      (updateResult.getMatchedCount _).expects().returning(0)
      (updateResult.getModifiedCount _).expects().returning(0)
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

  "RepositoryService#deleteService" should {
    "return DeleteResult when repository successfully deletes the entry" in {
      val deleteResult = mock[DeleteResult]
      (deleteResult.getDeletedCount _).expects().returning(1)
      (mockRepository.delete _).expects("123").returning(Future.successful(Right(deleteResult)))

      whenReady(repositoryTestService.deleteService("123")) { result =>
        result shouldBe Right(deleteResult)
      }
    }

    "return NotModified error when the entry was not deleted" in {
      val deleteResult = mock[DeleteResult]
      (deleteResult.getDeletedCount _).expects().returning(0)
      (mockRepository.delete _).expects("123").returning(Future.successful(Right(deleteResult)))

      whenReady(repositoryTestService.deleteService("123")) { result =>
        result shouldBe Left(APIError.NotModified(304, "The entry cannot be deleted"))
      }
    }

    "return a DatabaseError when repository encounters a database error" in {
      val error = APIError.DatabaseError(500, "Database error")
      (mockRepository.delete _).expects("123").returning(Future.successful(Left(error)))

      whenReady(repositoryTestService.deleteService("123")) { result =>
        result shouldBe Left(error)
      }
    }
  }
}
