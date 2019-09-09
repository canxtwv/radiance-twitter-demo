package com.affine.Twitter

import java.util.UUID
import io.surfkit.typebus._
import io.surfkit.typebus.event.DbAccessor
import io.surfkit.typebus.entity.EntityDb
import scala.concurrent.Future

package object data {


  sealed trait TwitteruserCommand
  case class CreateTwitteruser(data: String) extends TwitteruserCommand
  case class GetTwitteruser(id: UUID) extends TwitteruserCommand
  case class GetTwitteruserEntityState(id: String) extends TwitteruserCommand with DbAccessor

  sealed trait TwitteruserEvent
  case class TwitteruserCreated(entity: Twitteruser) extends TwitteruserEvent
  case class TwitteruserCompensatingActionPerformed(state: TwitteruserState) extends TwitteruserEvent
  case class TwitteruserState(entity: Option[Twitteruser])
  case class Twitteruser(id: UUID, data: String)

  object Implicits extends AvroByteStreams{
    implicit val createTwitteruserRW = Typebus.declareType[CreateTwitteruser, AvroByteStreamReader[CreateTwitteruser], AvroByteStreamWriter[CreateTwitteruser]]
    implicit val TwitteruserCreatedRW = Typebus.declareType[TwitteruserCreated, AvroByteStreamReader[TwitteruserCreated], AvroByteStreamWriter[TwitteruserCreated]]
    implicit val TwitteruserRW = Typebus.declareType[Twitteruser, AvroByteStreamReader[Twitteruser], AvroByteStreamWriter[Twitteruser]]
    implicit val getTwitteruserRW = Typebus.declareType[GetTwitteruser, AvroByteStreamReader[GetTwitteruser], AvroByteStreamWriter[GetTwitteruser]]
    implicit val getTwitteruserEntityStateRW = Typebus.declareType[GetTwitteruserEntityState, AvroByteStreamReader[GetTwitteruserEntityState], AvroByteStreamWriter[GetTwitteruserEntityState]]
    implicit val TwitteruserStateRW = Typebus.declareType[TwitteruserState, AvroByteStreamReader[TwitteruserState], AvroByteStreamWriter[TwitteruserState]]
  }

  trait TwitteruserDatabase extends EntityDb[TwitteruserState]{
    def createTwitteruser(x: CreateTwitteruser): Future[TwitteruserCreated]
    def getTwitteruser(x: GetTwitteruser): Future[Twitteruser]
  }
}



