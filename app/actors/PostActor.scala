package actors



import akka.actor._
import akka.persistence._
import play.api.libs.json.{JsArray, Json}
import play.api.libs.ws.{WS, WSClient}
import play.api.mvc.Action
import java.util.UUID
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.Play.current
import scala.collection.immutable._



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
}

case class Posts(var value : List[Post] = Nil)

class PostActor(ws : WSClient) extends  PersistentActor {
  override def persistenceId = "sample-id-1"

  var posts = new Posts()

  def receiveRecover = {
    case SnapshotOffer(_, snapshot: Posts) => posts = snapshot
  }
  def receiveCommand = {
    case PostActor.AddPost(post) => {
      posts.value = post :: posts.value
      self ! "snap"
    }
    case PostActor.DeletePost(uuid) => {
      posts.value = posts.value.filterNot(p => p.uuid==uuid)
      sender ! "ok"
      self ! "snap"
    }
    case PostActor.GetPosts => {
      sender ! posts.value
    }
    case "snap" => {
      deleteSnapshot(snapshotSequenceNr)
      saveSnapshot(posts)
    }
  }


  // val receiveRecover: Receive = {
  //   case evt: PostActor.MessageType   => self ! evt
  //   case SnapshotOffer(_, snapshot: List[Post]) => posts = snapshot
  // }


  // val receiveCommand: Receive = {
    // case Cmd(data) =>
    //   persist(Evt(s"${data}-${numEvents}"))(updateState)
    //   persist(Evt(s"${data}-${numEvents + 1}")) { event =>
    //     updateState(event)
    //     context.system.eventStream.publish(event)
    //   }
  //   case "snap"  => saveSnapshot(posts)
  //   case "print" => println(posts)
  // }

}
