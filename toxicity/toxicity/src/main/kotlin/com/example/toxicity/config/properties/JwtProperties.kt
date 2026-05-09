package com.example.toxicity.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties("security.jwt")
class JwtProperties {
    lateinit var secret: String
    var expirationSeconds: Long = 604800
}
