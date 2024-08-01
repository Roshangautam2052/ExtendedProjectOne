package services

import com.google.inject.Singleton
import connectors.LibraryConnector
import models.Book

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class LibraryService @Inject()(connector: LibraryConnector) {
  def getGoogleBook(urlOverride: Option[String] = None, search: String, term: String)(implicit ec: ExecutionContext): Future[Book] =
    connector.get[Book](urlOverride.getOrElse(s"https://www.googleapis.com/books/v1/volumes?q=$search%$term"))

}



