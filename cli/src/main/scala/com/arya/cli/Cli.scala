package com.arya.cli

import cats.effect.{ExitCode, IO}
import com.arya.cache.CacheServiceImpl
import com.monovore.decline._
import com.monovore.decline.effect._
import com.arya.cli.CliArgs._
import com.arya.filestore.`implicit`._

object Cli extends CommandIOApp(
  name = "kvstore",
  header = "Cli implementation of kvstore") {
  
  import scala.concurrent.duration._
  
  override def main: Opts[IO[ExitCode]] = (getOpts.orElse(putOpts).orElse(delOpts).orElse(ttlOpts)).map {
    case Get(key: String) => 
      CacheServiceImpl[IO].get(key).flatMap {
        case Some(value) => IO.println(value).as(ExitCode.Success)
        case None => IO.println(s"Key '$key' not found").as(ExitCode.Error)
      }
    case Put(key: String, value: String, ttlOpt: Option[Long]) => 
      ttlOpt match {
        case Some(ttlSeconds) if ttlSeconds > 0 =>
          CacheServiceImpl[IO].putWithTTL(key, value, ttlSeconds.seconds).flatMap { _ =>
            IO.println(s"Successfully stored key '$key' with TTL of $ttlSeconds seconds").as(ExitCode.Success)
          }
        case _ =>
          CacheServiceImpl[IO].put(key, value).flatMap { _ =>
            IO.println(s"Successfully stored key '$key'").as(ExitCode.Success)
          }
      }
    case Del(key: String) => 
      CacheServiceImpl[IO].del(key).flatMap { _ =>
        IO.println(s"Successfully deleted key '$key'").as(ExitCode.Success)
      }
    case Ttl(key: String) =>
      CacheServiceImpl[IO].ttl(key).flatMap {
        case Some(duration) => 
          val seconds = duration.toSeconds
          IO.println(s"TTL for key '$key': $seconds seconds").as(ExitCode.Success)
        case None => 
          IO.println(s"Key '$key' has no TTL set or does not exist").as(ExitCode.Success)
      }
  }
}


