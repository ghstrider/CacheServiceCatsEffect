package com.arya.filestore

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import java.nio.file.{Files, Paths}
import scala.util.Try
import scala.concurrent.duration._

class Fs2FileKeyValueStoreTTLSpec extends AsyncWordSpec with AsyncIOSpec with Matchers with BeforeAndAfterEach {
  
  val testFilePath = "test-ttl-filestore.kv"
  val testTtlFilePath = "test-ttl-filestore.kv.ttl"
  
  override def beforeEach(): Unit = {
    super.beforeEach()
    Try(Files.deleteIfExists(Paths.get(testFilePath)))
    Try(Files.deleteIfExists(Paths.get(testTtlFilePath)))
  }
  
  override def afterEach(): Unit = {
    super.afterEach()
    Try(Files.deleteIfExists(Paths.get(testFilePath)))
    Try(Files.deleteIfExists(Paths.get(testTtlFilePath)))
  }
  
  "Fs2FileKeyValueStoreTTL" should {
    
    "store and retrieve a value with TTL" in {
      val ttlStore = new Fs2FileKeyValueStoreTTL[IO, String, String](testFilePath, ".")
      val key = "ttl-key"
      val value = "ttl-value"
      val ttl = 30.seconds
      
      val result = for {
        _ <- ttlStore.putWithTTL(key, value, ttl)
        retrieved <- ttlStore.get(key)
        currentTtl <- ttlStore.ttl(key)
      } yield (retrieved, currentTtl)
      
      result.asserting { case (retrieved, currentTtl) =>
        retrieved shouldBe Some(value)
        currentTtl.isDefined shouldBe true
        currentTtl.get.toSeconds should be <= 30L
        currentTtl.get.toSeconds should be > 25L // Allow some time for execution
      }
    }
    
    "store without TTL using regular put" in {
      val ttlStore = new Fs2FileKeyValueStoreTTL[IO, String, String](testFilePath, ".")
      val key = "no-ttl-key"
      val value = "no-ttl-value"
      
      val result = for {
        _ <- ttlStore.put(key, value)
        retrieved <- ttlStore.get(key)
        currentTtl <- ttlStore.ttl(key)
      } yield (retrieved, currentTtl)
      
      result.asserting { case (retrieved, currentTtl) =>
        retrieved shouldBe Some(value)
        currentTtl shouldBe None
      }
    }
    
    "return None for TTL of non-existent key" in {
      val ttlStore = new Fs2FileKeyValueStoreTTL[IO, String, String](testFilePath, ".")
      
      ttlStore.ttl("non-existent-key").asserting(_ shouldBe None)
    }
    
    "handle expired entries" in {
      val ttlStore = new Fs2FileKeyValueStoreTTL[IO, String, String](testFilePath, ".")
      val key = "expired-key"
      val value = "expired-value"
      val shortTtl = 1.milli // Very short TTL
      
      val result = for {
        _ <- ttlStore.putWithTTL(key, value, shortTtl)
        _ <- IO.sleep(50.millis) // Wait for expiration
        retrieved <- ttlStore.get(key) // This should trigger cleanup
        currentTtl <- ttlStore.ttl(key)
      } yield (retrieved, currentTtl)
      
      result.asserting { case (retrieved, currentTtl) =>
        retrieved shouldBe None
        currentTtl shouldBe None
      }
    }
    
    "clean up expired entries but keep valid ones" in {
      val ttlStore = new Fs2FileKeyValueStoreTTL[IO, String, String](testFilePath, ".")
      val expiredKey = "expired-key"
      val validKey = "valid-key"
      val expiredValue = "expired-value"
      val validValue = "valid-value"
      
      val result = for {
        _ <- ttlStore.putWithTTL(expiredKey, expiredValue, 1.milli)
        _ <- ttlStore.putWithTTL(validKey, validValue, 30.seconds)
        _ <- IO.sleep(50.millis) // Wait for first key to expire
        expiredRetrieved <- ttlStore.get(expiredKey)
        validRetrieved <- ttlStore.get(validKey)
        expiredTtl <- ttlStore.ttl(expiredKey)
        validTtl <- ttlStore.ttl(validKey)
      } yield (expiredRetrieved, validRetrieved, expiredTtl, validTtl)
      
      result.asserting { case (expiredRetrieved, validRetrieved, expiredTtl, validTtl) =>
        expiredRetrieved shouldBe None
        validRetrieved shouldBe Some(validValue)
        expiredTtl shouldBe None
        validTtl.isDefined shouldBe true
      }
    }
    
    "update TTL when key is overwritten with putWithTTL" in {
      val ttlStore = new Fs2FileKeyValueStoreTTL[IO, String, String](testFilePath, ".")
      val key = "update-ttl-key"
      val value1 = "value1"
      val value2 = "value2"
      val shortTtl = 5.seconds
      val longTtl = 60.seconds
      
      val result = for {
        _ <- ttlStore.putWithTTL(key, value1, shortTtl)
        firstTtl <- ttlStore.ttl(key)
        _ <- ttlStore.putWithTTL(key, value2, longTtl)
        secondTtl <- ttlStore.ttl(key)
        retrieved <- ttlStore.get(key)
      } yield (firstTtl, secondTtl, retrieved)
      
      result.asserting { case (firstTtl, secondTtl, retrieved) =>
        firstTtl.isDefined shouldBe true
        firstTtl.get.toSeconds should be <= 5L
        secondTtl.isDefined shouldBe true
        secondTtl.get.toSeconds should be > 50L
        retrieved shouldBe Some(value2)
      }
    }
    
    "remove TTL when key is overwritten with regular put" in {
      val ttlStore = new Fs2FileKeyValueStoreTTL[IO, String, String](testFilePath, ".")
      val key = "remove-ttl-key"
      val value1 = "value1"
      val value2 = "value2"
      val ttl = 30.seconds
      
      val result = for {
        _ <- ttlStore.putWithTTL(key, value1, ttl)
        firstTtl <- ttlStore.ttl(key)
        _ <- ttlStore.put(key, value2) // Regular put should remove TTL
        secondTtl <- ttlStore.ttl(key)
        retrieved <- ttlStore.get(key)
      } yield (firstTtl, secondTtl, retrieved)
      
      result.asserting { case (firstTtl, secondTtl, retrieved) =>
        firstTtl.isDefined shouldBe true
        secondTtl shouldBe None
        retrieved shouldBe Some(value2)
      }
    }
    
    "delete key and its TTL" in {
      val ttlStore = new Fs2FileKeyValueStoreTTL[IO, String, String](testFilePath, ".")
      val key = "delete-ttl-key"
      val value = "delete-ttl-value"
      val ttl = 30.seconds
      
      val result = for {
        _ <- ttlStore.putWithTTL(key, value, ttl)
        beforeDelete <- ttlStore.get(key)
        beforeDeleteTtl <- ttlStore.ttl(key)
        _ <- ttlStore.delete(key)
        afterDelete <- ttlStore.get(key)
        afterDeleteTtl <- ttlStore.ttl(key)
      } yield (beforeDelete, beforeDeleteTtl, afterDelete, afterDeleteTtl)
      
      result.asserting { case (beforeDelete, beforeDeleteTtl, afterDelete, afterDeleteTtl) =>
        beforeDelete shouldBe Some(value)
        beforeDeleteTtl.isDefined shouldBe true
        afterDelete shouldBe None
        afterDeleteTtl shouldBe None
      }
    }
    
    "persist TTL data across store instances" in {
      val key = "persistent-ttl-key"
      val value = "persistent-ttl-value"
      val ttl = 30.seconds
      
      val result = for {
        firstStore <- IO.pure(new Fs2FileKeyValueStoreTTL[IO, String, String](testFilePath, "."))
        _ <- firstStore.putWithTTL(key, value, ttl)
        firstTtl <- firstStore.ttl(key)
        // Create a new store instance
        secondStore <- IO.pure(new Fs2FileKeyValueStoreTTL[IO, String, String](testFilePath, "."))
        retrieved <- secondStore.get(key)
        secondTtl <- secondStore.ttl(key)
      } yield (firstTtl, retrieved, secondTtl)
      
      result.asserting { case (firstTtl, retrieved, secondTtl) =>
        firstTtl.isDefined shouldBe true
        retrieved shouldBe Some(value)
        secondTtl.isDefined shouldBe true
        secondTtl.get.toSeconds should be <= firstTtl.get.toSeconds
      }
    }
    
    "handle multiple keys with different TTLs" in {
      val ttlStore = new Fs2FileKeyValueStoreTTL[IO, String, String](testFilePath, ".")
      
      val result = for {
        _ <- ttlStore.putWithTTL("key1", "value1", 5.seconds)
        _ <- ttlStore.putWithTTL("key2", "value2", 10.seconds)
        _ <- ttlStore.put("key3", "value3") // No TTL
        ttl1 <- ttlStore.ttl("key1")
        ttl2 <- ttlStore.ttl("key2")
        ttl3 <- ttlStore.ttl("key3")
        val1 <- ttlStore.get("key1")
        val2 <- ttlStore.get("key2")
        val3 <- ttlStore.get("key3")
      } yield (ttl1, ttl2, ttl3, val1, val2, val3)
      
      result.asserting { case (ttl1, ttl2, ttl3, val1, val2, val3) =>
        ttl1.isDefined shouldBe true
        ttl1.get.toSeconds should be <= 5L
        ttl2.isDefined shouldBe true
        ttl2.get.toSeconds should be <= 10L
        ttl3 shouldBe None
        val1 shouldBe Some("value1")
        val2 shouldBe Some("value2")
        val3 shouldBe Some("value3")
      }
    }
  }
  
  implicit class TraverseOps[A](list: List[A]) {
    def traverse[B](f: A => IO[B]): IO[List[B]] = 
      list.foldLeft(IO.pure(List.empty[B])) { (acc, a) =>
        for {
          accList <- acc
          b <- f(a)
        } yield accList :+ b
      }
  }
}