package com.example.toxicity.controller

import com.example.toxicity.dto.PostDto
import com.example.toxicity.dto.PublishPostResponse
import com.example.toxicity.service.PostService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/posts")
class PostController(
    private val postService: PostService
) {

    @PostMapping("")
    fun publishPost(@RequestBody post: PostDto): PublishPostResponse {
        return postService.publishPost(post)
    }
}