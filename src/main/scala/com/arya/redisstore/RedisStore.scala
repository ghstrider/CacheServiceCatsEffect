package com.arya.redisstore

import cats.Monad
import com.arya.dsl.KeyValueStore
import cats.effect.{Resource, Sync}
import dev.profunktor.redis4cats._
import dev.profunktor.redis4cats.algebra.{KeyCommands, StringCommands}
import dev.profunktor.redis4cats.connection._
import dev.profunktor.redis4cats.data.RedisCodec
import dev.profunktor.redis4cats.effect.MkRedis


class RedisStore[F[_]: Sync: MkRedis: Monad, K, V](redisCodec: RedisCodec[K, V], redisURI: RedisURI) extends KeyValueStore[F, K, V]{

  val commandsApi: Resource[F, StringCommands[F, K, V]] =
    RedisClient[F]
      .fromUri(redisURI)
      .flatMap(Redis[F].fromClient(_, redisCodec))

  val keyCommands: Resource[F, KeyCommands[F, K]] = RedisClient[F].fromUri(redisURI).flatMap(Redis[F].fromClient(_, redisCodec))

  override def get(k: K): F[Option[V]] = commandsApi.use(redisCommand => redisCommand.get(k))

  override def put(k: K, v: V): F[Unit] = commandsApi.use(redisCommand => redisCommand.set(k, v))

  override def delete(k: K): F[Unit] = keyCommands.use(redisCommand => Sync[F].flatMap(redisCommand.del(k))(_ => Sync[F].pure(())))
}
