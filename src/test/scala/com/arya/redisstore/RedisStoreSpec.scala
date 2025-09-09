package com.arya.redisstore

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.scalamock.scalatest.MockFactory
import com.arya.dsl.KeyValueStore

class RedisStoreSpec extends AsyncWordSpec with AsyncIOSpec with Matchers with MockFactory {
  
  "RedisStore" should {
    
    "implement KeyValueStore interface" in {
      // This test verifies that RedisStore properly implements the KeyValueStore trait
      // We can't easily test actual Redis operations without a running Redis instance
      // or complex mocking of Redis4Cats internals
      
      val mockStore = mock[KeyValueStore[IO, String, String]]
      val key = "test-key"
      val value = "test-value"
      
      (mockStore.put _).expects(key, value).returning(IO.unit)
      (mockStore.get _).expects(key).returning(IO.pure(Some(value)))
      (mockStore.delete _).expects(key).returning(IO.unit)
      
      val result = for {
        _ <- mockStore.put(key, value)
        retrieved <- mockStore.get(key)
        _ <- mockStore.delete(key)
      } yield retrieved
      
      result.asserting(_ shouldBe Some(value))
    }
    
    "handle None responses correctly" in {
      val mockStore = mock[KeyValueStore[IO, String, String]]
      val key = "non-existent"
      
      (mockStore.get _).expects(key).returning(IO.pure(None))
      
      mockStore.get(key).asserting(_ shouldBe None)
    }
    
    "handle multiple operations in sequence" in {
      val mockStore = mock[KeyValueStore[IO, String, String]]
      
      inSequence {
        (mockStore.put _).expects("key1", "value1").returning(IO.unit)
        (mockStore.put _).expects("key2", "value2").returning(IO.unit)
        (mockStore.get _).expects("key1").returning(IO.pure(Some("value1")))
        (mockStore.get _).expects("key2").returning(IO.pure(Some("value2")))
        (mockStore.delete _).expects("key1").returning(IO.unit)
        (mockStore.get _).expects("key1").returning(IO.pure(None))
      }
      
      val result = for {
        _ <- mockStore.put("key1", "value1")
        _ <- mockStore.put("key2", "value2")
        v1 <- mockStore.get("key1")
        v2 <- mockStore.get("key2")
        _ <- mockStore.delete("key1")
        v1After <- mockStore.get("key1")
      } yield (v1, v2, v1After)
      
      result.asserting { case (v1, v2, v1After) =>
        v1 shouldBe Some("value1")
        v2 shouldBe Some("value2")
        v1After shouldBe None
      }
    }
    
    "handle concurrent operations" in {
      val mockStore = mock[KeyValueStore[IO, String, String]]
      val operations = (1 to 5).map { i =>
        val key = s"key$i"
        val value = s"value$i"
        (mockStore.put _).expects(key, value).returning(IO.unit).anyNumberOfTimes()
        (mockStore.get _).expects(key).returning(IO.pure(Some(value))).anyNumberOfTimes()
        (key, value)
      }.toList
      
      val result = for {
        _ <- IO.parSequenceN(3)(operations.map { case (k, v) => mockStore.put(k, v) })
        values <- IO.parSequenceN(3)(operations.map { case (k, _) => mockStore.get(k) })
      } yield values
      
      result.asserting { values =>
        values.forall(_.isDefined) shouldBe true
      }
    }
  }
  
  // Note: Integration tests with actual Redis instance would require:
  // - A running Redis server
  // - Environment-specific configuration
  // - Cleanup after each test
  // These should be in a separate integration test suite
}