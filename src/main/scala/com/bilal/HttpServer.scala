package com.bilal

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.{Directives, Route}
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.bilal.DnsActor.{AddTxtRecord, GetAllTxtRecords}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

class HttpServer(ref: ActorRef)(implicit actorSystem: ActorSystem)
  extends Directives
{
  private implicit val actorMaterializer: ActorMaterializer = ActorMaterializer()
  private implicit val timeout: Timeout = Timeout(2.seconds)
  private def route: Route = {
    (post & parameter("txtValue")){ txtValue =>
      ref ! AddTxtRecord(txtValue)
      complete("OK")
    } ~ get {
      complete(
      (ref ? GetAllTxtRecords).mapTo[Seq[String]]
        .map(_.mkString("[",",","]")))
    }
  }

  def start: Future[Http.ServerBinding] =
    Http().bindAndHandleAsync(
      Route.asyncHandler(route),
      "0.0.0.0", 6000)
}
