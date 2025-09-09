package com.arya

import cats.effect.{IO, IOApp}
import com.arya.cache.CacheServiceImpl
import com.arya.dsl.KeyValueStore
import dev.profunktor.redis4cats.data.RedisCodec
import com.arya.redisstore.RedisStore
import dev.profunktor.redis4cats.log4cats._
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import cats.effect.Async
import com.arya.filestore.Fs2FileKeyValueStore

object CacheServiceMain extends IOApp.Simple {
//  val fs2File = new Fs2FileKeyValueStore[IO, String, String]
//  val kv = fileKVStore[IO, String, String](fs2File)
  val fs2File: Fs2FileKeyValueStore[IO, String, String] = KeyValueStore.file[IO, String, String]()

  implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]
  val redisCodec: RedisCodec[String, String] = RedisCodec.Utf8
  val rKv: RedisStore[IO, String, String] = KeyValueStore.redis[IO, String, String]("localhost", 6379, redisCodec)
  //new RedisStore[IO, String, String](redisCodec)

  implicit val combinedRedisAndFile: KeyValueStore[IO, String, String] =
    KeyValueStore.retry(rKv, fs2File)
  override def run: IO[Unit] = CacheServiceImpl[IO].make.use(store => for {
//    _ <- store.put("1","Lets rock")
    first <- store.get("1")
//    _ <- store.put("2", "Code functional")
    _ <- IO.println(first)
//    second <- store.get("2")
  } yield (first)).as(())

}
