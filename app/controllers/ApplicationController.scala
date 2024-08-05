package controllers


import cats.data.EitherT
import models.{APIError, Book, DataModel}
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.OFormat.oFormatFromReadsAndOWrites
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}
import repositories.DataRepository
import services.LibraryService

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class ApplicationController @Inject()(val controllerComponents: ControllerComponents,
                                      val dataRepository: DataRepository,
                                      implicit val ec: ExecutionContext,
                                      val service: LibraryService) extends BaseController {

  def getGoogleBook(search: String, term: String): Action[AnyContent] = Action.async { implicit request =>
    service.getGoogleBook(search = search, term = term).value.map {
      case Right(book) => Ok(Json.toJson(book))
      case Left(error) => Status(error.httpResponseStatus)
    }
  }

  def index(): Action[AnyContent] = Action.async { implicit request =>
    dataRepository.index().map {
      case Right(item) => if(item.nonEmpty)Ok {
        Json.toJson(item)
      }
      else{
        NotFound {
          Json.toJson("The book list is empty")
        }
      }
      case Left(error) => Status(error.httpResponseStatus)(Json.toJson(error.upstreamStatus))
    }
  }

  def create: Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.body.validate[Book] match {
      case JsSuccess(dataModel, _) =>
        dataRepository.create(dataModel).map(_ => Created
        {
          Json.toJson(s"Successfully Created ${request.body}")
        })
      case JsError(_) => Future(BadRequest{
        Json.toJson(s"Invalid body ${request.body}")
      })
    }
  }

  def read(id: String): Action[AnyContent] = Action.async { implicit request =>
    dataRepository.read(id).map { dataModel =>
      Ok(Json.toJson(dataModel))
    } recover {
      case _: NoSuchElementException => NotFound(s"Could not find the book with id: $id")
    }
  }

  def findByName(name:String): Action[AnyContent] = Action.async { implicit request  =>
     dataRepository.searchByName(name).map {
       book => Ok(Json.toJson(book))
     }recover {
       case _: NoSuchElementException => NotFound(s"Could not find the book with the name: $name")
     }

  }
  def updateFieldValue(id:String,fieldName:String,  newValue:String): Action[AnyContent] = Action.async { implicit request =>
    dataRepository.updateField(id, fieldName, newValue).map{ result =>
      if (result.getMatchedCount > 0 && result.getModifiedCount > 0) {
        // Successfully updated
        Accepted(Json.obj("message" -> s"Field '$fieldName' updated successfully"))
      } else if (result.getMatchedCount > 0) {
        // Document exists but field was not updated
        Ok(Json.obj("message" -> s"Field '$fieldName' already has the value '$newValue' or was not modified"))
      } else {
        // Document not found
        NotFound(Json.obj("message" -> s"Document with id '$id' not found"))
      }
    }
  }

  def update(id: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.body.validate[Book] match {
      case JsSuccess(dataModel: Book, _) =>
        dataRepository.update(id, dataModel).map { result =>
          if (result.getMatchedCount > 0 && result.getModifiedCount > 0) {
            Accepted(Json.toJson(s"Updated Successfully:${request.body}"))
          }
          else {
            NotFound(Json.toJson(s"The book of given: $id not found"))
          }
        }
      case JsError(_) => Future(BadRequest{
        Json.toJson(s"The request body is invalid: ${request.body}")
      })
    }
  }

  def delete(id: String): Action[AnyContent] = Action.async { implicit request =>
    dataRepository.delete(id).map { result =>
      if (result.getDeletedCount > 0) {
        Accepted(Json.toJson(s"Successfully deleted the book with id: $id"))
      }
      else {
        NotFound(s"Could not find  the book with id: $id")
      }
    }
  }


}