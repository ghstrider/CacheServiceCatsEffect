package com.arya.dsl

import cats.Monad
import cats.data.OptionT
import cats.effect.{Concurrent, Sync}
import com.arya.filestore.Fs2FileKeyValueStore
import com.arya.redisstore.RedisStore
import dev.profunktor.redis4cats.connection.RedisURI
import dev.profunktor.redis4cats.data.RedisCodec
import dev.profunktor.redis4cats.effect.MkRedis
import fs2.io.file.Files
import io.lettuce.core.{RedisURI => JRedisURI}

trait KeyValueStore[F[_], K, V] {
  def get(k: K): F[Option[V]]
  def put(k: K, v: V): F[Unit]
  def delete(k: K): F[Unit]

}

  //  def pure(): KeyValueStore[F, K, V]
object KeyValueStore {
  def apply[F[_], K, V](implicit kvStore: KeyValueStore[F, K, V]): KeyValueStore[F, K, V] = kvStore

//  def getOrElse[F[_], K, V](key: K) = KeyValueStore.apply[F, K, V].get(key)

    def retry[F[_]: Monad, K, V](
                                  first: KeyValueStore[F, K, V],
                                  second: KeyValueStore[F, K, V]
                                ): KeyValueStore[F, K, V] = new KeyValueStore[F, K, V] {

      def get(k: K): F[Option[V]] =
        OptionT(first.get(k))
          .orElseF(second.get(k))
          .value

      def put(k: K, v: V): F[Unit] = first.put(k, v)
      def delete(k: K): F[Unit] = first.delete(k)
    }

    def redis[F[_]: Sync: MkRedis: Monad, K, V](host: String, port: Int, redisCodec: RedisCodec[K,V]) = new RedisStore[F, K, V](redisCodec, redisURI = RedisURI.fromUnderlying(JRedisURI.builder.withHost(host).withPort(port).build()))
    def file[F[_]: Files: Concurrent: Monad, K, V](filename: String = "filestore.kv", path: String = System.getProperty("user.dir")) = new Fs2FileKeyValueStore[F, K, V](filename, path)
}




