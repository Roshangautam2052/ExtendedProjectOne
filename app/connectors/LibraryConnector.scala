package connectors


import javax.inject.Inject
import play.api.libs.json.{JsError, JsSuccess, OFormat}
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}

class LibraryConnector @Inject()(ws: WSClient) {

  def get[Response](url: String)(implicit rds: OFormat[Response], ec: ExecutionContext): Future[Response] = {
    val request = ws.url(url)
    val response = request.get()
    response.map { response =>
      println(response.json) // Log the JSON response

      response.json.validate[Response] match {
        case JsSuccess(responseData, _) => responseData
        case JsError(errors) =>
          // Handle errors, e.g., log them, return a default value, etc.
          println(s"Failed to parse JSON: $errors")
          throw new RuntimeException("Invalid JSON response")
      }
    }

  }
}
