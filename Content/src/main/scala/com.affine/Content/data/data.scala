package com.affine.Content

import java.util.UUID
import io.surfkit.typebus._
import io.surfkit.typebus.event.DbAccessor
import io.surfkit.typebus.entity.EntityDb
import scala.concurrent.Future

package object data {


  sealed trait ContentCommand
  case class CreateContentCommand(entity: Content) extends ContentCommand
  case class GetContentCommand(id: UUID) extends ContentCommand
  case class GetContentEntityState(id: String) extends ContentCommand with DbAccessor

  sealed trait ContentEvent
  case class ContentCreated(entity: Content) extends ContentEvent
  case class ContentState(entity: Option[Content], timeStamp: String)
  case class Content(id: UUID, data: String)

  case class NotificationSent(json: Seq[String]) extends ContentEvent

  object Implicits extends AvroByteStreams{
    implicit val createContentRW = Typebus.declareType[CreateContentCommand, AvroByteStreamReader[CreateContentCommand], AvroByteStreamWriter[CreateContentCommand]]
    implicit val ContentRW = Typebus.declareType[Content, AvroByteStreamReader[Content], AvroByteStreamWriter[Content]]
    implicit val getContentRW = Typebus.declareType[GetContentCommand, AvroByteStreamReader[GetContentCommand], AvroByteStreamWriter[GetContentCommand]]

    implicit val notificationSentRW = Typebus.declareType[NotificationSent, AvroByteStreamReader[NotificationSent], AvroByteStreamWriter[NotificationSent]]

    implicit val ContentStateRW = Typebus.declareType[ContentState, AvroByteStreamReader[ContentState], AvroByteStreamWriter[ContentState]]
    implicit val GetContentEntityStateRW = Typebus.declareType[GetContentEntityState, AvroByteStreamReader[GetContentEntityState], AvroByteStreamWriter[GetContentEntityState]]
  }

  trait ContentDatabase extends EntityDb[ContentState]{
    def createContent(x: CreateContentCommand): Future[ContentCreated]
    def getContent(x: GetContentCommand): Future[Content]
  }
}



