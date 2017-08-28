name := "scalaTestFeatureReport"

version := "1.0-SNAPSHOT"

scalaVersion := "2.12.0"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1"
libraryDependencies += "io.spray" %% "spray-json" % "1.3.3"
libraryDependencies += "org.pegdown" % "pegdown" % "1.6.0"


parallelExecution in Test := false
testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-C", "com.dy.feature.report.FeatureReport")