package com.example.toxicity.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class RegisterRequest(
    @field:NotBlank
    @field:Pattern(regexp = "^[a-zA-Z0-9_]{3,30}$", message = "Username must be 3-30 chars, letters/digits/underscores")
    val username: String,
    @field:NotBlank
    @field:Size(min = 6, max = 100)
    val password: String,
    @field:NotBlank
    @field:Size(min = 1, max = 100)
    val displayName: String,
)

data class LoginRequest(
    @field:NotBlank
    val username: String,
    @field:NotBlank
    val password: String,
)

data class AuthResponse(
    val token: String,
    val user: UserDto,
)
