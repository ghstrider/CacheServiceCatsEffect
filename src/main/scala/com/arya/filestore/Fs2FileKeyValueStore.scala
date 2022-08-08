package com.arya.filestore

import cats.implicits._
import cats.Monad
import cats.effect.Concurrent
import com.arya.dsl.KeyValueStore
import fs2.io.file.{Files, Path}


class Fs2FileKeyValueStore[F[_] : Files : Concurrent : Monad, K, V](filename: String, path: String) extends KeyValueStore[F, K, V]{

  private val absPath: Path = Path(path) / filename

  private val createFileIfAbsent: F[Unit] = Files[F].isRegularFile(absPath).flatMap {
    case false => Files[F].createFile(absPath)
    case true => Monad[F].pure(())
  }

  private val fs2f = new Fs2File(absPath)

  override def get(k: K): F[Option[V]] = createFileIfAbsent.flatMap(_ => fs2f.getOne[F, K, V](k))

  override def put(k: K, v: V): F[Unit] = createFileIfAbsent.flatMap(_ => fs2f.putOne(k, v))

  override def delete(k: K): F[Unit] = createFileIfAbsent.flatMap(_ => fs2f.delete(k))
}