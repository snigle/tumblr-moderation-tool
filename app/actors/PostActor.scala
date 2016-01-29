package actors



import akka.actor._
import play.api.libs.json.{JsArray, Json}
import play.api.libs.ws.{WS, WSClient}
import play.api.mvc.Action
import java.util.UUID
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.Play.current
import scala.collection.immutable._
import play.api.libs.oauth._
import play.api.Play.current
import play.api.Mode

import controllers._
import models._
/**
 * Created by eisti on 1/15/16.
 */

object PostActor {
  trait MessageType
  object GetPosts extends MessageType
  case class AddPost(post : Post) extends MessageType
  case class DeletePost(post : UUID) extends MessageType
  case class ValidatePost(credentials : RequestToken, post : UUID) extends MessageType
}

class PostActor(ws : WSClient) extends Actor {
  var posts = List[Post]()
  def receive = {
      case PostActor.AddPost(post) => {
        posts = post :: posts
        println(posts)
      }
      case PostActor.DeletePost(uuid) => {
        posts = posts.filterNot(p => p.uuid==uuid)
        sender ! "ok"
      }
      case PostActor.ValidatePost(credentials, uuid) => {
        val state = current.mode match {
            case Mode.Dev => Seq("draft")
            case Mode.Prod => Seq("published")
        }
        val post = posts.find(p => p.uuid == uuid).get
        posts = posts.filterNot(p => p.uuid==uuid)
        ws.url("https://api.tumblr.com/v2/blog/prenezlapose/post")
        .sign(OAuthCalculator(Tumblr.KEY, credentials))
        .post(Map(
          "type" -> Seq("text"),
          "title" -> Seq(post.title),
          "body" -> Seq("<img src=\""+post.url+"\"/><br />by "+post.author),
          "state" -> Seq("draft"),
          "native_inline_images" -> Seq("true")
        )
        ).map(response => {
        })
        sender ! "ok"
      }
      case PostActor.GetPosts => {
        sender ! posts
      }
    }

}
