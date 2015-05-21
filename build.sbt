name := """LoadGen"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
	"org.mongodb" %% "casbah" % "2.8.1",
	javaJdbc,
	javaEbean,
	cache,
	javaWs
)


fork in run := true

fork in run := true
