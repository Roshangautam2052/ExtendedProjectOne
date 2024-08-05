package repositories

import models.{APIError, Book}
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters.empty
import org.mongodb.scala.model._
import org.mongodb.scala.result
import org.mongodb.scala.result.UpdateResult
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

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

  def index(): Future[Either[APIError.BadAPIResponse, Seq[Book]]] =
    collection.find().toFuture().map {
      case books => Right(books)
      case _ => Left(APIError.BadAPIResponse(404, "Books cannot be found"))
    }

  def create(book: Book): Future[Book] =
    collection
      .insertOne(book)
      .toFuture()
      .map(_ => book)

  private def byID(id: String): Bson = {
    Filters.and(
      Filters.equal("_id", id)
    )
  }
    private def normalizedString(name:String): String = {
      name.strip().toLowerCase
    }

  private def byName(name: String): Bson =
      Filters.regex("name", s"^\\s*${java.util.regex.Pattern.quote(name)}\\s*$$", "i")

  def read(id: String): Future[Book] =
    collection.find(byID(id)).headOption flatMap {
      case Some(data) => Future(data)
      case None => Future.failed(new NoSuchElementException(s"Not data found"))
    }

  def searchByName(name:String):Future[Book] =
    collection.find(byName(normalizedString(name))).headOption() flatMap{
      case Some(data) => Future(data)
      case None => Future.failed(new NoSuchElementException(s"Book with the name not found"))
    }

  def update(id: String, book: Book): Future[result.UpdateResult] =
    collection.replaceOne(
      filter = byID(id),
      replacement = book,
      options = new ReplaceOptions().upsert(true) //What happens when we set this to false?
    ).toFuture()

  def updateField(id:String, fieldName:String, newValue:String):Future[UpdateResult]= {
    val update = Updates.set(fieldName, newValue)
    collection.updateOne(
      filter = byID(id),
      update = update,
      options = new UpdateOptions().upsert(false)
    ).toFuture()
  }


  def delete(id: String): Future[result.DeleteResult] =
    collection.deleteOne(
      filter = byID(id)
    ).toFuture()

  def deleteAll(): Future[Unit] = collection.deleteMany(empty()).toFuture().map(_ => ()) //Hint: needed for tests

}
