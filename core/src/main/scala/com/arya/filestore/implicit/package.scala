package com.arya.filestore

import cats.effect.{Clock, IO}
import com.arya.dsl.KeyValueStore
import com.arya.filestore.FileKVStore.fileKVStore
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

package object `implicit` {
  // Use TTL-aware file store in user home directory
  implicit val fs2File: Fs2FileKeyValueStoreTTL[IO, String, String] = new Fs2FileKeyValueStoreTTL[IO, String, String]("filestore.kv", System.getProperty("user.home"))
  implicit val kv: KeyValueStore[IO, String, String] = fs2File
}
