package controllers


import play.api._
import play.api.mvc._
import play.api.libs.oauth._
import play.api.libs.ws._
import com.typesafe.config.ConfigFactory
import scala.concurrent.Future
import javax.inject._
import scala.concurrent.ExecutionContext.Implicits.global

class Tumblr @Inject() (ws : WSClient) extends Controller{



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
    val KEY = ConsumerKey(ConfigFactory.load().getString("tumblr.app.key"), ConfigFactory.load().getString("tumblr.app.secret"))

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

      def isAdmin(implicit request: RequestHeader, ws : WSClient) : Future[Boolean] = {
        sessionTokenPair(request) match{
          case Some(credentials) => {
            ws.url("https://api.tumblr.com/v2/user/info")
            .sign(OAuthCalculator(Tumblr.KEY, credentials))
            .get.map(res => {
              (res.json \ "response" \ "user" \ "name").as[String] == ConfigFactory.load().getString("tumblr.app.admin")
            })
          }
          case None => Future(false)
        }

      }
    }
