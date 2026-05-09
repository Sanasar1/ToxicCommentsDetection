package com.example.toxicity.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

data class CreateCommentRequest(
    @field:NotBlank
    @field:Size(max = 1000)
    val content: String,
)

data class CommentDto(
    val id: Long,
    val postId: Long,
    val author: UserDto,
    val content: String,
    val createdAt: Instant,
)
