package com.example.toxicity.adapter

import com.example.toxicity.config.properties.ToxicityDetectionProperties
import com.example.toxicity.dto.ToxicityPredictionRequest
import com.example.toxicity.dto.ToxicityPredictionResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate

class ToxicityServiceUnavailableException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

data class ToxicityCheckResult(
    val isToxic: Boolean,
    val rejectedBy: List<String>,
)

@Component
class ToxicityDetectionAdapter(
    private val props: ToxicityDetectionProperties,
    private val restTemplate: RestTemplate,
) {

    private val log = LoggerFactory.getLogger(ToxicityDetectionAdapter::class.java)

    fun check(text: String): ToxicityCheckResult {
        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
        val request = HttpEntity(ToxicityPredictionRequest(text = text), headers)

        val response = try {
            restTemplate.postForEntity(props.url, request, ToxicityPredictionResponse::class.java).body
        } catch (e: RestClientException) {
            log.error("Toxicity service call failed: {}", e.message)
            throw ToxicityServiceUnavailableException("Moderation service is temporarily unavailable", e)
        }
        if (response == null) {
            throw ToxicityServiceUnavailableException("Moderation service returned an empty response")
        }
        val toxic = response.label == TOXIC_LABEL
        val categories = response.rejectedBy.orEmpty()
        log.debug("Toxicity prediction: label={} rejectedBy={}", response.label, categories)
        return ToxicityCheckResult(isToxic = toxic, rejectedBy = categories)
    }

    fun isToxic(text: String): Boolean = check(text).isToxic

    companion object {
        private const val TOXIC_LABEL = 1
    }
}
