package com.dy.feature.report

import org.scalatest.ResourcefulReporter
import org.scalatest.events.Event


private case class Report(suites: Map[String, Suite])
private case class Suite(parent: Suite, name: String, scope: Option[Scope])
private case class Scope(parent: Scope, tests: List[Test])
private case class Test()

class FeatureReport extends ResourcefulReporter {
  val events = List.newBuilder[Event]

  override def dispose(): Unit = {
    println("\n\n\n\nCFR dispose\n\n\n\n")
    events.result().foreach(v => println(v))


  }

  override def apply(event: Event): Unit = {
    events += event
  }
}
