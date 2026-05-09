package com.example.toxicity.controller

import com.example.toxicity.dto.CommentDto
import com.example.toxicity.dto.CreateCommentRequest
import com.example.toxicity.dto.CreatePostRequest
import com.example.toxicity.dto.PostDto
import com.example.toxicity.service.CommentService
import com.example.toxicity.service.LikeService
import com.example.toxicity.service.LikeStatus
import com.example.toxicity.service.PostService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/posts")
class PostController(
    private val postService: PostService,
    private val commentService: CommentService,
    private val likeService: LikeService,
) {

    @PostMapping
    fun create(@Valid @RequestBody req: CreatePostRequest): PostDto = postService.create(req)

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): PostDto = postService.getById(id)

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long) = postService.delete(id)

    @GetMapping("/{id}/comments")
    fun comments(@PathVariable id: Long): List<CommentDto> = commentService.listByPost(id)

    @PostMapping("/{id}/comments")
    fun addComment(@PathVariable id: Long, @Valid @RequestBody req: CreateCommentRequest): CommentDto =
        commentService.create(id, req)

    @PostMapping("/{id}/like")
    fun like(@PathVariable id: Long): LikeStatus = likeService.like(id)

    @DeleteMapping("/{id}/like")
    fun unlike(@PathVariable id: Long): LikeStatus = likeService.unlike(id)
}
