package com.example.toxicity.controller

import com.example.toxicity.dto.AuthResponse
import com.example.toxicity.dto.LoginRequest
import com.example.toxicity.dto.RegisterRequest
import com.example.toxicity.dto.UserDto
import com.example.toxicity.service.AuthService
import com.example.toxicity.service.UserService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService,
    private val userService: UserService,
) {

    @PostMapping("/register")
    fun register(@Valid @RequestBody req: RegisterRequest): AuthResponse = authService.register(req)

    @PostMapping("/login")
    fun login(@Valid @RequestBody req: LoginRequest): AuthResponse = authService.login(req)

    @GetMapping("/me")
    fun me(): UserDto = userService.me()
}
