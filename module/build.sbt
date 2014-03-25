name := "SignalJ"

version := "0.1.0-SNAPSHOT"

libraryDependencies ++= Seq(
	"com.google.inject" % "guice" % "3.0",
	"akkaguice" %% "akkaguice" % "0.7.0"
)

publishTo <<= version { (v: String) =>
	if (v.trim.endsWith("SNAPSHOT"))
    	Some(Resolver.file("file",  new File( "../../maven-repo/snapshots" )) )
    else
    	Some(Resolver.file("file",  new File( "../../maven-repo/releases" )) )
} 

publishArtifact in(Compile, packageDoc) := false

publishMavenStyle := true 

play.Project.playJavaSettings

resolvers += "release repository" at "http://chanan.github.io/maven-repo/releases/"

resolvers += "snapshot repository" at "http://chanan.github.io/maven-repo/snapshots/"