name := "dns4s_try"

version := "0.1"

scalaVersion := "2.12.8"

resolvers += "bintray" at "http://jcenter.bintray.com"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % "2.5.25",
  "com.typesafe.akka" %% "akka-stream" % "2.5.25",
  "com.typesafe.akka" %% "akka-http" % "10.1.9",
  "com.github.mkroli" %% "dns4s-akka" % "0.14.0",
  "com.lihaoyi" %% "pprint" % "0.5.5"
)