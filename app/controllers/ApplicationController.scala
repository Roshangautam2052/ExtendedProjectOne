package controllers

import akka.actor.TypedActor.dispatcher
import models.DataModel
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}

import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents, Request}
import repositories.DataRepository

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}


@Singleton
class ApplicationController @Inject()(val controllerComponents: ControllerComponents,
                                      val dataRepository: DataRepository, val ec: ExecutionContext) extends BaseController {

  def index(): Action[AnyContent] = Action.async { implicit request =>
    dataRepository.index().map {
      case Right(item: Seq[DataModel]) => Ok {
        Json.toJson(item)
      }
      case Left(error) => Status(error)(Json.toJson("Unable to find any books"))
    }
  }

  def create: Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.body.validate[DataModel] match {
      case JsSuccess(dataModel, _) =>
        dataRepository.create(dataModel).map(_ => Created)
      case JsError(_) => Future(BadRequest)
    }
  }

  def read(id: String): Action[AnyContent] = Action.async { implicit request =>
    dataRepository.read(id).map {
      case dataModel: DataModel => Ok {
        Json.toJson(dataModel)
      }
      case _ => Status(NOT_FOUND)(s"Successfully deleted the book with id: $id")
    }

  }

  def update(id: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.body.validate[DataModel] match {
      case JsSuccess(dataModel, _) =>
        dataRepository.update(id, dataModel).map { result =>
          if (result.getMatchedCount > 0 && result.getModifiedCount > 0) {
            Accepted(Json.toJson(request.body))
          }
          else {
            NotFound(Json.toJson(id))
          }
        }
      case JsError(_) => Future(BadRequest)
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