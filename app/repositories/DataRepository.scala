package repositories

import com.google.inject.ImplementedBy
import models.{APIError, Book}
import org.mongodb.scala.FindObservable
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model._
import org.mongodb.scala.result.{DeleteResult, UpdateResult}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@ImplementedBy(classOf[DataRepository])
trait Repository {
  def index(): Future[Either[APIError, Seq[Book]]]

  def create(book: Book): Future[Either[APIError, Book]]

  def read(id: String): Future[Either[APIError, Book]]

  def searchByName(name: String): Future[Either[APIError, Book]]

  def update(id: String, book: Book): Future[Either[APIError, UpdateResult]]

  def updateField(id: String, fieldName: String, newValue: String): Future[Either[APIError, UpdateResult]]

  def delete(id: String): Future[Either[APIError, DeleteResult]]
}

@Singleton
class DataRepository @Inject()(
                                mongoComponent: MongoComponent
                              )(implicit ec: ExecutionContext) extends PlayMongoRepository[Book](
  collectionName = "dataModels",
  mongoComponent = mongoComponent,
  domainFormat = Book.formats,
  indexes = Seq(IndexModel(
    Indexes.ascending("_id")
  )),
  replaceIndexes = false
) with Repository {

  def index(): Future[Either[APIError, Seq[Book]]] = {
    collection.find().toFuture().map {
      case book: Seq[Book] => Right(book)
    }.recover {
      case e: Exception =>
        Left(APIError.DatabaseError(500, s"Database error occurred: ${e.getMessage}"))
    }
  }


  def create(book: Book): Future[Either[APIError, Book]] =
    collection
      .insertOne(book)
      .toFuture()
      .map { result =>
        if (result.wasAcknowledged()) Right(book)
        else Left(APIError.BadAPIResponse(500, "Failed to add book"))

      }.recover {
        case exception: Throwable => Left(APIError.DatabaseError(500, s"Failed to insert book due to ${exception.getMessage}"))
      }

  private def byID(id: String): Bson = {
    Filters.and(
      Filters.equal("_id", id)
    )
  }

  private def byName(name: String): Bson =
    Filters.regex("name", s"^\\s*${java.util.regex.Pattern.quote(name)}\\s*$$", "i")

  def read(id: String): Future[Either[APIError, Book]] = {
    val findObservable: FindObservable[Book] = collection.find(byID(id))
    // Convert FindObservable to Future
    findObservable.toFuture().map { books =>
      books.headOption match {
        case Some(book) => Right(book)
        case None => Left(APIError.NotFoundError(404, "Book is not found."))
      }
    }.recover {
      case e: Throwable => Left(APIError.DatabaseError(500, s"Failed to retrieve book due to ${e.getMessage}"))
    }
  }

  def searchByName(name: String): Future[Either[APIError, Book]] = {
    val findObservable: FindObservable[Book] = collection.find(byName(name))
    findObservable.toFuture().map { book =>
      book.headOption match {
        case Some(book) => Right(book)
        case None => Left(APIError.NotFoundError(404, s"The book with $name is not found."))
      }
    }.recover {
      case e: Throwable => Left(APIError.DatabaseError(500, s"Failed to insert book due to ${e.getMessage}"))
    }
  }

  def update(id: String, book: Book): Future[Either[APIError, UpdateResult]] = {
    collection.replaceOne(
      filter = byID(id),
      replacement = book,
      options = new ReplaceOptions().upsert(true) //What happens when we set this to false?
    ).toFuture().map { updateResult =>
      Right(updateResult)
    }.recover {
      case e: Throwable =>
        Left(APIError.DatabaseError(500, s"An unexpected error occurred ${e.getMessage}")) // Handle other exceptions
    }
  }

  def updateField(id: String, fieldName: String, newValue: String): Future[Either[APIError.DatabaseError, UpdateResult]] = {
    val update = Updates.set(fieldName, newValue)
    collection.updateOne(
        filter = byID(id),
        update = update,
        options = new UpdateOptions().upsert(false)
      ).toFuture().map { updateResult =>
        Right(updateResult)
      }
      .recover {
        case e: Throwable =>
          Left(APIError.DatabaseError(500, s"An unexpected error occurred ${e.getMessage}")) // Handle other exceptions
      }
  }


  def delete(id: String): Future[Either[APIError.DatabaseError, DeleteResult]] =
    collection.deleteOne(
        filter = byID(id)
      ).toFuture().map { deleteResult =>
        Right(deleteResult)
      }
      .recover {
        case NonFatal(e) =>
          Left(APIError.DatabaseError(500, s"An unexpected error occurred ${e.getMessage}")) // Handle other exceptions
      }

  //def deleteAll(): Future[Unit] = collection.deleteMany(empty()).toFuture().map(_ => ()) //Hint: needed for tests

}
