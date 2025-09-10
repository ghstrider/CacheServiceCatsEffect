package com.arya.webapi

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import com.arya.webapi.routes.CacheRoutes
import com.comcast.ip4s._
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.{CORS, Logger}
import org.http4s.server.Router
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.{Logger => Log4CatsLogger}
import com.arya.filestore.`implicit`._

object WebApiServer extends IOApp {
  
  implicit val logger: Log4CatsLogger[IO] = Slf4jLogger.getLogger[IO]
  
  def run(args: List[String]): IO[ExitCode] = {
    
    // Create cache routes with implicit KeyValueStore from filestore.implicit
    val cacheRoutes = CacheRoutes[IO].routesWithErrorHandling
    
    // Combine all routes
    val httpApp = Router(
      "/" -> cacheRoutes
    ).orNotFound
    
    // Add middleware
    val httpAppWithMiddleware = CORS.policy.withAllowOriginAll(
      Logger.httpApp(logHeaders = true, logBody = false)(httpApp)
    )
    
    // Build and start server
    EmberServerBuilder
      .default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8080")
      .withHttpApp(httpAppWithMiddleware)
      .build
      .use { server =>
        logger.info(s"Cache Web API server started at ${server.address}") *>
        logger.info("Available endpoints:") *>
        logger.info("  GET    /api/cache/{key}     - Get value by key") *>
        logger.info("  PUT    /api/cache/{key}     - Store value (with optional TTL)") *>
        logger.info("  DELETE /api/cache/{key}     - Delete key") *>
        logger.info("  GET    /api/cache/{key}/ttl - Get remaining TTL for key") *>
        logger.info("  GET    /api/health          - Health check") *>
        logger.info("Press CTRL+C to stop the server") *>
        IO.never
      }
      .as(ExitCode.Success)
  }
}