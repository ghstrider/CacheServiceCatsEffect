package com.arya.cache

import cats.effect.kernel.{Resource, Sync}
import com.arya.dsl.KeyValueStore
import scala.concurrent.duration.FiniteDuration

class CacheServiceImpl[F[_]: Sync]() {
  def make(implicit keyValueStore: KeyValueStore[F, String, String]): Resource[F, KeyValueStore[F, String, String]] = {
    Resource.eval(Sync[F].delay(keyValueStore))
  }

  def get(key: String)(implicit keyValueStore: KeyValueStore[F, String, String]): F[Option[String]] = 
    make.use(store => store.get(key))
  
  def put(key: String, value: String)(implicit keyValueStore: KeyValueStore[F, String, String]): F[Unit] = 
    make.use(store => store.put(key, value))
  
  def putWithTTL(key: String, value: String, ttl: FiniteDuration)(implicit keyValueStore: KeyValueStore[F, String, String]): F[Unit] = 
    make.use(store => store.putWithTTL(key, value, ttl))
  
  def del(key: String)(implicit keyValueStore: KeyValueStore[F, String, String]): F[Unit] = 
    make.use(store => store.delete(key))
  
  def ttl(key: String)(implicit keyValueStore: KeyValueStore[F, String, String]): F[Option[FiniteDuration]] = 
    make.use(store => store.ttl(key))
}

object CacheServiceImpl {
  def apply[F[_]: Sync] = new CacheServiceImpl[F]
}
