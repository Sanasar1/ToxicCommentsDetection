package com.example.toxicity.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties("service.toxicity-detection")
class ToxicityDetectionProperties {
    lateinit var url: String
}