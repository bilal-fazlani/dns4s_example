name := "dns4s_try"

version := "0.1"

scalaVersion := "2.12.8"

resolvers += "bintray" at "http://jcenter.bintray.com"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.12",
  "com.github.mkroli" %% "dns4s-akka" % "0.14.0"
)