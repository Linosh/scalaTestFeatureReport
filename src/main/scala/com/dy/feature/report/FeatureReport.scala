package com.dy.feature.report

import org.scalatest.ResourcefulReporter
import org.scalatest.events._
import spray.json._

import scala.collection.immutable.Stack

private object ReportJsonProtocol extends DefaultJsonProtocol {

  implicit val summaryFormat: JsonFormat[Summary] = new JsonFormat[Summary] {
    override def read(json: JsValue) = ???

    override def write(obj: Summary) = JsObject(
      ("testsSucceededCount", JsNumber(obj.testsSucceededCount)),
      ("testsFailedCount", JsNumber(obj.testsFailedCount)),
      ("testsIgnoredCount", JsNumber(obj.testsIgnoredCount)),
      ("testsPendingCount", JsNumber(obj.testsPendingCount)),
      ("testsCanceledCount", JsNumber(obj.testsCanceledCount)),
      ("suitesCompletedCount", JsNumber(obj.suitesCompletedCount)),
      ("suitesAbortedCount", JsNumber(obj.suitesAbortedCount)),
      ("scopesPendingCount", JsNumber(obj.scopesPendingCount)),
      ("testsCompletedCount", JsNumber(obj.testsCompletedCount)),
      ("totalTestsCount", JsNumber(obj.totalTestsCount))
    )
  }

  implicit val throwableFormat: JsonFormat[Throwable] = new JsonFormat[Throwable] {
    override def read(json: JsValue) = new Throwable(json.toString())

    override def write(obj: Throwable) = JsArray(obj.getStackTrace.map(s => JsString(s.toString)).toVector)
  }

  implicit val testFormat: JsonFormat[Test] = jsonFormat5(Test)

  implicit val scopeFormat: JsonFormat[Scope] = new JsonFormat[Scope] {

    override def read(json: JsValue) = ???

    override def write(scope: Scope) = JsObject(
      ("name", JsString(scope.name)),
      ("startTime", JsNumber(scope.startTime)),
      ("stopTime", JsNumber(scope.stopTime)),
      ("parent", scope.parent.map(write).getOrElse(JsNull)),
      ("tests", JsObject(scope.tests.map(e => e._1 -> e._2.toJson)))
    )
  }

  implicit val suiteFormat: JsonFormat[Suite] = jsonFormat5(Suite)
  implicit val reportFormat: JsonFormat[Report] = jsonFormat5(Report)
}


private case class Report(startTime: Long = 0,
                          stopTime: Long = 0,
                          duration: Option[Long] = None,
                          suites: Map[String, Suite] = Map(),
                          summary: Option[Summary] = None) {

  def +(suite: Suite): Report = copy(suites = suites + (suite.id -> suite))
}

private case class Suite(id: String = "",
                         name: String = "",
                         startTime: Long = 0,
                         stopTime: Long = 0,
                         scopes: Map[String, Scope] = Map())

private case class Scope(name: String = "",
                         startTime: Long = 0,
                         stopTime: Long = 0,
                         parent: Option[Scope],
                         tests: Map[String, Test] = Map())

private case class Test(name: String = "",
                        testText: String = "",
                        startTime: Long = 0,
                        stopTime: Long = 0,
                        exception: Option[Throwable] = None)

class FeatureReport extends ResourcefulReporter {

  import ReportJsonProtocol._

  val events = List.newBuilder[Event]

  override def dispose(): Unit = {
    val report = events.result()
      .sorted
      .foldLeft((Report(), Stack[Scope]())) { case ((r, scopeStack), ev) =>
        ev match {
          case v: RunStarting => (r.copy(startTime = v.timeStamp), scopeStack)
          case v: RunCompleted => (r.copy(stopTime = v.timeStamp, summary = v.summary, duration = v.duration), scopeStack)
          case v: RunStopped => (r.copy(stopTime = v.timeStamp, summary = v.summary, duration = v.duration), scopeStack)
          case v: RunAborted => (r.copy(stopTime = v.timeStamp, summary = v.summary, duration = v.duration), scopeStack)

          case v: SuiteStarting => (r + Suite(v.suiteId, v.suiteName, v.timeStamp), scopeStack)

          case v: SuiteCompleted =>
            val suite = r.suites(v.suiteId).copy(stopTime = v.timeStamp)
            (r + suite, scopeStack)

          case v: ScopeOpened =>
            val currSuite = r.suites(v.nameInfo.suiteId)
            val currScope = if (scopeStack.isEmpty) None else Some(scopeStack.top)
            val newScope = Scope(name = v.message, startTime = v.timeStamp, parent = currScope)
            val newScopes = currSuite.scopes + (v.message -> newScope)

            (r + currSuite.copy(scopes = newScopes), scopeStack.push(newScope))

          case v: ScopeClosed =>
            val suite = r.suites(v.nameInfo.suiteId)
            val newScope = v.message -> suite.scopes(v.message).copy(stopTime = v.timeStamp)

            (r + suite.copy(scopes = suite.scopes + newScope), scopeStack.pop)

          case v: ScopePending =>
            val suite = r.suites(v.nameInfo.suiteId)
            val newScope = v.message -> suite.scopes(v.message).copy(stopTime = v.timeStamp)

            (r + suite.copy(scopes = suite.scopes + newScope), scopeStack.pop)

          case v: TestStarting =>
            val test = v.testName -> Test(v.testName, v.testText, v.timeStamp)
            val suite = r.suites(v.suiteId)
            val scope = suite.scopes(scopeStack.top.name)
            val newScope = scope.name -> scope.copy(tests = scope.tests + test)

            (r + suite.copy(scopes = suite.scopes + newScope), scopeStack)

          case v: TestSucceeded =>
            val suite = r.suites(v.suiteId)
            val scope = suite.scopes(scopeStack.top.name)
            val test = v.testName -> scope.tests(v.testName).copy(stopTime = v.timeStamp)
            val newScope = scope.name -> scope.copy(tests = scope.tests + test)

            (r + suite.copy(scopes = suite.scopes + newScope), scopeStack)

          case v: TestFailed =>
            val suite = r.suites(v.suiteId)
            val scope = suite.scopes(scopeStack.top.name)
            val test = v.testName -> scope.tests(v.testName).copy(stopTime = v.timeStamp, exception = v.throwable)
            val newScope = scope.name -> scope.copy(tests = scope.tests + test)

            (r + suite.copy(scopes = suite.scopes + newScope), scopeStack)

          case _ => (r, scopeStack)
        }
      }._1.toJson.prettyPrint


    println(report)

  }

  override def apply(event: Event): Unit = {
    events += event
  }
}
