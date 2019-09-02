package com.bilal

import java.net.InetSocketAddress
import pprint._
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.event.LoggingReceive
import akka.io.IO
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import com.bilal.DnsActor.{AddTxtRecord, GetAllTxtRecords}
import com.github.mkroli.dns4s.Message
import com.github.mkroli.dns4s.akka._
import com.github.mkroli.dns4s.dsl._

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong
import scala.language.postfixOps

class DnsActor extends Actor {
  import context._

  val root = "apps.bilal-fazlani.com"
  val acme = s"_acme-challenge.$root"

  val names: Map[String, String] = Map(
    root -> "13.235.235.149",
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
      println(s"adding new txt address: $value\n")
      txtAddresses = txtAddresses :+ value
      println(s"new txtAddress list: $txtAddresses\n")

    case GetAllTxtRecords =>
      pprintln("getting all txt records\n")
      sender() ! txtAddresses

    case Query(q) ~ Questions(QName(host) ~ TypeTXT() :: Nil) =>
      println(s"TXT_RECORD query received for host: $host")
      pprintln(q)
      println()
      val res = txtAddresses.map(RRName(acme) ~ TXTRecord(_))
      val response = (Response(q) ~ Answers(res: _*) ~ AuthoritativeAnswer).copy(additional = Seq.empty)
      sender ! response
      println("response sent")
      pprintln(response)

    case Query(q) ~ Questions(QName(host) ~ TypeA() :: Nil) if names.contains(host.toLowerCase) =>
      println(s"A_RECORD query received for $host")
      pprintln(q)
      println()
      val response = Response(q) ~ Answers(RRName(host) ~ ARecord(names(host))) ~ AuthoritativeAnswer
      sender ! response
      println("response sent")
      pprintln(response)

    case message: Message =>
      println(s"UNKNOWN query received & refused")
      pprintln(message)
      println()
      val response = Response(message) ~ Refused
      sender ! response
      println("response sent")
      pprintln(response)

    case x =>
      println(s"UNKNOWN akka message received")
      pprintln(x)
      println()
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