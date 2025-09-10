package com.arya.webapi.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._
import scala.concurrent.duration.FiniteDuration

// Request models
case class PutRequest(value: String, ttl: Option[Long] = None)
case class PutResponse(message: String, key: String, ttl: Option[Long] = None)

// Response models
case class GetResponse(value: String)
case class TtlResponse(ttl: Option[Long], message: String)
case class DeleteResponse(message: String, key: String)

// Error response
case class ErrorResponse(error: String, message: String)

// Success response for operations without specific data
case class SuccessResponse(message: String)

object ApiModels {
  // JSON encoders and decoders
  implicit val putRequestDecoder: Decoder[PutRequest] = deriveDecoder[PutRequest]
  implicit val putRequestEncoder: Encoder[PutRequest] = deriveEncoder[PutRequest]
  
  implicit val putResponseDecoder: Decoder[PutResponse] = deriveDecoder[PutResponse]
  implicit val putResponseEncoder: Encoder[PutResponse] = deriveEncoder[PutResponse]
  
  implicit val getResponseDecoder: Decoder[GetResponse] = deriveDecoder[GetResponse]
  implicit val getResponseEncoder: Encoder[GetResponse] = deriveEncoder[GetResponse]
  
  implicit val ttlResponseDecoder: Decoder[TtlResponse] = deriveDecoder[TtlResponse]
  implicit val ttlResponseEncoder: Encoder[TtlResponse] = deriveEncoder[TtlResponse]
  
  implicit val deleteResponseDecoder: Decoder[DeleteResponse] = deriveDecoder[DeleteResponse]
  implicit val deleteResponseEncoder: Encoder[DeleteResponse] = deriveEncoder[DeleteResponse]
  
  implicit val errorResponseDecoder: Decoder[ErrorResponse] = deriveDecoder[ErrorResponse]
  implicit val errorResponseEncoder: Encoder[ErrorResponse] = deriveEncoder[ErrorResponse]
  
  implicit val successResponseDecoder: Decoder[SuccessResponse] = deriveDecoder[SuccessResponse]
  implicit val successResponseEncoder: Encoder[SuccessResponse] = deriveEncoder[SuccessResponse]
}