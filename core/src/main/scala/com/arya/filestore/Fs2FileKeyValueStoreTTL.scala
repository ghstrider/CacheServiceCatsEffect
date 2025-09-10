package com.arya.filestore

import cats.implicits._
import cats.Monad
import cats.effect.{Clock, Concurrent, Sync}
import com.arya.dsl.KeyValueStore
import fs2.io.file.{Files, Path}
import scala.concurrent.duration._

class Fs2FileKeyValueStoreTTL[F[_] : Files : Concurrent : Clock : Monad, K, V](filename: String, path: String) extends KeyValueStore[F, K, V] {

  private val absPath: Path = Path(path) / filename
  private val ttlPath: Path = Path(path) / s"$filename.ttl"  // Separate file for TTL data

  private val createFileIfAbsent: F[Unit] = for {
    _ <- Files[F].isRegularFile(absPath).flatMap {
      case false => Files[F].createFile(absPath)
      case true => Monad[F].pure(())
    }
    _ <- Files[F].isRegularFile(ttlPath).flatMap {
      case false => Files[F].createFile(ttlPath)
      case true => Monad[F].pure(())
    }
  } yield ()

  private val fs2f = new Fs2File(absPath)
  private val fs2fTtl = new Fs2File(ttlPath)

  // Check if key has expired
  private def isExpired(k: K): F[Boolean] = {
    Files[F].exists(ttlPath).flatMap { ttlFileExists =>
      if (ttlFileExists) {
        fs2fTtl.getOne[F, K, String](k).flatMap {
          case Some(expTimeStr) =>
            Clock[F].realTime.map { now =>
              val expTime = expTimeStr.toLong
              val nowMillis = now.toMillis
              nowMillis > expTime
            }
          case None => Monad[F].pure(false)  // No TTL set
        }
      } else {
        Monad[F].pure(false)  // TTL file doesn't exist, so no keys are expired
      }
    }
  }

  override def get(k: K): F[Option[V]] = 
    createFileIfAbsent.flatMap { _ =>
      isExpired(k).flatMap { expired =>
        if (expired) {
          // Clean up expired entry
          delete(k).as(None)
        } else {
          fs2f.getOne[F, K, V](k)
        }
      }
    }

  override def put(k: K, v: V): F[Unit] = 
    createFileIfAbsent.flatMap { _ => 
      // Remove any existing TTL when putting without TTL
      fs2fTtl.delete(k).flatMap(_ => fs2f.putOne(k, v))
    }

  override def putWithTTL(k: K, v: V, ttl: FiniteDuration): F[Unit] = 
    createFileIfAbsent.flatMap { _ =>
      Clock[F].realTime.flatMap { now =>
        val expirationTime = now.toMillis + ttl.toMillis
        for {
          _ <- fs2f.putOne(k, v)
          _ <- fs2fTtl.putOne(k, expirationTime.toString)
        } yield ()
      }
    }

  override def delete(k: K): F[Unit] = 
    createFileIfAbsent.flatMap { _ => 
      for {
        _ <- fs2f.delete(k)
        _ <- fs2fTtl.delete(k)  // Also delete TTL data
      } yield ()
    }

  override def ttl(k: K): F[Option[FiniteDuration]] = 
    Files[F].exists(ttlPath).flatMap { ttlFileExists =>
      if (ttlFileExists) {
        fs2fTtl.getOne[F, K, String](k).flatMap {
          case Some(expTimeStr) =>
            Clock[F].realTime.map { now =>
              val expTime = expTimeStr.toLong
              val nowMillis = now.toMillis
              val remainingMillis = expTime - nowMillis
              if (remainingMillis > 0)
                Some(remainingMillis.millis)
              else
                None
            }
          case None => Monad[F].pure(None)
        }
      } else {
        Monad[F].pure(None)  // TTL file doesn't exist, so no TTL
      }
    }
}