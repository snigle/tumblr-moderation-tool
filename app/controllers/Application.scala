package controllers

import play.api._
import play.api.mvc._
import akka.actor._
import javax.inject._
import play.api.libs.ws._
import scala.concurrent.Future
import play.api.data._
import play.api.data.Forms._
import akka.pattern._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import java.util.UUID
import play.api.Play.current
import play.api.Mode
import play.api.libs.oauth._

import actors._
import models._

class Application @Inject() (system: ActorSystem, ws : WSClient) extends Controller {

  val postActor = system.actorOf(Props[PostActor](new PostActor(ws)))
  def index = Action { implicit request =>
    // implicit val newFlash = request.flash
    Ok(views.html.form())
  }

  def delete(uuid : String) = Action.async{implicit request =>
    Tumblr.sessionTokenPair(request) match {
      case Some(credentials) => {
        ask(postActor, PostActor.DeletePost(UUID.fromString(uuid)))(1 seconds).mapTo[String].map(posts =>
        {
          Redirect("/admin").flashing("succes"->"Le post a été supprimé")
        })
      }
      case _ => Future.successful(Redirect(routes.Tumblr.authenticate))
  }}

  def validate(uuid : String) = Action.async(parse.form(postForm)){ implicit request =>
    Tumblr.sessionTokenPair(request) match {
      case Some(credentials) => {
        val post = Post(UUID.fromString(uuid),request.body.title, request.body.url, request.body.author)
        val state = current.mode match {
            case Mode.Dev => Seq("draft")
            case Mode.Prod => Seq("published")
        }
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
          if(response.status==201){
            postActor ! PostActor.DeletePost(post.uuid)
            Redirect("/admin").flashing("success"->"Le post a été ajouté au compte Tumblr")
          }else if(response.status == 401){
            Redirect("/admin").withNewSession.flashing("error"->"Login session timeout")
          }else{
            Redirect("/admin").flashing("error"->("Unkown error, tumblr api respond with status "+response.status))
          }
        })

      }
      case _ => Future.successful(Redirect(routes.Tumblr.authenticate))
    }



  }

  def pendingPosts = Action.async { implicit request =>
    Tumblr.sessionTokenPair(request) match {
      case Some(credentials) => {
        ask(postActor, PostActor.GetPosts)(1 seconds).mapTo[List[Post]].map(posts =>
        {
          Ok(views.html.moderate(posts))
        })
      }
      case _ => Future.successful(Redirect(routes.Tumblr.authenticate))
    }
  }


  val postForm = Form(
  mapping(
    "title" -> nonEmptyText,
    "url" -> nonEmptyText,
    "author" -> nonEmptyText
  )(InputPost.apply)(InputPost.unapply)
)
  def posts = Action.async(parse.form(postForm)) { implicit request =>
    val post = request.body
    if(!post.url.endsWith(".gif")){
      Future(Redirect("/").flashing("error"->("L'url doit se terminer par .gif")))
    }
    else{
      ws.url(post.url).get.map(r => {
        if(r.status != 200 || r.header("Content-Type") != Some("image/gif")){
          Redirect("/").flashing("error"->("L'url ne pointe pas vers un gif"))
        }
        else {
          postActor ! PostActor.AddPost(Post(UUID.randomUUID(),post.title,post.url,post.author))
          Redirect("/").flashing("success"->"Post ajouté")
        }
      })
    }


  }

}
