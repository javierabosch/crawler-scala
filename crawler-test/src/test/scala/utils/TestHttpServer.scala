package org.blikk.test

import org.blikk.crawler.Logging
import akka.pattern.ask
import akka.actor._
import akka.io._
import spray.can._
import spray.http._
import spray.http.StatusCodes._
import spray.routing._
import scala.concurrent.Await
import scala.concurrent.duration._
import akka.util.Timeout

class HttpServerListener extends HttpServiceActor with ActorLogging {

  def receive = runRoute {
    path("links" / IntNumber) { number =>
      val linkHtml : String = number match {
        case 1 => """<a href="/links/2">This is a link</a>"""
        case 2 => """<a href="/links/3">This is a link</a>"""
        case _ => ""
      }
      complete(s"""<html><body>${linkHtml}</body></html>""")
    } ~ 
    path("status" / "301") {
      redirect("/", StatusCodes.PermanentRedirect)
    } ~
    path("status" / "404") {
      respondWithStatus(StatusCodes.NotFound) { complete(StatusCodes.NotFound) }
    } ~
    path("status" / "503") {
      complete(StatusCodes.ServiceUnavailable)
    } ~
    complete("OK!")
  }

}

object TestHttpServer extends Logging {

  implicit val askTimeout = Timeout(1 seconds)

  // Starts a test HTTP server and returns the listener actor
  def start()(implicit system: ActorSystem) : ActorRef = {
    val listener = system.actorOf(Props[HttpServerListener])
    val boundFuture = (IO(Http) ? Http.Bind(listener, 
      TestConfig.HttpServerHost, TestConfig.HttpServerPort)).mapTo[Http.Bound]
    val boundResult = Await.result(boundFuture, 1.seconds)
    log.info(s"HTTP server started: ${boundResult.toString}")
    listener
  }

}