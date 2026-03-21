package com.example.toxicity.service

import com.example.toxicity.dto.PostDto
import com.example.toxicity.dto.PublishPostResponse

interface PostService {
    fun publishPost(postDto: PostDto): PublishPostResponse
}