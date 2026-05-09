package com.example.toxicity.security

import com.example.toxicity.config.properties.JwtProperties
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtService(props: JwtProperties) {

    private val key: SecretKey = Keys.hmacShaKeyFor(props.secret.toByteArray(StandardCharsets.UTF_8))
    private val expirationMillis: Long = props.expirationSeconds * 1000

    fun issue(userId: Long, username: String): String {
        val now = Date()
        return Jwts.builder()
            .subject(userId.toString())
            .claim("username", username)
            .issuedAt(now)
            .expiration(Date(now.time + expirationMillis))
            .signWith(key, Jwts.SIG.HS256)
            .compact()
    }

    fun parseUserId(token: String): Long? = try {
        Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload.subject.toLong()
    } catch (_: Exception) {
        null
    }
}
