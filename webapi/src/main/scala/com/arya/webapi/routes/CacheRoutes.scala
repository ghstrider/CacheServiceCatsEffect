package com.arya.webapi.routes

import cats.effect.Async
import cats.implicits._
import com.arya.cache.CacheServiceImpl
import com.arya.dsl.KeyValueStore
import com.arya.webapi.model._
import com.arya.webapi.model.ApiModels._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import scala.concurrent.duration._

class CacheRoutes[F[_]: Async](implicit kvStore: KeyValueStore[F, String, String]) extends Http4sDsl[F] {
  
  private val cacheService = CacheServiceImpl[F]
  
  // JSON decoders for request entities
  implicit val putRequestEntityDecoder: EntityDecoder[F, PutRequest] = jsonOf[F, PutRequest]
  
  val routes: HttpRoutes[F] = HttpRoutes.of[F] {
    
    // GET /api/cache/{key} - Get value by key
    case GET -> Root / "api" / "cache" / key =>
      cacheService.get(key).flatMap {
        case Some(value) =>
          Ok(GetResponse(value).asJson)
        case None =>
          NotFound(ErrorResponse("NOT_FOUND", s"Key '$key' not found").asJson)
      }
    
    // PUT /api/cache/{key} - Store value with optional TTL
    case req @ PUT -> Root / "api" / "cache" / key =>
      for {
        putRequest <- req.as[PutRequest]
        response <- putRequest.ttl match {
          case Some(ttlSeconds) if ttlSeconds > 0 =>
            val ttlDuration = ttlSeconds.seconds
            cacheService.putWithTTL(key, putRequest.value, ttlDuration) *>
            Ok(PutResponse(s"Successfully stored key '$key' with TTL of $ttlSeconds seconds", key, Some(ttlSeconds)).asJson)
          case _ =>
            cacheService.put(key, putRequest.value) *>
            Ok(PutResponse(s"Successfully stored key '$key'", key).asJson)
        }
      } yield response
    
    // DELETE /api/cache/{key} - Delete key
    case DELETE -> Root / "api" / "cache" / key =>
      cacheService.del(key) *>
      Ok(DeleteResponse(s"Successfully deleted key '$key'", key).asJson)
    
    // GET /api/cache/{key}/ttl - Get remaining TTL for key
    case GET -> Root / "api" / "cache" / key / "ttl" =>
      cacheService.ttl(key).flatMap {
        case Some(duration) =>
          val seconds = duration.toSeconds
          Ok(TtlResponse(Some(seconds), s"TTL for key '$key': $seconds seconds").asJson)
        case None =>
          Ok(TtlResponse(None, s"Key '$key' has no TTL set or does not exist").asJson)
      }
    
    // GET /api/health - Health check endpoint
    case GET -> Root / "api" / "health" =>
      Ok(SuccessResponse("Cache service is healthy").asJson)
  }
  
  val routesWithErrorHandling: HttpRoutes[F] = routes
}

object CacheRoutes {
  def apply[F[_]: Async](implicit kvStore: KeyValueStore[F, String, String]): CacheRoutes[F] = 
    new CacheRoutes[F]
}