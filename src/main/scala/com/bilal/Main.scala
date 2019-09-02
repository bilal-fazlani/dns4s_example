package com.bilal

import akka.actor.ActorSystem

import scala.io.StdIn

object Main extends App {
  implicit val system: ActorSystem = ActorSystem("DnsServer")
  DnsActor.start

  val a = StdIn.readLine("press any button to exit...\n")
  system.terminate()
}
