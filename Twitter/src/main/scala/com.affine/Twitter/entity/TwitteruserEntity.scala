package com.affine.Twitter.entity

import java.time.LocalDateTime
import java.util.UUID
import com.affine.Twitter.data._
import scala.concurrent.Future

import scala.concurrent.{ExecutionContext, Future}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.cluster.sharding.typed.{ClusterShardingSettings, ShardingEnvelope}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import io.surfkit.typebus.bus.Publisher
import io.surfkit.typebus.entity.EntityDb

object TwitteruserEntity {

  sealed trait Command
  // command
  final case class EntityCreateTwitteruser(id: UUID, create: CreateTwitteruser)(val replyTo: ActorRef[TwitteruserCreated]) extends Command
  final case class EntityModifyState(state: TwitteruserState)(val replyTo: ActorRef[TwitteruserCompensatingActionPerformed]) extends Command
  // query
  final case class EntityGetTwitteruser(get: GetTwitteruser)(val replyTo: ActorRef[Twitteruser]) extends Command
  final case class EntityGetState(id: UUID)(val replyTo: ActorRef[TwitteruserState]) extends Command

  val entityTypeKey: EntityTypeKey[Command] =
    EntityTypeKey[Command]("TwitteruserEntity")

  def behavior(entityId: String): Behavior[Command] =
    EventSourcedBehavior[Command, TwitteruserEvent, TwitteruserState](
    persistenceId = PersistenceId(entityId),
    emptyState =  TwitteruserState(None),
    commandHandler,
    eventHandler)

  private val commandHandler: (TwitteruserState, Command) => Effect[TwitteruserEvent, TwitteruserState] = { (state, command) =>
    command match {
      case x: EntityCreateTwitteruser =>
        val id = x.id
        val entity = Twitteruser(id, x.create.data)
        val created = TwitteruserCreated(entity)
        Effect.persist(created).thenRun(_ => x.replyTo.tell(created))

      case x: EntityGetTwitteruser =>
        state.entity.map(x.replyTo.tell)
        Effect.none

      case x: EntityGetState =>
        x.replyTo.tell(state)
        Effect.none

      case x: EntityModifyState =>
        val compensatingActionPerformed = TwitteruserCompensatingActionPerformed(x.state)
        Effect.persist(compensatingActionPerformed).thenRun(_ => x.replyTo.tell(compensatingActionPerformed))

      case _ => Effect.unhandled
    }
  }

  private val eventHandler: (TwitteruserState, TwitteruserEvent) => TwitteruserState = { (state, event) =>
    state match {
      case state: TwitteruserState =>
        event match {
        case TwitteruserCreated(module) =>
          TwitteruserState(Some(module))
        case TwitteruserCompensatingActionPerformed(newState) =>
          newState
        case _ => throw new IllegalStateException(s"unexpected event [$event] in state [$state]")
      }
      case _ => throw new IllegalStateException(s"unexpected event [$event] in state [$state]")
    }
  }

}


class TwitteruserEntityDatabase(system: ActorSystem[_], val producer: Publisher)(implicit val ex: ExecutionContext)
  extends TwitteruserDatabase{
  import akka.util.Timeout
  import scala.concurrent.duration._
  import akka.actor.typed.scaladsl.adapter._
  private implicit val askTimeout: Timeout = Timeout(5.seconds)

  override def typeKey = TwitteruserEntity.entityTypeKey
  val sharding =  ClusterSharding(system)
  val psEntities: ActorRef[ShardingEnvelope[TwitteruserEntity.Command]] =
    sharding.init(Entity(typeKey = typeKey,
      createBehavior = createEntity(TwitteruserEntity.behavior)(system.toUntyped))
      .withSettings(ClusterShardingSettings(system)))

  def entity(id: String) =
    sharding.entityRefFor(TwitteruserEntity.entityTypeKey, id)

  override def createTwitteruser(x: CreateTwitteruser): Future[TwitteruserCreated] = {
    val id = UUID.randomUUID()
    entity(id.toString) ? TwitteruserEntity.EntityCreateTwitteruser(id, x)
  }

  override def getTwitteruser(x: GetTwitteruser): Future[Twitteruser] =
    entity(x.id.toString) ? TwitteruserEntity.EntityGetTwitteruser(x)

  override def getState(id: String): Future[TwitteruserState] =
    entity(id) ? TwitteruserEntity.EntityGetState(UUID.fromString(id))

  override def modifyState(id: String, state: TwitteruserState): Future[TwitteruserState] =
    (entity(id) ? TwitteruserEntity.EntityModifyState(state)).map(_.state)
}
