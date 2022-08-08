package com.arya.cache

import cats.effect.kernel.{Resource, Sync}
import com.arya.dsl.KeyValueStore

class CacheServiceImpl[F[_]: Sync]() {
  def make(implicit keyValueStore: KeyValueStore[F, String, String]): Resource[F, KeyValueStore[F, String, String]] = {
    Resource.eval(Sync[F].delay(keyValueStore))
  }

  def get(key: String)(implicit keyValueStore: KeyValueStore[F, String, String]): F[Option[String]] = make.use(store =>store.get(key))
  def put(key: String, value: String)(implicit keyValueStore: KeyValueStore[F, String, String]): F[Unit] = make.use(store => store.put(key, value))
  def del(key: String)(implicit keyValueStore: KeyValueStore[F, String, String]): F[Unit] = make.use(store => store.delete(key))
}

object CacheServiceImpl {
  def apply[F[_]: Sync] = new CacheServiceImpl[F]
}
