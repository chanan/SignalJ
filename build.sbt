name := "SignalJSample"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  cache
)     

play.Project.playJavaSettings

lazy val module = project.in(file("module"))

lazy val main = project.in(file(".")).dependsOn(module).aggregate(module)