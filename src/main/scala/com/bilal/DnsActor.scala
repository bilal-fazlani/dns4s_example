package com.bilal

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorSystem, Props}
import akka.event.LoggingReceive
import akka.io.IO
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import com.github.mkroli.dns4s.Message
import com.github.mkroli.dns4s.akka._
import com.github.mkroli.dns4s.dsl._

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong
import scala.language.postfixOps

class DnsActor(txtValue: String) extends Actor {
  import context._

  val root = "apps.bilal-fazlani.com"
  val acme = s"_acme-challenge.$root"

  val names: Map[String, String] = Map(
    s"myapp.$root" -> "192.168.0.104",
    s"myapp2.$root" -> "192.168.0.104",
    s"myapp3.$root" -> "192.168.0.104",
    s"tadaa.$root" -> "192.168.0.104"
  )

   val destinationDns = new InetSocketAddress("8.8.8.8", 53)
//  val destinationDns = new InetSocketAddress("10.0.0.2", 53)

  def forwardMessage(message: Message): Future[Message] = {
    implicit val timeout: Timeout = Timeout(2 seconds)
    (IO(Dns) ? Dns.DnsPacket(message, destinationDns)).mapTo[Message]
  }

  override def receive: PartialFunction[Any, Unit] =  LoggingReceive {
    case Query(q) ~ Questions(QName(host) ~ TypeTXT() :: Nil) if host == acme =>
      println(s"txt query $q \nreceived for host: $host")
      sender ! Response(q) ~ Answers(RRName(host) ~ TXTRecord())
    case Query(q) ~ Questions(QName(host) ~ TypeA() :: Nil) if names.contains(host) =>
      println(s"query received for $host")
      sender ! Response(q) ~ Answers(RRName(host) ~ ARecord(names(host)))
    case message: Message =>
      println(s"query some query $message")
      forwardMessage(message).pipeTo(sender)
  }
}

object DnsActor {
  def start(txtValue:String)(implicit system: ActorSystem): Future[Any] = {
    implicit val timeout: Timeout    = Timeout(5 seconds)
    IO(Dns) ? Dns.Bind(system.actorOf(Props(new DnsActor(txtValue))), 53)
  }
}