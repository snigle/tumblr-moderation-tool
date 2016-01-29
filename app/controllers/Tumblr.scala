package controllers


import play.api._
import play.api.mvc._
import play.api.libs.oauth._


class Tumblr extends Controller{



  def authenticate = Action { request =>
    request.getQueryString("oauth_verifier").map { verifier =>
      val tokenPair = Tumblr.sessionTokenPair(request).get
      // We got the verifier; now get the access token, store it and back to index
      Tumblr.TUMBLR.retrieveAccessToken(tokenPair, verifier) match {
        case Right(t) => {
          // We received the authorized tokens in the OAuth object - store it before we proceed
          Redirect(routes.Application.index).withSession("token" -> t.token, "secret" -> t.secret)
        }
        case Left(e) => throw e
      }
    }.getOrElse(
      Tumblr.TUMBLR.retrieveRequestToken("http://localhost:9000/auth") match {
        case Right(t) => {
          // We received the unauthorized tokens in the OAuth object - store it before we proceed
          Redirect(Tumblr.TUMBLR.redirectUrl(t.token)).withSession("token" -> t.token, "secret" -> t.secret)
        }
        case Left(e) => throw e
      })
  }
}

object Tumblr extends Controller {
  val KEY = ConsumerKey("XXX", "XXX")

  val TUMBLR = OAuth(ServiceInfo(
    "https://www.tumblr.com/oauth/request_token",
    "https://www.tumblr.com/oauth/access_token",
    "https://www.tumblr.com/oauth/authorize", KEY),
    true)





  def sessionTokenPair(implicit request: RequestHeader): Option[RequestToken] = {
    for {
      token <- request.session.get("token")
      secret <- request.session.get("secret")
    } yield {
      RequestToken(token, secret)
    }
  }
}
