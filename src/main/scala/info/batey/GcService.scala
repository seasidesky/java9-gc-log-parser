package info.batey

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.event.{Logging, LoggingAdapter}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink}
import info.batey.actors.GcStateActor.{GcState, GenerationSizes, HeapSize}
import info.batey.actors.{GcStateActor, UnknownLineEvent, PauseActor}
import scala.concurrent.ExecutionContext.Implicits._
import spray.json._

object GcService extends GcLogStream with GcStateJson {

  override implicit val system: ActorSystem = ActorSystem("GCParser")
  override implicit val materialiser: ActorMaterializer = ActorMaterializer()

  override val log: LoggingAdapter = Logging(system, "main")

  override val young: ActorRef = system.actorOf(Props(classOf[PauseActor]), "YoungGen")
  override val unknown: ActorRef = system.actorOf(Props(classOf[UnknownLineEvent]), "UnknownMsgs")
  override val gcState: ActorRef = system.actorOf(Props(classOf[GcStateActor]), "GcState")

  val mode = "console"

  def main(args: Array[String]): Unit = {
    // todo cmd line args
    // todo in http mode we need to create the actors per request

    val (_, gcFun) = process.recover {
        case e: Throwable =>
          e.printStackTrace(System.out)
          GcState(0, 0, 0, 0, 0, HeapSize(0, 0), 0.0, GenerationSizes(0, 0, 0, 0))
      }.map(_.toJson)
      .runWith(
      streamedLogEvents,
      Sink.foreach(println))
    gcFun.flatMap(_ => system.terminate())
    //todo open tsdb and prometheus sinks!
  }
}

