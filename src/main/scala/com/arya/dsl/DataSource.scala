package com.arya.dsl

trait DataSource[F[_], A] {
  def get(): F[A]
  def put(a: A) : F[Unit]
}


