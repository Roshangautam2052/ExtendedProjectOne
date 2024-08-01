package models

import play.api.libs.json.{Json, OFormat}

case class Book(_id: String,
                title: String,
                subtitle: String,
                pageCount: Int)

object Book{
  implicit val formats: OFormat[Book] = Json.format[Book]
}


