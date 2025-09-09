package com.arya.filestore

import cats.effect.IO
import com.arya.dsl.KeyValueStore
import com.arya.filestore.FileKVStore.fileKVStore

package object `implicit` {
  implicit val fs2File: Fs2FileKeyValueStore[IO, String, String] = new Fs2FileKeyValueStore[IO, String, String]("filestore.kv", System.getProperty("user.dir"))
  implicit val kv: KeyValueStore[IO, String, String] = fileKVStore[IO, String, String](fs2File)
}
