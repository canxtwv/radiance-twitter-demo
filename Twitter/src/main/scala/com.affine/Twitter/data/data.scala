package com.affine.Twitter

import java.util.UUID
import io.surfkit.typebus._
import io.surfkit.typebus.event.DbAccessor
import io.surfkit.typebus.entity.EntityDb
import scala.concurrent.Future

package object data {

  final case class Tweet(favorite_count: Int = 0,
                         favorited: Boolean = false,
                         filter_level: Option[String] = None,
                         id: String,
                         id_str: String,
                         lang: Option[String] = None,
                         possibly_sensitive: Boolean = false,
                         quoted_status_id: Option[String] = None,
                         quoted_status_id_str: Option[String] = None,
                         scopes: Map[String, Boolean] = Map.empty,
                         retweet_count: Int = 0,
                         retweeted: Boolean = false,
                         source: String,
                         text: String,
                         truncated: Boolean = false)

  sealed trait TwitteruserCommand
  case class GetTwitteruserCommand(user: String) extends TwitteruserCommand
  case class AddTwitterStream(user: String, count: Int = 5) extends TwitteruserCommand
  case class AddTweets(user: String, tweets: Seq[Tweet]) extends TwitteruserCommand
  case class GetTwitteruserEntityState(id: String) extends TwitteruserCommand with DbAccessor

  sealed trait TwitteruserEvent
  case class TwitteruserState(tweets: Map[String, Tweet])
  case class Twitteruser(user: String, tweets: Seq[Tweet])

  case class RecentTweets(user: String, tweets: Seq[Tweet]) extends TwitteruserEvent
  case class TwitterStreamAdded(user: String, count: Int) extends TwitteruserEvent
  case class TweetsAdded(user: String, tweets: Seq[Tweet]) extends TwitteruserEvent

  object Implicits extends AvroByteStreams{
    implicit val TwitteruserRW = Typebus.declareType[Twitteruser, AvroByteStreamReader[Twitteruser], AvroByteStreamWriter[Twitteruser]]
    implicit val getTwitteruserRW = Typebus.declareType[GetTwitteruserCommand, AvroByteStreamReader[GetTwitteruserCommand], AvroByteStreamWriter[GetTwitteruserCommand]]

    implicit val AddTwitterStreamRW = Typebus.declareType[AddTwitterStream, AvroByteStreamReader[AddTwitterStream], AvroByteStreamWriter[AddTwitterStream]]
    implicit val recentTweetsRW = Typebus.declareType[RecentTweets, AvroByteStreamReader[RecentTweets], AvroByteStreamWriter[RecentTweets]]
    implicit val twitterStreamAddedRW = Typebus.declareType[TwitterStreamAdded, AvroByteStreamReader[TwitterStreamAdded], AvroByteStreamWriter[TwitterStreamAdded]]
    implicit val tweetsAddedRW = Typebus.declareType[TweetsAdded, AvroByteStreamReader[TweetsAdded], AvroByteStreamWriter[TweetsAdded]]

    implicit val TwitteruserStateRW = Typebus.declareType[TwitteruserState, AvroByteStreamReader[TwitteruserState], AvroByteStreamWriter[TwitteruserState]]
    implicit val GetTwitteruserEntityStateRW = Typebus.declareType[GetTwitteruserEntityState, AvroByteStreamReader[GetTwitteruserEntityState], AvroByteStreamWriter[GetTwitteruserEntityState]]
  }

  trait TwitteruserDatabase extends EntityDb[TwitteruserState]{
    def addTweets(x: AddTweets): Future[TweetsAdded]
    def getTwitteruser(x: GetTwitteruserCommand): Future[Twitteruser]
  }
}



