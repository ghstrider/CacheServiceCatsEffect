package com.arya.filestore

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import java.nio.file.{Files, Paths}
import scala.util.Try

class FileKVStoreSpec extends AsyncWordSpec with AsyncIOSpec with Matchers with BeforeAndAfterEach {
  
  val testFilePath = "test-filestore.kv"
  
  override def beforeEach(): Unit = {
    super.beforeEach()
    Try(Files.deleteIfExists(Paths.get(testFilePath)))
  }
  
  override def afterEach(): Unit = {
    super.afterEach()
    Try(Files.deleteIfExists(Paths.get(testFilePath)))
  }
  
  "FileKVStore" should {
    
    "store and retrieve a simple key-value pair" in {
      val fileStore = new Fs2FileKeyValueStore[IO, String, String](testFilePath, ".")
      val key = "test-key"
      val value = "test-value"
      
      val result = for {
        _ <- fileStore.put(key, value)
        retrieved <- fileStore.get(key)
      } yield retrieved
      
      result.asserting(_ shouldBe Some(value))
    }
    
    "return None for non-existent key" in {
      val fileStore = new Fs2FileKeyValueStore[IO, String, String](testFilePath, ".")
      
      fileStore.get("non-existent").asserting(_ shouldBe None)
    }
    
    "update existing key with new value" in {
      val fileStore = new Fs2FileKeyValueStore[IO, String, String](testFilePath, ".")
      val key = "update-key"
      val initialValue = "initial"
      val updatedValue = "updated"
      
      val result = for {
        _ <- fileStore.put(key, initialValue)
        initial <- fileStore.get(key)
        _ <- fileStore.put(key, updatedValue)
        updated <- fileStore.get(key)
      } yield (initial, updated)
      
      result.asserting { case (initial, updated) =>
        initial shouldBe Some(initialValue)
        updated shouldBe Some(updatedValue)
      }
    }
    
    "delete an existing key" in {
      val fileStore = new Fs2FileKeyValueStore[IO, String, String](testFilePath, ".")
      val key = "delete-key"
      val value = "to-be-deleted"
      
      val result = for {
        _ <- fileStore.put(key, value)
        beforeDelete <- fileStore.get(key)
        _ <- fileStore.delete(key)
        afterDelete <- fileStore.get(key)
      } yield (beforeDelete, afterDelete)
      
      result.asserting { case (before, after) =>
        before shouldBe Some(value)
        after shouldBe None
      }
    }
    
    "handle multiple keys simultaneously" in {
      val fileStore = new Fs2FileKeyValueStore[IO, String, String](testFilePath, ".")
      val pairs = Map(
        "key1" -> "value1",
        "key2" -> "value2",
        "key3" -> "value3"
      )
      
      val result = for {
        _ <- pairs.toList.traverse { case (k, v) => fileStore.put(k, v) }
        retrieved <- pairs.keys.toList.traverse(fileStore.get)
      } yield retrieved
      
      result.asserting { values =>
        values should contain theSameElementsAs List(
          Some("value1"),
          Some("value2"),
          Some("value3")
        )
      }
    }
    
    "persist data across store instances" in {
      val firstStore = new Fs2FileKeyValueStore[IO, String, String](testFilePath, ".")
      val key = "persistent-key"
      val value = "persistent-value"
      
      val result = for {
        _ <- firstStore.put(key, value)
        // Create a new store instance
        secondStore = new Fs2FileKeyValueStore[IO, String, String](testFilePath, ".")
        retrieved <- secondStore.get(key)
      } yield retrieved
      
      result.asserting(_ shouldBe Some(value))
    }
    
    "handle special characters in keys and values" in {
      val fileStore = new Fs2FileKeyValueStore[IO, String, String](testFilePath, ".")
      val specialKey = "key:with:special@chars#"
      val specialValue = "value\nwith\nnewlines\tand\ttabs"
      
      val result = for {
        _ <- fileStore.put(specialKey, specialValue)
        retrieved <- fileStore.get(specialKey)
      } yield retrieved
      
      result.asserting(_ shouldBe Some(specialValue))
    }
    
    "handle empty string values" in {
      val fileStore = new Fs2FileKeyValueStore[IO, String, String](testFilePath, ".")
      val key = "empty-value-key"
      val emptyValue = ""
      
      val result = for {
        _ <- fileStore.put(key, emptyValue)
        retrieved <- fileStore.get(key)
      } yield retrieved
      
      result.asserting(_ shouldBe Some(emptyValue))
    }
    
    "handle sequential operations correctly" in {
      val fileStore = new Fs2FileKeyValueStore[IO, String, String](testFilePath, ".")
      val operations = (1 to 10).map { i =>
        fileStore.put(s"sequential-$i", s"value-$i")
      }.toList
      
      val result = for {
        // Execute operations sequentially to avoid file corruption
        _ <- operations.traverse(identity)
        values <- (1 to 10).map(i => fileStore.get(s"sequential-$i")).toList.traverse(identity)
      } yield values
      
      result.asserting { values =>
        values.zipWithIndex.foreach { case (value, index) =>
          value shouldBe Some(s"value-${index + 1}")
        }
        succeed
      }
    }
    
    "handle delete on non-existent key gracefully" in {
      val fileStore = new Fs2FileKeyValueStore[IO, String, String](testFilePath, ".")
      
      fileStore.delete("non-existent-key").asserting(_ shouldBe ())
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