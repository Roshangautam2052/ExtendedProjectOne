package models

import play.api.libs.json.{Json, OFormat}

case class InvalidDataModel(_id: String,
                     name: String,
                     description: String)

object InvalidDataModel{
  implicit val formats: OFormat[InvalidDataModel] = Json.format[InvalidDataModel]
}