package com.example.toxicity.adapter

import com.example.toxicity.config.properties.ToxicityDetectionProperties
import com.example.toxicity.dto.PostDto
import com.example.toxicity.dto.ToxicityPredictionResponse
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class ToxicityDetectionAdapter(
    private val props: ToxicityDetectionProperties,
    private val restTemplate: RestTemplate
) {

    fun getToxicityPrediction(post: PostDto): ToxicityPredictionResponse? {
        val url = props.url

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        val request = HttpEntity(post, headers)

        val response = restTemplate.postForEntity(
            url,
            request,
            ToxicityPredictionResponse::class.java
        )

        return response.body
    }
}