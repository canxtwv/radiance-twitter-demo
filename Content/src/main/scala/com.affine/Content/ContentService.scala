package com.affine.Content

import akka.actor._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import com.affine.Content.data._
import io.surfkit.typebus
import io.surfkit.typebus._
import io.surfkit.typebus.annotations.ServiceMethod
import io.surfkit.typebus.bus.{Publisher, RetryBackoff}
import io.surfkit.typebus.event.{EventMeta, ServiceIdentifier}
import io.surfkit.typebus.module.Service

import scala.concurrent.Future
import scala.concurrent.duration._

class ContentService(serviceIdentifier: ServiceIdentifier, publisher: Publisher, sys: ActorSystem, contentDb: ContentDatabase) extends Service(serviceIdentifier,publisher) with AvroByteStreams{
  implicit val system = sys

  system.log.info("Starting service: " + serviceIdentifier.name)
  val bus = publisher.busActor
  import com.affine.Content.data.Implicits._
  import api.com.affine.Twitter.data.Implicits._

  registerDataBaseStream[GetContentEntityState, ContentState](contentDb)

  import system.dispatcher

  val decider: Supervision.Decider = {
    case _ => Supervision.Resume  // Never give up !
  }

  implicit val materializer = ActorMaterializer(ActorMaterializerSettings(system).withSupervisionStrategy(decider))

  val notificationWords = Set(
    "lightbend",
    "coffee",
    "reactive",
    "cqrs",
    "demo",
    "micro",
    "kafka",
    "cassandra"
  )

  val slackUrl = "https://hooks.slack.com/services/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"

  /*
  curl -X POST -H 'Content-type: application/json' --data '{"text":"Hello, World!"}' https://hooks.slack.com/services/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
   */

  @ServiceMethod
  def notifyRecentTweets(tweet: api.com.affine.Twitter.data.RecentTweets, meta: EventMeta): Future[NotificationSent] = {
    val foundWordsInTweets = tweet.tweets.map(x => (x, x.text.split(' ')))
      .filter(y => y._2.toSet.intersect(notificationWords).size > 0)
      .map(_._1)

    val found = foundWordsInTweets.map{ t =>
      ("https://twitter.com/statuses/"+t.id, t.text.split(' ').toSet.intersect(notificationWords))
    }
    println(s"notifyRecentTweets: ${tweet}")
    Future.sequence(
      found.map{ y =>
        println(s"found: ${y}")
        val json =
          s"""
             |{ "text": "Found a match for words ${y._2} at: ${y._1}" }
          """.stripMargin
        val jsonEntity = HttpEntity(ContentTypes.`application/json`, json)
        println(s"curl -XPOST 'slackUrl' -d '${json}'")
        Http().singleRequest(HttpRequest(method = HttpMethods.POST, uri = slackUrl, entity = jsonEntity)).map(_ => json)
      }
    ).map{ x =>
      NotificationSent(x)
    }

  }
  registerStream(notifyRecentTweets _)

  system.log.info("Finished registering streams, trying to start service.")

}