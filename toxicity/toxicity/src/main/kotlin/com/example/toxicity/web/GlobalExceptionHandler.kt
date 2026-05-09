package com.example.toxicity.web

import com.example.toxicity.adapter.ToxicityServiceUnavailableException
import com.example.toxicity.security.UnauthorizedException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

class NotFoundException(message: String) : RuntimeException(message)
class BadRequestException(message: String) : RuntimeException(message)
class ConflictException(message: String) : RuntimeException(message)

data class ErrorResponse(val error: String)

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(UnauthorizedException::class)
    fun unauthorized(e: UnauthorizedException) =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse(e.message ?: "Unauthorized"))

    @ExceptionHandler(NotFoundException::class)
    fun notFound(e: NotFoundException) =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse(e.message ?: "Not found"))

    @ExceptionHandler(BadRequestException::class)
    fun badRequest(e: BadRequestException) =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse(e.message ?: "Bad request"))

    @ExceptionHandler(ConflictException::class)
    fun conflict(e: ConflictException) =
        ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse(e.message ?: "Conflict"))

    @ExceptionHandler(ToxicityServiceUnavailableException::class)
    fun toxicityUnavailable(e: ToxicityServiceUnavailableException) =
        ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ErrorResponse(e.message ?: "Moderation service unavailable"))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun validation(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val msg = e.bindingResult.fieldErrors.joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
            .ifBlank { "Validation failed" }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse(msg))
    }
}
