package com.example.toxicity.service.impl

import com.example.toxicity.adapter.ToxicityDetectionAdapter
import com.example.toxicity.dto.PostDto
import com.example.toxicity.dto.PublishPostResponse
import com.example.toxicity.service.PostService
import org.springframework.stereotype.Service

@Service
class PostServiceImpl(
    private val toxicityDetectionAdapter: ToxicityDetectionAdapter
) : PostService {

    override fun publishPost(postDto: PostDto): PublishPostResponse {
        val toxicityPredictionResponse = toxicityDetectionAdapter.getToxicityPrediction(postDto)!!
        val toxicityPredictionLabel = toxicityPredictionResponse.label

        return when (toxicityPredictionLabel) {
            SAFE_POST_LABEL -> PublishPostResponse(success = true, errorMessage = null)
            TOXIC_POST_LABEL -> PublishPostResponse(success = false, errorMessage = TOXIC_MESSAGE_ERROR_MESSAGE)
            else -> throw IllegalStateException("Received unexpected toxicity label")
        }
    }

    companion object {
        private const val SAFE_POST_LABEL = 0
        private const val TOXIC_POST_LABEL = 1
        private const val TOXIC_MESSAGE_ERROR_MESSAGE = "Toxic message"
    }
}