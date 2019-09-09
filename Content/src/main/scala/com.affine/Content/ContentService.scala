package com.affine.Content

import akka.actor._
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

  registerDataBaseStream[GetContentEntityState, ContentState](contentDb)

  @ServiceMethod
  def createContent(createContent: CreateContent, meta: EventMeta): Future[ContentCreated] = contentDb.createContent(createContent)
  registerStream(createContent _)
    .withPartitionKey(_.entity.id.toString)

  @ServiceMethod
  def getContent(getContent: GetContent, meta: EventMeta): Future[Content] = contentDb.getContent(getContent)
  registerStream(getContent _)
    .withRetryPolicy{
    case _ => typebus.bus.RetryPolicy(3, 1 second, RetryBackoff.Exponential)
  }

  system.log.info("Finished registering streams, trying to start service.")

}