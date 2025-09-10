package com.arya.cli

import com.monovore.decline.Command
import org.scalatest.funsuite.AnyFunSuite
import com.arya.cli.CliArgs._

class CliArgsSpec extends AnyFunSuite {

  private val cmd = Command("kvstore", "kv store app")(getOpts.orElse(putOpts).orElse(delOpts).orElse(ttlOpts))

  test("parse get command") {
    val result = cmd.parse(Seq("get", "--key", "foo"))
    assert(result == Right(Get("foo")))
  }

  test("parse put command") {
    val result = cmd.parse(Seq("put", "--key", "foo", "--value", "bar"))
    assert(result == Right(Put("foo", "bar", None)))
  }
  
  test("parse put command with TTL") {
    val result = cmd.parse(Seq("put", "--key", "foo", "--value", "bar", "--ttl", "300"))
    assert(result == Right(Put("foo", "bar", Some(300))))
  }

  test("parse del command") {
    val result = cmd.parse(Seq("del", "--key", "foo"))
    assert(result == Right(Del("foo")))
  }
  
  test("parse ttl command") {
    val result = cmd.parse(Seq("ttl", "--key", "foo"))
    assert(result == Right(Ttl("foo")))
  }
}
