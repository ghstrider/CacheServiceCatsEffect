package com.arya.filestore

import cats.Monad
import com.arya.dsl.KeyValueStore
import scala.concurrent.duration.FiniteDuration

object FileKVStore {

  def fileKVStore[F[_] : Monad, K, V](fileStore: KeyValueStore[F, K, V]): KeyValueStore[F, K, V] = new KeyValueStore[F, K, V] {
    override def get(k: K): F[Option[V]] = fileStore.get(k)

    override def put(k: K, v: V): F[Unit] = fileStore.put(k, v)

    override def putWithTTL(k: K, v: V, ttl: FiniteDuration): F[Unit] = fileStore.putWithTTL(k, v, ttl)

    override def delete(k: K): F[Unit] = fileStore.delete(k)

    override def ttl(k: K): F[Option[FiniteDuration]] = fileStore.ttl(k)
  }

}
