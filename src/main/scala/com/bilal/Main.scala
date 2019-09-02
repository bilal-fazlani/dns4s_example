package com.bilal

import akka.actor.ActorSystem

import scala.concurrent.Await
import scala.concurrent.duration.DurationLong
import scala.io.StdIn
import scala.util.control.NonFatal

object Main extends App {
  implicit val system: ActorSystem = ActorSystem("DnsServer")
  var exitCode = 0
  try{
    val txtValue: String = args.headOption.getOrElse(throw new RuntimeException("please provide txt value"))
    val startResult = Await.result(DnsActor.start(txtValue), 5.seconds)
    println(s"DNS service: $startResult")
    StdIn.readLine("press any button to exit...\n")
  }
  catch {
    case NonFatal(exception) =>
      println("DNS service failed to start")
      println(exception.getClass.getSimpleName)
      println(exception.getMessage)
      exception.printStackTrace()
      exitCode = 1
  }
  finally {
    system.terminate()
    sys.exit(exitCode)
  }
}
