name := "SignalJSample"

version := "1.0.0"

libraryDependencies ++= Seq(
  "com.google.inject" % "guice" % "3.0"
)     

lazy val module = (project in file("module")).enablePlugins(PlayJava)

lazy val root = (project in file(".")).enablePlugins(PlayJava).aggregate(module).dependsOn(module)

scalaVersion := "2.11.1"