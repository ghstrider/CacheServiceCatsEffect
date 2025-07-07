package com.arya.cli

import cats.implicits._
import com.monovore.decline.Opts

object CliArgs {
  case class Get(key: String)

  case class Put(key: String, value: String)

  case class Del(key: String)

  val getKeyOpts: Opts[String] = Opts.option[String]("key", "Fetches value for given key")
  val getOpts: Opts[Get] = Opts.subcommand("get", "Get value of key from kvstore") {
    getKeyOpts.map(Get)
  }

  val putKeyArgsOpts: Opts[String] = Opts.option[String]("key", "Provide key")
  val putValueArgsOpts: Opts[String] = Opts.option[String]("value", "Provide value")
  val putOpts: Opts[Put] = Opts.subcommand("put", "Puts key and value into kvstore") {
    (putKeyArgsOpts, putValueArgsOpts).mapN(Put)
  }

  val delKeyOpts: Opts[String] = Opts.option[String]("key", "Fetches value for given key")
  val delOpts: Opts[Del] = Opts.subcommand("del", "Delete key and its value from kvstore") {
    delKeyOpts.map(Del)
  }
}