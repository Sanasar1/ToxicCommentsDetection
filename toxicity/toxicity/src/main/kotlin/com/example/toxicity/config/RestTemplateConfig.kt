package com.example.toxicity.config

import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestTemplate

@Configuration
class RestTemplateConfig {

    @Bean
    fun restTemplate(builder: RestTemplateBuilder): RestTemplate {
        val rt = builder.build()
        rt.requestFactory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(3_000)
            setReadTimeout(15_000)
        }
        return rt
    }
}
