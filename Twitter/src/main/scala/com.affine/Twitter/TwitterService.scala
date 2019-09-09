package com.affine.Twitter

import akka.actor._
import com.affine.Twitter.data._
import io.surfkit.typebus
import io.surfkit.typebus._
import io.surfkit.typebus.annotations.ServiceMethod
import io.surfkit.typebus.bus.{Publisher, RetryBackoff}
import io.surfkit.typebus.event.{EventMeta, ServiceIdentifier}
import io.surfkit.typebus.module.Service

import scala.concurrent.Future
import scala.concurrent.duration._

class TwitterService(serviceIdentifier: ServiceIdentifier, publisher: Publisher, sys: ActorSystem, twitteruserDb: TwitteruserDatabase) extends Service(serviceIdentifier,publisher) with AvroByteStreams{
  implicit val system = sys

  system.log.info("Starting service: " + serviceIdentifier.name)
  val bus = publisher.busActor
  import com.affine.Twitter.data.Implicits._

  registerDataBaseStream[GetTwitteruserEntityState, TwitteruserState](twitteruserDb)

  @ServiceMethod
  def createTwitteruser(createTwitteruser: CreateTwitteruser, meta: EventMeta): Future[TwitteruserCreated] = twitteruserDb.createTwitteruser(createTwitteruser)
  registerStream(createTwitteruser _)
    .withPartitionKey(_.entity.id.toString)

  @ServiceMethod
  def getTwitteruser(getTwitteruser: GetTwitteruser, meta: EventMeta): Future[Twitteruser] = twitteruserDb.getTwitteruser(getTwitteruser)
  registerStream(getTwitteruser _)
    .withRetryPolicy{
    case _ => typebus.bus.RetryPolicy(3, 1 second, RetryBackoff.Exponential)
  }

  system.log.info("Finished registering streams, trying to start service.")

}