/** MACHINE-GENERATED FROM AVRO SCHEMA. DO NOT EDIT DIRECTLY */

package api.com.affine.Twitter

import akka.actor.ActorSystem
import scala.concurrent.Future
import io.surfkit.typebus._
import io.surfkit.typebus.event.EventMeta
import io.surfkit.typebus.bus.Publisher
import io.surfkit.typebus.client._
import io.surfkit.typebus.event.{ServiceIdentifier, ServiceException}

package object data{

   final case class Twitteruser(user: java.lang.String, tweets: scala.collection.Seq[api.com.affine.Twitter.data.Tweet])
   final case class AddTwitterStream(user: java.lang.String, count: scala.Int = 5) extends api.com.affine.Twitter.data.TwitteruserCommand
   final case class Tweet(favorite_count: scala.Int = 0, favorited: scala.Boolean = false, filter_level: scala.Option[java.lang.String] = None, id: java.lang.String, id_str: java.lang.String, lang: scala.Option[java.lang.String] = None, possibly_sensitive: scala.Boolean = false, quoted_status_id: scala.Option[java.lang.String] = None, quoted_status_id_str: scala.Option[java.lang.String] = None, scopes: scala.collection.immutable.Map[java.lang.String,scala.Boolean] = Map.empty, retweet_count: scala.Int = 0, retweeted: scala.Boolean = false, source: java.lang.String, text: java.lang.String, truncated: scala.Boolean = false)
   final case class TweetsAdded(user: java.lang.String, tweets: scala.collection.Seq[api.com.affine.Twitter.data.Tweet]) extends api.com.affine.Twitter.data.TwitteruserEvent
   sealed trait TwitteruserEvent{

   }
   final case class GetTwitteruserCommand(user: java.lang.String) extends api.com.affine.Twitter.data.TwitteruserCommand
   final case class RecentTweets(user: java.lang.String, tweets: scala.collection.Seq[api.com.affine.Twitter.data.Tweet]) extends api.com.affine.Twitter.data.TwitteruserEvent
   final case class TwitterStreamAdded(user: java.lang.String, count: scala.Int) extends api.com.affine.Twitter.data.TwitteruserEvent
   sealed trait TwitteruserCommand

   object Implicits extends AvroByteStreams{

      implicit val TweetsAddedReader = new AvroByteStreamReader[TweetsAdded]
      implicit val TweetsAddedWriter = new AvroByteStreamWriter[TweetsAdded]
            
      implicit val TweetReader = new AvroByteStreamReader[Tweet]
      implicit val TweetWriter = new AvroByteStreamWriter[Tweet]
            
      implicit val TwitterStreamAddedReader = new AvroByteStreamReader[TwitterStreamAdded]
      implicit val TwitterStreamAddedWriter = new AvroByteStreamWriter[TwitterStreamAdded]
            
      implicit val TwitteruserEventReader = new AvroByteStreamReader[TwitteruserEvent]
      implicit val TwitteruserEventWriter = new AvroByteStreamWriter[TwitteruserEvent]
            
      implicit val RecentTweetsReader = new AvroByteStreamReader[RecentTweets]
      implicit val RecentTweetsWriter = new AvroByteStreamWriter[RecentTweets]
            
      implicit val TwitteruserReader = new AvroByteStreamReader[Twitteruser]
      implicit val TwitteruserWriter = new AvroByteStreamWriter[Twitteruser]
            
      implicit val AddTwitterStreamReader = new AvroByteStreamReader[AddTwitterStream]
      implicit val AddTwitterStreamWriter = new AvroByteStreamWriter[AddTwitterStream]
            
      implicit val TwitteruserCommandReader = new AvroByteStreamReader[TwitteruserCommand]
      implicit val TwitteruserCommandWriter = new AvroByteStreamWriter[TwitteruserCommand]
            
      implicit val GetTwitteruserCommandReader = new AvroByteStreamReader[GetTwitteruserCommand]
      implicit val GetTwitteruserCommandWriter = new AvroByteStreamWriter[GetTwitteruserCommand]
            
   }

   /** Generated Actor Client */
   class TwitterClient(serviceIdentifier: ServiceIdentifier, publisher: Publisher)(implicit system: ActorSystem) extends Client(serviceIdentifier, publisher, system){
      import Implicits._
      def addTwitterStream(x: AddTwitterStream, eventMeta: Option[EventMeta] = None): Future[Either[ServiceException,TwitterStreamAdded]] = wire[AddTwitterStream, TwitterStreamAdded](x, eventMeta)
      def getTwitteruserCommand(x: GetTwitteruserCommand, eventMeta: Option[EventMeta] = None): Future[Either[ServiceException,Twitteruser]] = wire[GetTwitteruserCommand, Twitteruser](x, eventMeta)
   }
}
