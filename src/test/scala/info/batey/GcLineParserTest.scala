package info.batey

import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.{FunSpec, FunSuite, Matchers}

import scala.concurrent.duration._
import scala.language.postfixOps

class GcLineParserTest extends FunSpec with Matchers {

  import GcLineParser._
  import TimeOffset._

  val lines = Table(
    ("gc_line", "outcome"),

    ("[0.010s][info][gc] Using G1",
      GcInfo(Metadata(10L, Info), "Using G1")),

    ("[39.708s][info][gc] GC(0) Pause Young (G1 Evacuation Pause) 24M->8M(256M) 6.545ms",
      EvacuationPause(Metadata(39708L, Info), Young, CollectionStats(24, 8, 256, 6.545 milliseconds))),

    ("[555.879s][info][gc] GC(8) Pause Initial Mark (G1 Evacuation Pause) 185M->159M(256M) 1.354ms",
        InitialMark(Metadata(555879L, Info), CollectionStats(185, 159, 256, 1.354 milliseconds))),

    ("[555.879s][info][gc] GC(9) Concurrent Cycle",
      ConcurrentCycle(Metadata(555879L, Info))),

    ("[613.102s][info][gc] GC(15) Pause Remark 149M->149M(256M) 1.381ms",
      Remark(Metadata(613102L, Info), CollectionStats(149, 149, 256, 1.381 milliseconds)))
  )

  forAll(lines) { (line: String, outcome: G1GcEvent) => {
    parse(gcLine, line).get should equal(outcome)
  }
  }
}
