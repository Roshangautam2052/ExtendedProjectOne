package controllers


import models.Book
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.OFormat.oFormatFromReadsAndOWrites
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}
import services.{LibraryService, RepositoryService}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class ApplicationController @Inject()(val controllerComponents: ControllerComponents,
                                      val repoService: RepositoryService,
                                      implicit val ec: ExecutionContext,
                                      val service: LibraryService) extends BaseController {

  def getGoogleBook(search: String, term: String): Action[AnyContent] = Action.async { implicit request =>
    service.getGoogleBook(search = search, term = term).value.map {
      case Right(book) => Ok(Json.toJson(book))
      case Left(error) => Status(error.httpResponseStatus)
    }
  }

  def index(): Action[AnyContent] = Action.async { implicit request =>
    repoService.indexService().map {
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
      case JsSuccess(book, _) =>
        repoService.createService(book).map(_ => Created
        {
          Json.toJson(s"Successfully Created ${request.body}")
        })
      case JsError(_) => Future(BadRequest{
        Json.toJson(s"Invalid body ${request.body}")
      })
    }
  }

  def read(id: String): Action[AnyContent] = Action.async { implicit request =>
    repoService.readService(id).map {
      case Right(book) => Ok(Json.toJson(book))
      case Left(error) => NotFound(Json.obj("error" -> error.reason))
    }
  }

  def findByName(name:String): Action[AnyContent] = Action.async { implicit request =>
    repoService.findByNameService(name).map {
      case Right(book) => Ok(Json.toJson(book))
      case Left(error) => NotFound(Json.toJson("error" -> error.reason))
    }
  }

  def updateFieldValue(id:String,fieldName:String,  newValue:String): Action[AnyContent] = Action.async { implicit request =>
    repoService.updateFieldValueService(id, fieldName, newValue).map {
      case Right(result) => if (result.getMatchedCount > 0 && result.getModifiedCount > 0) {
        Accepted(Json.obj("message" -> s"Field '$fieldName' updated successfully"))
      } else {
        Ok(Json.obj("message" -> s"Field '$fieldName' already has the value '$newValue' or was not modified"))}
      case Left(error) => NotFound(Json.toJson("error" -> error.reason))
    }
  }

  def update(id: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.body.validate[Book] match {
      case JsSuccess(book: Book, _) =>
        repoService.updateService(id,book).map {
          case Right(result) => if (result.getMatchedCount > 0 && result.getModifiedCount > 0) {
            Accepted(Json.obj("message" -> s"Field with ${id} updated successfully"))
          } else {
            Ok(Json.obj("message" -> s"Field ${id} already has the value or was not modified"))}
          case Left(error) => NotFound(Json.toJson("error" -> error.reason))
        }
      case JsError(_) => Future(BadRequest{
        Json.toJson(s"The request body is invalid: ${request.body}")
      })
    }
  }

  def delete(id: String): Action[AnyContent] = Action.async { implicit request =>
    repoService.deleteService(id).map {
      case Right(deletedResult) => if (deletedResult.getDeletedCount > 0) {
        Accepted(Json.toJson(s"Successfully deleted the book with id: $id"))
      }
      else {
        NotFound(s"Could not find  the book with id: $id")
      }
      case Left(error) => NotFound(Json.toJson("error" -> error.reason))
    }
  }


}