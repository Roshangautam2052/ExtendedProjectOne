package repositories

import models.{APIError, Book}
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters.empty
import org.mongodb.scala.model._
import org.mongodb.scala.result.{DeleteResult, UpdateResult}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

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
) {

  def index(): Future[Either[APIError.NotFoundError, Seq[Book]]] = {
    collection.find().toFuture().map {
      case books: Seq[Book] => Right(books)
      case _ => Left(APIError.NotFoundError(404, "Books cannot be found"))
    }
  }

  def create(book: Book): Future[Either[APIError.BadAPIResponse, Book]] =
    collection
      .insertOne(book)
      .toFuture()
      .map { _ =>
        Right(book)
      }
      .recover {
        case exception => Left(APIError.BadAPIResponse(500, s"Failed to insert book ${exception.getMessage}"))
      }

  private def byID(id: String): Bson = {
    Filters.and(
      Filters.equal("_id", id)
    )
  }

  private def normalizedString(name: String): String = {
    name.strip().toLowerCase
  }

  private def byName(name: String): Bson =
    Filters.regex("name", s"^\\s*${java.util.regex.Pattern.quote(name)}\\s*$$", "i")

  def read(id: String): Future[Either[APIError.NotFoundError, Book]] =
    collection.find(byID(id)).headOption flatMap {
      case Some(book) => Future(Right(book))
      case None => Future(Left(APIError.NotFoundError(404, "Book cannot be found")))
    }

  def searchByName(name: String): Future[Either[APIError.NotFoundError, Book]] =
    collection.find(byName(normalizedString(name))).headOption() flatMap {
      case Some(book) => Future(Right(book))
      case None => Future(Left(APIError.NotFoundError(400, s"The book with $name is not found.")))
    }

  def update(id: String, book: Book): Future[Either[APIError.BadAPIResponse, UpdateResult]] = {
    collection.replaceOne(
      filter = byID(id),
      replacement = book,
      options = new ReplaceOptions().upsert(true) //What happens when we set this to false?
    ).toFuture().map { updateResult =>
      Right(updateResult)
    }.recover {
      case NonFatal(e) =>
        Left(APIError.BadAPIResponse(500, "An unexpected error occurred")) // Handle other exceptions
    }
  }

  def updateField(id: String, fieldName: String, newValue: String): Future[Either[APIError.BadAPIResponse, UpdateResult]] = {
    val update = Updates.set(fieldName, newValue)
    collection.updateOne(
      filter = byID(id),
      update = update,
      options = new UpdateOptions().upsert(false)
    ).toFuture().map { updateResult =>
      Right(updateResult)
    }.recover {
      case NonFatal(e) =>
        Left(APIError.BadAPIResponse(500, "An unexpected error occurred")) // Handle other exceptions
    }
  }


  def delete(id: String): Future[Either[APIError.BadAPIResponse, DeleteResult]] =
    collection.deleteOne(
      filter = byID(id)
    ).toFuture().map { deleteResult =>
      Right(deleteResult)
    }.recover {
      case NonFatal(e) =>
        Left(APIError.BadAPIResponse(500, "An unexpected error occurred")) // Handle other exceptions
    }

  def deleteAll(): Future[Unit] = collection.deleteMany(empty()).toFuture().map(_ => ()) //Hint: needed for tests

}
