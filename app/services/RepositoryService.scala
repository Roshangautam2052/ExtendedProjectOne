package services

import models.{APIError, Book}
import org.mongodb.scala.result.{DeleteResult, UpdateResult}
import repositories.{DataRepository, Repository}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RepositoryService @Inject()(dataRepository: Repository)(implicit ec: ExecutionContext) {

  private def normalizeString(name: String): String = {
    name.strip().toLowerCase
  }

  def indexService(): Future[Either[APIError, Seq[Book]]] = {
    dataRepository.index().map {
      case Right(books) if books.nonEmpty => Right(books)
      case Left(APIError.DatabaseError(code, message)) =>
        Left(APIError.DatabaseError(code, message))
    }
  }


  def createService(book: Book): Future[Either[APIError, Book]] = {
    dataRepository.create(book).map {
      case Right(createdBook) => Right(createdBook)
      case Left(APIError.DatabaseError(code, message)) => Left(APIError.DatabaseError(code, message))
      case Left(APIError.BadAPIResponse(code, message)) => Left(APIError.BadAPIResponse(code, message))
    }

  }

  def readService(id: String): Future[Either[APIError, Book]] = {
    dataRepository.read(id).map {
      case Right(book) => Right(book)
      case Left(APIError.DatabaseError(code, message)) => Left(APIError.DatabaseError(code, message))
      case Left(APIError.NotFoundError(code, message)) => Left(APIError.NotFoundError(code, message))
    }
  }


  def findByNameService(name: String): Future[Either[APIError, Book]] = {
    dataRepository.searchByName(normalizeString(name)).map {
      case Right(book) => Right(book)
      case Left(APIError.DatabaseError(code, message)) => Left(APIError.DatabaseError(code, message))
      case Left(APIError.NotFoundError(code, message)) => Left(APIError.NotFoundError(code, message))
    }
  }

  def updateFieldValueService(id: String, fieldName: String, newValue: String): Future[Either[APIError, UpdateResult]] = {
    dataRepository.updateField(id, fieldName, newValue).map {
      case Right(updateResult) => if (updateResult.getMatchedCount > 0 && updateResult.getModifiedCount > 0)
        Right(updateResult) else Left(APIError.NotModified(304, "The field cannot be updated"))
      case Left(APIError.DatabaseError(code, message)) => Left(APIError.DatabaseError(code, message))
    }
  }


  def updateService(id: String, book: Book): Future[Either[APIError, UpdateResult]] = {
    dataRepository.update(id, book).map {
      case Right(updatedResult) => if (updatedResult.getMatchedCount > 0 && updatedResult.getModifiedCount > 0)
        Right(updatedResult) else Left(APIError.NotModified(304, "The field cannot be updated"))
      case Left(APIError.DatabaseError(code, message)) => Left(APIError.DatabaseError(code, message))
    }
  }

  def deleteService(id: String): Future[Either[APIError, DeleteResult]] = {
    dataRepository.delete(id).map {
      case Right(deletedResult: DeleteResult) => if (deletedResult.getDeletedCount > 0)
        Right(deletedResult) else Left(APIError.NotModified(304, "The entry cannot be deleted"))
      case Left(APIError.DatabaseError(code, message)) => Left(APIError.DatabaseError(code, message))
    }

  }


}
