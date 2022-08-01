package com.arya.dsl

import cats.data.Kleisli

trait KVStore[F[_], K, V] {
  def get(k: K): F[Option[V]]
  def put(k: K, v: V): F[Unit]
  def delete(k: K): F[Unit]

  def getK: Kleisli[F, K, Option[V]] = Kleisli((k: K) => get(k))
  def putK: Kleisli[F, (K, V), Unit] = Kleisli{case(k,v) => put(k, v)}
  def delK: Kleisli[F, K, Unit] = Kleisli((k: K) => delete(k))

}

object KVStore {
//  def get[F[_],K, V]
}
