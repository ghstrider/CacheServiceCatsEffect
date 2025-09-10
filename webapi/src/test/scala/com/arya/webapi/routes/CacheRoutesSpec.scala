package com.arya.webapi.routes

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import com.arya.dsl.KeyValueStore
import com.arya.webapi.model._
import com.arya.webapi.model.ApiModels._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.implicits._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.scalamock.scalatest.MockFactory
import scala.concurrent.duration._

class CacheRoutesSpec extends AsyncWordSpec with AsyncIOSpec with Matchers with MockFactory {
  
  "CacheRoutes" should {
    
    "return a value for an existing key" in {
      val mockStore = mock[KeyValueStore[IO, String, String]]
      val key = "test-key"
      val value = "test-value"
      
      (mockStore.get _).expects(key).returning(IO.pure(Some(value)))
      
      implicit val kvStore: KeyValueStore[IO, String, String] = mockStore
      val routes = CacheRoutes[IO].routes
      
      val request = Request[IO](Method.GET, uri"/api/cache/test-key")
      
      routes.orNotFound(request).flatMap { response =>
        response.status shouldBe Status.Ok
        response.as[GetResponse].map(_.value shouldBe value)
      }
    }
    
    "return 404 for a non-existent key" in {
      val mockStore = mock[KeyValueStore[IO, String, String]]
      val key = "non-existent"
      
      (mockStore.get _).expects(key).returning(IO.pure(None))
      
      implicit val kvStore: KeyValueStore[IO, String, String] = mockStore
      val routes = CacheRoutes[IO].routes
      
      val request = Request[IO](Method.GET, uri"/api/cache/non-existent")
      
      routes.orNotFound(request).flatMap { response =>
        response.status shouldBe Status.NotFound
        response.as[ErrorResponse].map { errorResp =>
          errorResp.error shouldBe "NOT_FOUND"
          errorResp.message should include("non-existent")
        }
      }
    }
    
    "store a value without TTL" in {
      val mockStore = mock[KeyValueStore[IO, String, String]]
      val key = "store-key"
      val value = "store-value"
      
      (mockStore.put _).expects(key, value).returning(IO.unit)
      
      implicit val kvStore: KeyValueStore[IO, String, String] = mockStore
      val routes = CacheRoutes[IO].routes
      
      val putRequest = PutRequest(value)
      val request = Request[IO](Method.PUT, uri"/api/cache/store-key")
        .withEntity(putRequest.asJson)
      
      routes.orNotFound(request).flatMap { response =>
        response.status shouldBe Status.Ok
        response.as[PutResponse].map { putResp =>
          putResp.key shouldBe key
          putResp.message should include("Successfully stored")
          putResp.ttl shouldBe None
        }
      }
    }
    
    "store a value with TTL" in {
      val mockStore = mock[KeyValueStore[IO, String, String]]
      val key = "ttl-key"
      val value = "ttl-value"
      val ttlSeconds = 300L
      
      (mockStore.putWithTTL _).expects(key, value, ttlSeconds.seconds).returning(IO.unit)
      
      implicit val kvStore: KeyValueStore[IO, String, String] = mockStore
      val routes = CacheRoutes[IO].routes
      
      val putRequest = PutRequest(value, Some(ttlSeconds))
      val request = Request[IO](Method.PUT, uri"/api/cache/ttl-key")
        .withEntity(putRequest.asJson)
      
      routes.orNotFound(request).flatMap { response =>
        response.status shouldBe Status.Ok
        response.as[PutResponse].map { putResp =>
          putResp.key shouldBe key
          putResp.message should include("with TTL")
          putResp.ttl shouldBe Some(ttlSeconds)
        }
      }
    }
    
    "delete a key" in {
      val mockStore = mock[KeyValueStore[IO, String, String]]
      val key = "delete-key"
      
      (mockStore.delete _).expects(key).returning(IO.unit)
      
      implicit val kvStore: KeyValueStore[IO, String, String] = mockStore
      val routes = CacheRoutes[IO].routes
      
      val request = Request[IO](Method.DELETE, uri"/api/cache/delete-key")
      
      routes.orNotFound(request).flatMap { response =>
        response.status shouldBe Status.Ok
        response.as[DeleteResponse].map { deleteResp =>
          deleteResp.key shouldBe key
          deleteResp.message should include("Successfully deleted")
        }
      }
    }
    
    "return TTL for a key with expiration" in {
      val mockStore = mock[KeyValueStore[IO, String, String]]
      val key = "ttl-check-key"
      val remainingTtl = 120.seconds
      
      (mockStore.ttl _).expects(key).returning(IO.pure(Some(remainingTtl)))
      
      implicit val kvStore: KeyValueStore[IO, String, String] = mockStore
      val routes = CacheRoutes[IO].routes
      
      val request = Request[IO](Method.GET, uri"/api/cache/ttl-check-key/ttl")
      
      routes.orNotFound(request).flatMap { response =>
        response.status shouldBe Status.Ok
        response.as[TtlResponse].map { ttlResp =>
          ttlResp.ttl shouldBe Some(120L)
          ttlResp.message should include("TTL for key")
        }
      }
    }
    
    "return no TTL for a key without expiration" in {
      val mockStore = mock[KeyValueStore[IO, String, String]]
      val key = "no-ttl-key"
      
      (mockStore.ttl _).expects(key).returning(IO.pure(None))
      
      implicit val kvStore: KeyValueStore[IO, String, String] = mockStore
      val routes = CacheRoutes[IO].routes
      
      val request = Request[IO](Method.GET, uri"/api/cache/no-ttl-key/ttl")
      
      routes.orNotFound(request).flatMap { response =>
        response.status shouldBe Status.Ok
        response.as[TtlResponse].map { ttlResp =>
          ttlResp.ttl shouldBe None
          ttlResp.message should include("has no TTL set")
        }
      }
    }
    
    "return health check" in {
      val mockStore = mock[KeyValueStore[IO, String, String]]
      implicit val kvStore: KeyValueStore[IO, String, String] = mockStore
      val routes = CacheRoutes[IO].routes
      
      val request = Request[IO](Method.GET, uri"/api/health")
      
      routes.orNotFound(request).flatMap { response =>
        response.status shouldBe Status.Ok
        response.as[SuccessResponse].map { healthResp =>
          healthResp.message should include("healthy")
        }
      }
    }
    
    "handle invalid JSON in PUT request" in {
      val mockStore = mock[KeyValueStore[IO, String, String]]
      implicit val kvStore: KeyValueStore[IO, String, String] = mockStore
      val routes = CacheRoutes[IO].routesWithErrorHandling
      
      val request = Request[IO](Method.PUT, uri"/api/cache/invalid-json")
        .withEntity("invalid json")
      
      routes.orNotFound(request).flatMap { response =>
        response.status shouldBe Status.BadRequest
        response.as[ErrorResponse].map { errorResp =>
          errorResp.error shouldBe "BAD_REQUEST"
          errorResp.message should include("Invalid JSON")
        }
      }
    }
  }
}