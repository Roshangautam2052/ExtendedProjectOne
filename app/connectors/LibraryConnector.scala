package connectors


import javax.inject.Inject
import play.api.libs.json.{JsError, JsSuccess, OFormat}
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}

class LibraryConnector @Inject()(ws: WSClient) {

  def get[Response](url: String)(implicit rds: OFormat[Response], ec: ExecutionContext): Future[Response] = {
    val request = ws.url(url)
    val response = request.get()
    response.map {
      result =>
        result.json.as[Response]
    }
  }
}
