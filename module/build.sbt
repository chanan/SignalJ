name := "SignalJ"

version := "0.5.0"

organization := "signalJ"

libraryDependencies ++= Seq(
	"org.reflections" % "reflections" % "0.9.9-RC1",
  "com.fasterxml.jackson.core" % "jackson-annotations" % "2.4.3"
)

publishTo <<= version { (v: String) =>
	if (v.trim.endsWith("SNAPSHOT"))
    	Some(Resolver.file("file",  new File( "../../maven-repo/snapshots" )) )
    else
    	Some(Resolver.file("file",  new File( "../../maven-repo/releases" )) )
} 

publishArtifact in(Compile, packageDoc) := false

publishMavenStyle := true

resolvers += "release repository" at "http://chanan.github.io/maven-repo/releases/"

resolvers += "snapshot repository" at "http://chanan.github.io/maven-repo/snapshots/"

scalaVersion := "2.11.1"

lazy val root = (project in file(".")).enablePlugins(PlayJava)