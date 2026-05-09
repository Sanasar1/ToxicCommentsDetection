package com.example.toxicity.service

import com.example.toxicity.domain.UserEntity
import com.example.toxicity.dto.AuthResponse
import com.example.toxicity.dto.LoginRequest
import com.example.toxicity.dto.RegisterRequest
import com.example.toxicity.dto.toDto
import com.example.toxicity.repository.UserRepository
import com.example.toxicity.security.JwtService
import com.example.toxicity.security.UnauthorizedException
import com.example.toxicity.web.ConflictException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
) {

    @Transactional
    fun register(req: RegisterRequest): AuthResponse {
        if (userRepository.existsByUsername(req.username)) {
            throw ConflictException("Username already taken")
        }
        val user = UserEntity(
            username = req.username,
            passwordHash = passwordEncoder.encode(req.password),
            displayName = req.displayName,
        )
        val saved = userRepository.save(user)
        return AuthResponse(token = jwtService.issue(saved.id!!, saved.username), user = saved.toDto())
    }

    @Transactional(readOnly = true)
    fun login(req: LoginRequest): AuthResponse {
        val user = userRepository.findByUsername(req.username)
            ?: throw UnauthorizedException("Invalid credentials")
        if (!passwordEncoder.matches(req.password, user.passwordHash)) {
            throw UnauthorizedException("Invalid credentials")
        }
        return AuthResponse(token = jwtService.issue(user.id!!, user.username), user = user.toDto())
    }
}
