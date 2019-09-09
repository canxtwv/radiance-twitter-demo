package com.affine.Content

import java.util.UUID
import io.surfkit.typebus._
import io.surfkit.typebus.event.DbAccessor
import io.surfkit.typebus.entity.EntityDb
import scala.concurrent.Future

package object data {


  sealed trait ContentCommand
  case class CreateContent(data: String) extends ContentCommand
  case class GetContent(id: UUID) extends ContentCommand
  case class GetContentEntityState(id: String) extends ContentCommand with DbAccessor

  sealed trait ContentEvent
  case class ContentCreated(entity: Content) extends ContentEvent
  case class ContentCompensatingActionPerformed(state: ContentState) extends ContentEvent
  case class ContentState(entity: Option[Content])
  case class Content(id: UUID, data: String)

  object Implicits extends AvroByteStreams{
    implicit val createContentRW = Typebus.declareType[CreateContent, AvroByteStreamReader[CreateContent], AvroByteStreamWriter[CreateContent]]
    implicit val ContentCreatedRW = Typebus.declareType[ContentCreated, AvroByteStreamReader[ContentCreated], AvroByteStreamWriter[ContentCreated]]
    implicit val ContentRW = Typebus.declareType[Content, AvroByteStreamReader[Content], AvroByteStreamWriter[Content]]
    implicit val getContentRW = Typebus.declareType[GetContent, AvroByteStreamReader[GetContent], AvroByteStreamWriter[GetContent]]
    implicit val getContentEntityStateRW = Typebus.declareType[GetContentEntityState, AvroByteStreamReader[GetContentEntityState], AvroByteStreamWriter[GetContentEntityState]]
    implicit val ContentStateRW = Typebus.declareType[ContentState, AvroByteStreamReader[ContentState], AvroByteStreamWriter[ContentState]]
  }

  trait ContentDatabase extends EntityDb[ContentState]{
    def createContent(x: CreateContent): Future[ContentCreated]
    def getContent(x: GetContent): Future[Content]
  }
}



