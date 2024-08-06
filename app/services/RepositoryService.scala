package services

import cats.data.EitherT
import models.{APIError, Book}
import org.mongodb.scala.result
import org.mongodb.scala.result.{DeleteResult, UpdateResult}
import repositories.DataRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class RepositoryService @Inject()(dataRepository: DataRepository){

  def indexService():Future[Either[APIError.NotFoundError, Seq[Book]]] ={
     dataRepository.index()
  }

  def createService(book:Book):Future[Either[APIError.BadAPIResponse, Book]] = {
     dataRepository.create(book)

  }

  def readService(id:String):Future[Either[APIError.NotFoundError, Book]] = {
    dataRepository.read(id)
  }


  def findByNameService(name:String):Future[Either[APIError.NotFoundError, Book]] = {
    dataRepository.searchByName(name)
  }

  def updateFieldValueService(id:String, fieldName:String, newValue:String):Future[Either[APIError.BadAPIResponse, UpdateResult]] = {
    dataRepository.updateField(id, fieldName, newValue)
  }


  def updateService(id:String, book:Book):Future[Either[APIError.BadAPIResponse, UpdateResult]] = {
    dataRepository.update(id, book)
  }

  def deleteService(id:String):Future[Either[APIError.BadAPIResponse, DeleteResult]] = {
     dataRepository.delete(id)
  }


}
