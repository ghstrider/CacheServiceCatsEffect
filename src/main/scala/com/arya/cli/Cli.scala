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
    case Get(key: String) => CacheServiceImpl[IO].get(key).as(ExitCode.Success)
    case Put(key: String, value: String) => CacheServiceImpl[IO].put(key, value).as(ExitCode.Success)
    case Del(key: String) => CacheServiceImpl[IO].del(key).as(ExitCode.Success)
  }
}


