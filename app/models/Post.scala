package models

import play.api.libs.json._
import java.util.UUID

case class InputPost(
    title : String,
    url : String,
    author : String
    )

case class Post(
    uuid : UUID,
    title : String,
    url : String,
    author : String
    )

object Post{
  //Json descriptors :
  implicit val postReads = Json.reads[Post]
}
