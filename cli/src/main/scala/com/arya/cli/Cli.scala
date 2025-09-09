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
  override def main: Opts[IO[ExitCode]] = (getOpts.orElse(putOpts).orElse(delOpts)).map {
    case Get(key: String) => 
      CacheServiceImpl[IO].get(key).flatMap {
        case Some(value) => IO.println(value).as(ExitCode.Success)
        case None => IO.println(s"Key '$key' not found").as(ExitCode.Error)
      }
    case Put(key: String, value: String) => 
      CacheServiceImpl[IO].put(key, value).flatMap { _ =>
        IO.println(s"Successfully stored key '$key'").as(ExitCode.Success)
      }
    case Del(key: String) => 
      CacheServiceImpl[IO].del(key).flatMap { _ =>
        IO.println(s"Successfully deleted key '$key'").as(ExitCode.Success)
      }
  }
}


