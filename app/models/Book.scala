package models

import play.api.libs.json.{Json, OFormat}

case class Book(_id: String,
                name: String,
                description: String,
                pageCount: Int)

object Book{
  implicit val formats: OFormat[Book] = Json.format[Book]
}


