package com.example.toxicity.controller

import com.example.toxicity.dto.PostDto
import com.example.toxicity.service.PostService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/feed")
class FeedController(
    private val postService: PostService,
) {

    @GetMapping
    fun feed(
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(required = false) before: Long?,
    ): List<PostDto> = postService.feed(limit, before)
}
