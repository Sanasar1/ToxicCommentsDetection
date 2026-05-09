package com.example.toxicity.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthFilter(
    private val jwtService: JwtService,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val header = request.getHeader("Authorization")
        if (header != null && header.startsWith("Bearer ")) {
            val token = header.substring(7).trim()
            val userId = jwtService.parseUserId(token)
            if (userId != null) {
                CurrentUser.set(userId)
            }
        }
        try {
            filterChain.doFilter(request, response)
        } finally {
            CurrentUser.clear()
        }
    }
}
