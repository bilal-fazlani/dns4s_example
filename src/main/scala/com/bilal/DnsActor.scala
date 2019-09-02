package com.bilal

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.event.LoggingReceive
import akka.io.IO
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import com.bilal.DnsActor.{AddTxtRecord, GetAllTxtRecords}
import com.github.mkroli.dns4s.Message
import com.github.mkroli.dns4s.akka._
import com.github.mkroli.dns4s.dsl._
import com.github.mkroli.dns4s.section.{HeaderSection, QuestionSection, ResourceRecord}

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong
import scala.language.postfixOps

class DnsActor extends Actor {
  import context._

  val root = "apps.bilal-fazlani.com"
  val acme = s"_acme-challenge.$root"

  val names: Map[String, String] = Map(
    s"myapp.$root" -> "192.168.0.104",
    s"myapp2.$root" -> "192.168.0.104",
    s"myapp3.$root" -> "192.168.0.104",
    s"tadaa.$root" -> "192.168.0.104"
  )

  var txtAddresses: Seq[String] = Seq.empty

   val destinationDns = new InetSocketAddress("8.8.8.8", 53)
//  val destinationDns = new InetSocketAddress("10.0.0.2", 53)

  def forwardMessage(message: Message): Future[Message] = {
    implicit val timeout: Timeout = Timeout(2 seconds)
    (IO(Dns) ? Dns.DnsPacket(message, destinationDns)).mapTo[Message]
  }

  override def receive: PartialFunction[Any, Unit] =  LoggingReceive {
    case AddTxtRecord(value) =>
      println(s"adding new txt address: $value")
      txtAddresses = txtAddresses :+ value
      println(s"new txtAddress list: $txtAddresses")

    case GetAllTxtRecords =>
      println("getting all txt records")
      sender() ! txtAddresses

    case Query(q) ~ Questions(QName(host) ~ TypeTXT() :: Nil)
      if host.toLowerCase == acme.toLowerCase || host.toLowerCase == root =>
      println(s"txt query $q \nreceived for host: $host")
      val res = txtAddresses.map(RRName(acme) ~ TXTRecord(_))
      sender ! Response(q) ~ Answers(res :_*)

//      //generic
//      Message(
//        HeaderSection(61770,false,0,false,false,false,false,0,1,0,0,1),
//        Vector(
//          QuestionSection(aPPS.BilAL-FazlANi.COM,257,1)
//        ),
//        Vector(),
//        Vector(),
//        Vector(ResourceRecord(,41,512,32768,OPTResource(List())))
//      )
//
//      // txt
//      Message(
//        HeaderSection(4067,false,0,false,false,false,false,0,1,0,0,1),
//        Vector(
//          QuestionSection(_ACmE-challenge.appS.bilAl-fazlaNI.COm,16,1)
//        ),
//        Vector(),
//        Vector(),
//        Vector(ResourceRecord(,41,512,32768,OPTResource(List())))
//      )
//
    case Query(q) ~ Questions(QName(host) ~ TypeA() :: Nil) if names.contains(host) =>
      println(s"query received for $host")
      sender ! Response(q) ~ Answers(RRName(host) ~ ARecord(names(host)))

    case message: Message =>
      println(s"query some query $message")
      forwardMessage(message).pipeTo(sender)
  }
}

object DnsActor {

  case class AddTxtRecord(value: String)
  case object GetAllTxtRecords

  def start(implicit system: ActorSystem): ActorRef = {
    implicit val timeout: Timeout    = Timeout(5 seconds)
    system.actorOf(Props[DnsActor])
  }

  def bind(actorRef: ActorRef)(implicit system: ActorSystem): Future[Any] = {
    implicit val timeout: Timeout    = Timeout(5 seconds)
    IO(Dns) ? Dns.Bind(actorRef, 53)
  }
}