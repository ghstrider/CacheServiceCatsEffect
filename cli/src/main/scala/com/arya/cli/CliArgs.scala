package com.arya.cli

import cats.implicits._
import com.monovore.decline.Opts
import scala.concurrent.duration._

object CliArgs {
  case class Get(key: String)
  case class Put(key: String, value: String, ttl: Option[Long])  // TTL in seconds
  case class Del(key: String)
  case class Ttl(key: String)  // New command to check TTL

  val getKeyOpts: Opts[String] = Opts.option[String]("key", "Fetches value for given key")
  val getOpts: Opts[Get] = Opts.subcommand("get", "Get value of key from kvstore") {
    getKeyOpts.map(Get)
  }

  val putKeyArgsOpts: Opts[String] = Opts.option[String]("key", "Provide key")
  val putValueArgsOpts: Opts[String] = Opts.option[String]("value", "Provide value")
  val putTtlOpts: Opts[Option[Long]] = Opts.option[Long]("ttl", "Time-to-live in seconds").orNone
  
  val putOpts: Opts[Put] = Opts.subcommand("put", "Puts key and value into kvstore with optional TTL") {
    (putKeyArgsOpts, putValueArgsOpts, putTtlOpts).mapN(Put)
  }

  val delKeyOpts: Opts[String] = Opts.option[String]("key", "Delete key from kvstore")
  val delOpts: Opts[Del] = Opts.subcommand("del", "Delete key and its value from kvstore") {
    delKeyOpts.map(Del)
  }

  val ttlKeyOpts: Opts[String] = Opts.option[String]("key", "Get remaining TTL for key")
  val ttlOpts: Opts[Ttl] = Opts.subcommand("ttl", "Get remaining time-to-live for a key") {
    ttlKeyOpts.map(Ttl)
  }
}
