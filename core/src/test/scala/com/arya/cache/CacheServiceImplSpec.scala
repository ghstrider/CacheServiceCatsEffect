package com.arya.cache

import cats.effect.{IO, Resource}
import cats.effect.testing.scalatest.AsyncIOSpec
import com.arya.dsl.KeyValueStore
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.scalamock.scalatest.MockFactory
import scala.concurrent.duration._

class CacheServiceImplSpec extends AsyncWordSpec with AsyncIOSpec with Matchers with MockFactory {
  
  "CacheServiceImpl" should {
    
    "successfully store and retrieve a value" in {
      val mockStore = mock[KeyValueStore[IO, String, String]]
      val key = "test-key"
      val value = "test-value"
      
      (mockStore.put _).expects(key, value).returning(IO.unit)
      (mockStore.get _).expects(key).returning(IO.pure(Some(value)))
      
      implicit val kvStore: KeyValueStore[IO, String, String] = mockStore
      val cacheService = CacheServiceImpl[IO]
      
      val result = for {
        _ <- cacheService.put(key, value)
        retrieved <- cacheService.get(key)
      } yield retrieved
      
      result.asserting(_ shouldBe Some(value))
    }
    
    "return None when key doesn't exist" in {
      val mockStore = mock[KeyValueStore[IO, String, String]]
      val key = "non-existent-key"
      
      (mockStore.get _).expects(key).returning(IO.pure(None))
      
      implicit val kvStore: KeyValueStore[IO, String, String] = mockStore
      val cacheService = CacheServiceImpl[IO]
      
      cacheService.get(key).asserting(_ shouldBe None)
    }
    
    "successfully delete a key" in {
      val mockStore = mock[KeyValueStore[IO, String, String]]
      val key = "key-to-delete"
      
      (mockStore.delete _).expects(key).returning(IO.unit)
      
      implicit val kvStore: KeyValueStore[IO, String, String] = mockStore
      val cacheService = CacheServiceImpl[IO]
      
      cacheService.del(key).asserting(_ shouldBe ())
    }
    
    "handle multiple operations in sequence" in {
      val mockStore = mock[KeyValueStore[IO, String, String]]
      val key1 = "key1"
      val value1 = "value1"
      val key2 = "key2"
      val value2 = "value2"
      
      inSequence {
        (mockStore.put _).expects(key1, value1).returning(IO.unit)
        (mockStore.put _).expects(key2, value2).returning(IO.unit)
        (mockStore.get _).expects(key1).returning(IO.pure(Some(value1)))
        (mockStore.get _).expects(key2).returning(IO.pure(Some(value2)))
        (mockStore.delete _).expects(key1).returning(IO.unit)
        (mockStore.get _).expects(key1).returning(IO.pure(None))
      }
      
      implicit val kvStore: KeyValueStore[IO, String, String] = mockStore
      val cacheService = CacheServiceImpl[IO]
      
      val result = for {
        _ <- cacheService.put(key1, value1)
        _ <- cacheService.put(key2, value2)
        v1 <- cacheService.get(key1)
        v2 <- cacheService.get(key2)
        _ <- cacheService.del(key1)
        v1After <- cacheService.get(key1)
      } yield (v1, v2, v1After)
      
      result.asserting { case (v1, v2, v1After) =>
        v1 shouldBe Some(value1)
        v2 shouldBe Some(value2)
        v1After shouldBe None
      }
    }
    
    "handle update of existing key" in {
      val mockStore = mock[KeyValueStore[IO, String, String]]
      val key = "update-key"
      val initialValue = "initial"
      val updatedValue = "updated"
      
      inSequence {
        (mockStore.put _).expects(key, initialValue).returning(IO.unit)
        (mockStore.get _).expects(key).returning(IO.pure(Some(initialValue)))
        (mockStore.put _).expects(key, updatedValue).returning(IO.unit)
        (mockStore.get _).expects(key).returning(IO.pure(Some(updatedValue)))
      }
      
      implicit val kvStore: KeyValueStore[IO, String, String] = mockStore
      val cacheService = CacheServiceImpl[IO]
      
      val result = for {
        _ <- cacheService.put(key, initialValue)
        initial <- cacheService.get(key)
        _ <- cacheService.put(key, updatedValue)
        updated <- cacheService.get(key)
      } yield (initial, updated)
      
      result.asserting { case (initial, updated) =>
        initial shouldBe Some(initialValue)
        updated shouldBe Some(updatedValue)
      }
    }
    
    "handle empty string values" in {
      val mockStore = mock[KeyValueStore[IO, String, String]]
      val key = "empty-key"
      val emptyValue = ""
      
      (mockStore.put _).expects(key, emptyValue).returning(IO.unit)
      (mockStore.get _).expects(key).returning(IO.pure(Some(emptyValue)))
      
      implicit val kvStore: KeyValueStore[IO, String, String] = mockStore
      val cacheService = CacheServiceImpl[IO]
      
      val result = for {
        _ <- cacheService.put(key, emptyValue)
        retrieved <- cacheService.get(key)
      } yield retrieved
      
      result.asserting(_ shouldBe Some(emptyValue))
    }
    
    "handle special characters in keys and values" in {
      val mockStore = mock[KeyValueStore[IO, String, String]]
      val specialKey = "key:with:colons:and-dashes_underscores"
      val specialValue = "value with spaces, tabs\tand\nnewlines"
      
      (mockStore.put _).expects(specialKey, specialValue).returning(IO.unit)
      (mockStore.get _).expects(specialKey).returning(IO.pure(Some(specialValue)))
      
      implicit val kvStore: KeyValueStore[IO, String, String] = mockStore
      val cacheService = CacheServiceImpl[IO]
      
      val result = for {
        _ <- cacheService.put(specialKey, specialValue)
        retrieved <- cacheService.get(specialKey)
      } yield retrieved
      
      result.asserting(_ shouldBe Some(specialValue))
    }
    
    "store value with TTL" in {
      val mockStore = mock[KeyValueStore[IO, String, String]]
      val key = "ttl-key"
      val value = "ttl-value"
      val ttl = 30.seconds
      
      (mockStore.putWithTTL _).expects(key, value, ttl).returning(IO.unit)
      (mockStore.get _).expects(key).returning(IO.pure(Some(value)))
      
      implicit val kvStore: KeyValueStore[IO, String, String] = mockStore
      val cacheService = CacheServiceImpl[IO]
      
      val result = for {
        _ <- cacheService.putWithTTL(key, value, ttl)
        retrieved <- cacheService.get(key)
      } yield retrieved
      
      result.asserting(_ shouldBe Some(value))
    }
    
    "retrieve TTL for a key" in {
      val mockStore = mock[KeyValueStore[IO, String, String]]
      val key = "ttl-check-key"
      val remainingTtl = 25.seconds
      
      (mockStore.ttl _).expects(key).returning(IO.pure(Some(remainingTtl)))
      
      implicit val kvStore: KeyValueStore[IO, String, String] = mockStore
      val cacheService = CacheServiceImpl[IO]
      
      cacheService.ttl(key).asserting(_ shouldBe Some(remainingTtl))
    }
    
    "return None for TTL when key has no expiration" in {
      val mockStore = mock[KeyValueStore[IO, String, String]]
      val key = "no-ttl-key"
      
      (mockStore.ttl _).expects(key).returning(IO.pure(None))
      
      implicit val kvStore: KeyValueStore[IO, String, String] = mockStore
      val cacheService = CacheServiceImpl[IO]
      
      cacheService.ttl(key).asserting(_ shouldBe None)
    }
    
    "handle full TTL workflow" in {
      val mockStore = mock[KeyValueStore[IO, String, String]]
      val key = "workflow-key"
      val value = "workflow-value"
      val ttl = 60.seconds
      val remainingTtl = 45.seconds
      
      inSequence {
        (mockStore.putWithTTL _).expects(key, value, ttl).returning(IO.unit)
        (mockStore.get _).expects(key).returning(IO.pure(Some(value)))
        (mockStore.ttl _).expects(key).returning(IO.pure(Some(remainingTtl)))
        (mockStore.delete _).expects(key).returning(IO.unit)
        (mockStore.ttl _).expects(key).returning(IO.pure(None))
      }
      
      implicit val kvStore: KeyValueStore[IO, String, String] = mockStore
      val cacheService = CacheServiceImpl[IO]
      
      val result = for {
        _ <- cacheService.putWithTTL(key, value, ttl)
        retrieved <- cacheService.get(key)
        currentTtl <- cacheService.ttl(key)
        _ <- cacheService.del(key)
        ttlAfterDelete <- cacheService.ttl(key)
      } yield (retrieved, currentTtl, ttlAfterDelete)
      
      result.asserting { case (retrieved, currentTtl, ttlAfterDelete) =>
        retrieved shouldBe Some(value)
        currentTtl shouldBe Some(remainingTtl)
        ttlAfterDelete shouldBe None
      }
    }
  }
}