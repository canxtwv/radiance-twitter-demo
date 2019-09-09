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
  final case class EntityAddTweets(id: String, add: AddTweets)(val replyTo: ActorRef[TweetsAdded]) extends Command

  // query
  final case class EntityGetTwitteruserCommand(id: String)(val replyTo: ActorRef[Twitteruser]) extends Command
  final case class EntityGetState(id: String)(val replyTo: ActorRef[TwitteruserState]) extends Command

  val entityTypeKey: EntityTypeKey[Command] =
    EntityTypeKey[Command]("TwitteruserEntity")

  def behavior(entityId: String): Behavior[Command] =
    EventSourcedBehavior[Command, TwitteruserEvent, TwitteruserState](
    persistenceId = PersistenceId(entityId),
    emptyState =  TwitteruserState(tweets = Map.empty[String, Tweet]),
    commandHandler,
    eventHandler)

  private val commandHandler: (TwitteruserState, Command) => Effect[TwitteruserEvent, TwitteruserState] = { (state, command) =>
    command match {
      case x: EntityAddTweets =>
        val id = x.id
        val created = TweetsAdded(id, x.add.tweets)
        Effect.persist(created).thenRun(_ => x.replyTo.tell(created))

      case x: EntityGetTwitteruserCommand =>
        x.replyTo.tell(Twitteruser(x.id, state.tweets.values.toSeq) )
        Effect.none

      case x: EntityGetState =>
        x.replyTo.tell(state)
        Effect.none

      case _ => Effect.unhandled
    }
  }

  private val eventHandler: (TwitteruserState, TwitteruserEvent) => TwitteruserState = { (state, event) =>
    state match {
      case state: TwitteruserState =>
        event match {
        case TweetsAdded(_, x) =>
          TwitteruserState(tweets = state.tweets ++ x.map(y => y.id -> y).toMap)
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

  override def addTweets(x: AddTweets): Future[TweetsAdded] = {
    val id = x.user
    entity(id.toString) ? TwitteruserEntity.EntityAddTweets(id, x)
  }

  override def getTwitteruser(x: GetTwitteruserCommand): Future[Twitteruser] =
    entity(x.user) ? TwitteruserEntity.EntityGetTwitteruserCommand(x.user)

  override def getState(id: String): Future[TwitteruserState] =
    entity(id) ? TwitteruserEntity.EntityGetState(id)

  override def modifyState(id: String, state: TwitteruserState): Future[TwitteruserState] =
    Future.successful(state)
}
