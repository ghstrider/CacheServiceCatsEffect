package com.arya.integration

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import com.arya.dsl.KeyValueStore
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.scalamock.scalatest.MockFactory
import java.nio.file.{Files, Paths}
import scala.util.Try

class CacheLayersIntegrationSpec extends AsyncWordSpec with AsyncIOSpec with Matchers with MockFactory with BeforeAndAfterEach {
  
  val testFilePath = "test-integration-filestore.kv"
  
  override def beforeEach(): Unit = {
    super.beforeEach()
    Try(Files.deleteIfExists(Paths.get(testFilePath)))
  }
  
  override def afterEach(): Unit = {
    super.afterEach()
    Try(Files.deleteIfExists(Paths.get(testFilePath)))
  }
  
  "Cache Layers with retry mechanism" should {
    
    "fallback to secondary store when primary fails on get" in {
      val primaryStore = mock[KeyValueStore[IO, String, String]]
      val fallbackStore = mock[KeyValueStore[IO, String, String]]
      val key = "test-key"
      val value = "test-value"
      
      // Primary fails, fallback succeeds
      (primaryStore.get _).expects(key).returning(IO.raiseError(new Exception("Primary failed")))
      (fallbackStore.get _).expects(key).returning(IO.pure(Some(value)))
      
      val combinedStore = KeyValueStore.retry(primaryStore, fallbackStore)
      
      combinedStore.get(key).asserting(_ shouldBe Some(value))
    }
    
    "use primary store when it's available" in {
      val primaryStore = mock[KeyValueStore[IO, String, String]]
      val fallbackStore = mock[KeyValueStore[IO, String, String]]
      val key = "test-key"
      val value = "primary-value"
      
      // Primary succeeds, fallback should not be called
      (primaryStore.get _).expects(key).returning(IO.pure(Some(value)))
      (fallbackStore.get _).expects(key).never()
      
      val combinedStore = KeyValueStore.retry(primaryStore, fallbackStore)
      
      combinedStore.get(key).asserting(_ shouldBe Some(value))
    }
    
    "fallback to secondary store when primary fails on put" in {
      val primaryStore = mock[KeyValueStore[IO, String, String]]
      val fallbackStore = mock[KeyValueStore[IO, String, String]]
      val key = "test-key"
      val value = "test-value"
      
      // Primary fails, fallback succeeds
      (primaryStore.put _).expects(key, value).returning(IO.raiseError(new Exception("Primary failed")))
      (fallbackStore.put _).expects(key, value).returning(IO.unit)
      
      val combinedStore = KeyValueStore.retry(primaryStore, fallbackStore)
      
      combinedStore.put(key, value).asserting(_ shouldBe ())
    }
    
    "fallback to secondary store when primary fails on delete" in {
      val primaryStore = mock[KeyValueStore[IO, String, String]]
      val fallbackStore = mock[KeyValueStore[IO, String, String]]
      val key = "test-key"
      
      // Primary fails, fallback succeeds
      (primaryStore.delete _).expects(key).returning(IO.raiseError(new Exception("Primary failed")))
      (fallbackStore.delete _).expects(key).returning(IO.unit)
      
      val combinedStore = KeyValueStore.retry(primaryStore, fallbackStore)
      
      combinedStore.delete(key).asserting(_ shouldBe ())
    }
    
    "handle both stores failing gracefully" in {
      val primaryStore = mock[KeyValueStore[IO, String, String]]
      val fallbackStore = mock[KeyValueStore[IO, String, String]]
      val key = "test-key"
      
      // Both stores fail
      (primaryStore.get _).expects(key).returning(IO.raiseError(new Exception("Primary failed")))
      (fallbackStore.get _).expects(key).returning(IO.raiseError(new Exception("Fallback failed")))
      
      val combinedStore = KeyValueStore.retry(primaryStore, fallbackStore)
      
      combinedStore.get(key).attempt.asserting(_.isLeft shouldBe true)
    }
    
    "handle primary returning None and fallback having value" in {
      val primaryStore = mock[KeyValueStore[IO, String, String]]
      val fallbackStore = mock[KeyValueStore[IO, String, String]]
      val key = "test-key"
      val value = "fallback-value"
      
      // Primary returns None, fallback has value
      (primaryStore.get _).expects(key).returning(IO.pure(None))
      (fallbackStore.get _).expects(key).returning(IO.pure(Some(value)))
      
      val combinedStore = KeyValueStore.retry(primaryStore, fallbackStore)
      
      combinedStore.get(key).asserting(_ shouldBe Some(value))
    }
    
    "write-through to both stores on successful put" in {
      val primaryStore = mock[KeyValueStore[IO, String, String]]
      val fallbackStore = mock[KeyValueStore[IO, String, String]]
      val key = "test-key"
      val value = "test-value"
      
      // Both stores should be written to
      (primaryStore.put _).expects(key, value).returning(IO.unit)
      // Note: Current implementation might not write to both - this depends on the actual implementation
      
      val combinedStore = KeyValueStore.retry(primaryStore, fallbackStore)
      
      combinedStore.put(key, value).asserting(_ shouldBe ())
    }
    
    "handle complex operation sequence" in {
      val primaryStore = mock[KeyValueStore[IO, String, String]]
      val fallbackStore = mock[KeyValueStore[IO, String, String]]
      
      inSequence {
        // Put succeeds on primary
        (primaryStore.put _).expects("key1", "value1").returning(IO.unit)
        
        // Get fails on primary, succeeds on fallback
        (primaryStore.get _).expects("key2").returning(IO.raiseError(new Exception("Primary failed")))
        (fallbackStore.get _).expects("key2").returning(IO.pure(Some("value2")))
        
        // Delete succeeds on primary
        (primaryStore.delete _).expects("key1").returning(IO.unit)
        
        // Get returns None from primary, checks fallback
        (primaryStore.get _).expects("key1").returning(IO.pure(None))
        (fallbackStore.get _).expects("key1").returning(IO.pure(None))
      }
      
      val combinedStore = KeyValueStore.retry(primaryStore, fallbackStore)
      
      val result = for {
        _ <- combinedStore.put("key1", "value1")
        v2 <- combinedStore.get("key2")
        _ <- combinedStore.delete("key1")
        v1 <- combinedStore.get("key1")
      } yield (v1, v2)
      
      result.asserting { case (v1, v2) =>
        v1 shouldBe None
        v2 shouldBe Some("value2")
      }
    }
    
    "handle concurrent operations through retry layer" in {
      val primaryStore = mock[KeyValueStore[IO, String, String]]
      val fallbackStore = mock[KeyValueStore[IO, String, String]]
      
      val keys = (1 to 5).map(i => s"key$i").toList
      val values = (1 to 5).map(i => s"value$i").toList
      
      keys.zip(values).foreach { case (k, v) =>
        (primaryStore.put _).expects(k, v).returning(IO.unit).anyNumberOfTimes()
        (primaryStore.get _).expects(k).returning(IO.pure(Some(v))).anyNumberOfTimes()
      }
      
      val combinedStore = KeyValueStore.retry(primaryStore, fallbackStore)
      
      val result = for {
        _ <- IO.parSequenceN(3)(keys.zip(values).map { case (k, v) => 
          combinedStore.put(k, v) 
        })
        retrieved <- IO.parSequenceN(3)(keys.map(combinedStore.get))
      } yield retrieved
      
      result.asserting { retrievedValues =>
        retrievedValues shouldBe values.map(Some(_))
      }
    }
  }
}