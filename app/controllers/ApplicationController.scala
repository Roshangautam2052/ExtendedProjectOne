package controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc.{BaseController, ControllerComponents}

@Singleton
class ApplicationController @Inject()(val controllerComponents:ControllerComponents) extends BaseController{

  def index()= TODO

  def create = TODO
  def read(id:String) = TODO
  def update(id:String) = TODO
  def delete(id:String) = TODO
}