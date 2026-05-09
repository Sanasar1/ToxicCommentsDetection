package com.example.toxicity.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

data class CreatePostRequest(
    @field:NotBlank
    @field:Size(max = 2000)
    val content: String,
)

data class PostDto(
    val id: Long,
    val author: UserDto,
    val content: String,
    val createdAt: Instant,
    val likesCount: Long,
    val commentsCount: Long,
    val likedByMe: Boolean,
)
