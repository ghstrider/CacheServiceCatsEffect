package com.arya

import cats.effect.{IO, IOApp, Resource}
import com.arya.cache.CacheServiceImpl
import com.arya.config.{AppConfig, CacheServiceConfig}
import com.arya.dsl.KeyValueStore
import dev.profunktor.redis4cats.data.RedisCodec
import com.arya.redisstore.RedisStore
import dev.profunktor.redis4cats.log4cats._
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import com.arya.filestore.Fs2FileKeyValueStore

object CacheServiceMainWithConfig extends IOApp.Simple {
  
  implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]
  
  def createCacheStore(config: CacheServiceConfig): IO[KeyValueStore[IO, String, String]] = IO {
    val redisCodec: RedisCodec[String, String] = RedisCodec.Utf8
    
    val primaryStore = config.cacheLayers.primary match {
      case "redis" => 
        KeyValueStore.redis[IO, String, String](
          config.redis.host, 
          config.redis.port, 
          redisCodec
        )
      case "file" => 
        KeyValueStore.file[IO, String, String]()
      case other => 
        throw new IllegalArgumentException(s"Unknown primary cache type: $other")
    }
    
    val fallbackStore = if (config.cacheLayers.enableFallback) {
      config.cacheLayers.fallback match {
        case "file" => 
          Some(KeyValueStore.file[IO, String, String]())
        case "redis" => 
          Some(KeyValueStore.redis[IO, String, String](
            config.redis.host, 
            config.redis.port, 
            redisCodec
          ))
        case _ => None
      }
    } else None
    
    fallbackStore match {
      case Some(fallback) => KeyValueStore.retry(primaryStore, fallback)
      case None => primaryStore
    }
  }
  
  override def run: IO[Unit] = {
    val program = for {
      config <- AppConfig.loadResource[IO]
      _ <- Resource.eval(logger.info(s"Starting ${config.application.name} v${config.application.version}"))
      _ <- Resource.eval(logger.info(s"Primary cache: ${config.cacheLayers.primary}"))
      _ <- Resource.eval(logger.info(s"Fallback cache: ${config.cacheLayers.fallback} (enabled: ${config.cacheLayers.enableFallback})"))
      cacheStore <- Resource.eval(createCacheStore(config))
    } yield cacheStore
    
    program.use { implicit kvStore =>
      val cacheService = CacheServiceImpl[IO]
      for {
        _ <- cacheService.put("config-test", "Configuration working!")
        value <- cacheService.get("config-test")
        _ <- logger.info(s"Retrieved value: $value")
      } yield ()
    }
  }
}