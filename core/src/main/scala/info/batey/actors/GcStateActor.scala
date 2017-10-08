package info.batey.actors

import akka.actor.Actor
import akka.event.Logging
import info.batey.GCLogFileModel._
import info.batey.actors.GcStateActor._

import scala.concurrent.duration.Duration

class GcStateActor extends Actor {

  import context._

  private val log = Logging(context.system, this)

  def receive: Receive = gcState(GcState(TimeOffset(0), 0, 0, 0, 0, 0, HeapSize(0, 0), 0, GenerationSizes(0, 0, 0, 0)))

  def gcState(gs: GcState): Receive = {
    case DetailedPause(timeOffset, pauseType, _, HeapSizes(_, after, total), genSizes) =>
      val newState = pauseType match {
        case Young =>
          gs.copy(timeOffset = timeOffset,
            youngGcs = gs.youngGcs + 1,
            heapSize = HeapSize(after, total),
            generationSizes = genSizes
          )
        case Full =>
          gs.copy(timeOffset = timeOffset,
            fullGcs = gs.fullGcs + 1,
            heapSize = HeapSize(after, total),
            generationSizes = genSizes
          )
        case InitialMark =>
          gs.copy(timeOffset = timeOffset,
            initialMarks = gs.initialMarks + 1,
            heapSize = HeapSize(after, total),
            generationSizes = genSizes
          )
        case Mixed =>
          gs.copy(timeOffset = timeOffset,
            mixed = gs.mixed + 1,
            heapSize = HeapSize(after, total),
            generationSizes = genSizes
          )
        case ty@_ =>
          log.warning("Ignoring pause type: {}", ty)
          gs
      }
      sender ! newState
      become(gcState(newState))
    case RemarkPause(timeOffset, _, HeapSizes(_, after, total)) =>
      val newState = gs.copy(timeOffset = timeOffset,
        remarks = gs.remarks + 1,
        heapSize = HeapSize(after, total),
      )
      sender ! newState
      become(gcState(newState))
    case NotInteresting() =>
      log.debug("Received a not interesting, go make it interesting!")
      sender ! gs
  }
}

object GcStateActor {
  // state
  case class GcState(
                      timeOffset: TimeOffset,
                      fullGcs: Long,
                      youngGcs: Long,
                      initialMarks: Long,
                      remarks: Long,
                      mixed: Long,
                      heapSize: HeapSize,
                      allocationRatePerMb: Double,
                      generationSizes: GenerationSizes
                    )

  case class HeapSize(size: Long, total: Long)


  // All gc events
  sealed trait GcEvent
  // shouldn't really use PauseType as it is the file model
  case class DetailedPause(timeOffset: TimeOffset, gen: PauseType, dur: Duration, heapSize: HeapSizes, genSizes: GenerationSizes) extends GcEvent
  case class RemarkPause(timeOffset: TimeOffset, dur: Duration, heapSize: HeapSizes) extends GcEvent
  case class NotInteresting() extends GcEvent

  case class HeapSizes(before: Long, after: Long, total: Long)
  case class GenerationSizes(eden: Long, survivor: Long, old: Long, humongous: Long)

  object HeapSizes {
    def fromStats(stats: CollectionStats): HeapSizes = {
      HeapSizes(stats.before, stats.after, stats.total)
    }
  }

}
