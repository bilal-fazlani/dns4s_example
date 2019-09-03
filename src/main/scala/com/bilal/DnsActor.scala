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

  override def receive: PartialFunction[Any, Unit] =  LoggingReceive {
    case AddTxtRecord(value) =>
      pprintln(s"adding new txt address: $value")
      txtAddresses = txtAddresses :+ value
      pprintln(s"new txtAddress list: $txtAddresses")

    case GetAllTxtRecords =>
      pprintln("getting all txt records")
      sender() ! txtAddresses

    case Query(q) ~ Questions(QName(host) ~ TypeTXT() :: Nil) if host.toLowerCase == root =>
      pprintln(s"TXT_RECORD query received for host: $host")
      pprintln(q)
      println()
      val answers = txtAddresses.map(RRName(acme) ~ TXTRecord(_))
      val response = (Response(q) ~ Answers(answers: _*) ~ AuthoritativeAnswer)
      sender ! response
      pprintln("response sent")
      pprintln(response)
      println()

    case Query(q) ~ Questions(QName(host) ~ TypeCAA() :: Nil) if host.toLowerCase == root =>
      pprintln(s"CAA_RECORD query received for host: $host")
      pprintln(q)
      println()
      val answers = Vector(
        RRName(host) ~ CAARecord(1, "issuewild", "letsencrypt.org"),
        RRName(host) ~ CAARecord(1, "issue", "letsencrypt.org")
      )
      val response = (Response(q) ~ Answers(answers :_*) ~ AuthoritativeAnswer)
      sender ! response
      pprintln("response sent")
      pprintln(response)
      println()

    case Query(q) ~ Questions(QName(host) ~ TypeA() :: Nil) if names.contains(host.toLowerCase) =>
      pprintln(s"A_RECORD query received for $host")
      pprintln(q)
      println()
      val response = Response(q) ~ Answers(RRName(host) ~ ARecord(names(host))) ~ AuthoritativeAnswer
      sender ! response
      pprintln("response sent")
      pprintln(response)
      println()

    case message: Message =>
      pprintln(s"UNKNOWN query received & refused")
      pprintln(message)
      println()
      val response = Response(message) ~ Refused
      sender ! response
      pprintln("response sent")
      pprintln(response)
      println()

    case x =>
      pprintln(s"UNKNOWN akka message received")
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