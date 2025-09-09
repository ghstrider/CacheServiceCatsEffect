package com.arya.config

import pureconfig._
import pureconfig.generic.auto._
import cats.effect.{Sync, Resource}
import pureconfig.ConfigSource
import scala.concurrent.duration.FiniteDuration

case class RedisConfig(
  host: String,
  port: Int,
  password: Option[String],
  connectionPool: ConnectionPoolConfig,
  retry: RetryConfig
)

case class ConnectionPoolConfig(
  maxConnections: Int,
  maxIdle: Int,
  timeoutMs: Long
)

case class RetryConfig(
  maxAttempts: Int,
  initialDelayMs: Long,
  maxDelayMs: Long,
  backoffMultiplier: Double
)

case class FileStoreConfig(
  path: String,
  permissions: String,
  bufferSize: Int,
  useFileLock: Boolean
)

case class CacheLayersConfig(
  primary: String,
  fallback: String,
  enableFallback: Boolean
)

case class CacheConfig(
  defaultTtl: Long,
  maxSize: Int,
  evictionPolicy: String
)

case class LoggingConfig(
  level: String,
  logCacheMetrics: Boolean,
  slowOperationThreshold: Long
)

case class MetricsConfig(
  enabled: Boolean,
  exportIntervalSeconds: Int
)

case class ApplicationConfig(
  name: String,
  version: String,
  healthCheckEnabled: Boolean,
  metrics: MetricsConfig
)

case class CacheServiceConfig(
  redis: RedisConfig,
  fileStore: FileStoreConfig,
  cacheLayers: CacheLayersConfig,
  cache: CacheConfig,
  logging: LoggingConfig,
  application: ApplicationConfig
)

object AppConfig {
  def load[F[_]: Sync]: F[CacheServiceConfig] = {
    Sync[F].delay {
      ConfigSource.default.at("cache-service").loadOrThrow[CacheServiceConfig]
    }
  }
  
  def loadResource[F[_]: Sync]: Resource[F, CacheServiceConfig] = {
    Resource.eval(load[F])
  }
}