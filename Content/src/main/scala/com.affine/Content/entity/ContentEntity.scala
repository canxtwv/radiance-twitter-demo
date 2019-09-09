package com.affine.Content.entity

import java.time.LocalDateTime
import java.util.UUID
import com.affine.Content.data._
import scala.concurrent.Future

import scala.concurrent.{ExecutionContext, Future}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.cluster.sharding.typed.{ClusterShardingSettings, ShardingEnvelope}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import io.surfkit.typebus.bus.Publisher
import io.surfkit.typebus.entity.EntityDb

object ContentEntity {

  sealed trait Command
  // command
  final case class EntityCreateContent(id: UUID, create: CreateContentCommand)(val replyTo: ActorRef[ContentCreated]) extends Command
  // query
  final case class EntityGetContent(get: GetContentCommand)(val replyTo: ActorRef[Content]) extends Command
  final case class EntityGetState(id: UUID)(val replyTo: ActorRef[ContentState]) extends Command

  val entityTypeKey: EntityTypeKey[Command] =
    EntityTypeKey[Command]("ContentEntity")

  def behavior(entityId: String): Behavior[Command] =
    EventSourcedBehavior[Command, ContentEvent, ContentState](
    persistenceId = PersistenceId(entityId),
    emptyState =  ContentState(None, LocalDateTime.now().toString),
    commandHandler,
    eventHandler)

  private val commandHandler: (ContentState, Command) => Effect[ContentEvent, ContentState] = { (state, command) =>
    command match {
      case x: EntityCreateContent =>
        val id = x.id
        val entity = Content(id, x.create.entity.data)
        val created = ContentCreated(entity)
        Effect.persist(created).thenRun(_ => x.replyTo.tell(created))

      case x: EntityGetContent =>
        state.entity.map(x.replyTo.tell)
        Effect.none

      case x: EntityGetState =>
        x.replyTo.tell(state)
        Effect.none

      case _ => Effect.unhandled
    }
  }

  private val eventHandler: (ContentState, ContentEvent) => ContentState = { (state, event) =>
    state match {
      case state: ContentState =>
        event match {
        case ContentCreated(x) =>
          ContentState(Some(x), LocalDateTime.now().toString)
        case _ => throw new IllegalStateException(s"unexpected event [$event] in state [$state]")
      }
      case _ => throw new IllegalStateException(s"unexpected event [$event] in state [$state]")
    }
  }

}


class ContentEntityDatabase(system: ActorSystem[_], val producer: Publisher)(implicit val ex: ExecutionContext)
  extends ContentDatabase{
  import akka.util.Timeout
  import scala.concurrent.duration._
  import akka.actor.typed.scaladsl.adapter._
  private implicit val askTimeout: Timeout = Timeout(5.seconds)

  override def typeKey = ContentEntity.entityTypeKey
  val sharding =  ClusterSharding(system)
  val psEntities: ActorRef[ShardingEnvelope[ContentEntity.Command]] =
    sharding.init(Entity(typeKey = typeKey,
      createBehavior = createEntity(ContentEntity.behavior)(system.toUntyped))
      .withSettings(ClusterShardingSettings(system)))

  def entity(id: String) =
    sharding.entityRefFor(ContentEntity.entityTypeKey, id)

  override def createContent(x: CreateContentCommand): Future[ContentCreated] = {
    val id = x.entity.id
    entity(id.toString) ? ContentEntity.EntityCreateContent(id, x)
  }

  override def getContent(x: GetContentCommand): Future[Content] =
    entity(x.id.toString) ? ContentEntity.EntityGetContent(x)

  override def getState(id: String): Future[ContentState] =
    entity(id) ? ContentEntity.EntityGetState(UUID.fromString(id))

  override def modifyState(id: String, state: ContentState): Future[ContentState] =
    Future.successful(state)
}
