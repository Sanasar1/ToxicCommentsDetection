package com.example.toxicity.service

import com.example.toxicity.adapter.ToxicityDetectionAdapter
import com.example.toxicity.domain.PostEntity
import com.example.toxicity.domain.UserEntity
import com.example.toxicity.dto.CreatePostRequest
import com.example.toxicity.dto.PostDto
import com.example.toxicity.dto.toDto
import com.example.toxicity.repository.CommentRepository
import com.example.toxicity.repository.PostLikeRepository
import com.example.toxicity.repository.PostRepository
import com.example.toxicity.repository.UserRepository
import com.example.toxicity.security.CurrentUser
import com.example.toxicity.web.BadRequestException
import com.example.toxicity.web.NotFoundException
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PostService(
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val likeRepository: PostLikeRepository,
    private val commentRepository: CommentRepository,
    private val toxicityAdapter: ToxicityDetectionAdapter,
    private val feedService: FeedService,
    private val userService: UserService,
) {

    @Transactional
    fun create(req: CreatePostRequest): PostDto {
        val authorId = CurrentUser.id()
        val author = userRepository.findById(authorId)
            .orElseThrow { NotFoundException("User not found") }
        val check = toxicityAdapter.check(req.content)
        if (check.isToxic) {
            throw BadRequestException(buildToxicMessage("Post", check.rejectedBy))
        }
        val post = postRepository.save(PostEntity(authorId = authorId, content = req.content.trim()))
        feedService.onPostCreated(authorId, post.id!!, post.createdAt.epochSecond)
        return enrich(listOf(post), mapOf(authorId to author)).first()
    }

    @Transactional
    fun delete(postId: Long) {
        val me = CurrentUser.id()
        val post = postRepository.findById(postId).orElseThrow { NotFoundException("Post not found") }
        if (post.authorId != me) throw BadRequestException("Cannot delete another user's post")
        postRepository.deleteById(postId)
        feedService.onPostDeleted(post.authorId, postId)
    }

    @Transactional(readOnly = true)
    fun getById(postId: Long): PostDto {
        val post = postRepository.findById(postId).orElseThrow { NotFoundException("Post not found") }
        val author = userRepository.findById(post.authorId)
            .orElseThrow { NotFoundException("Author not found") }
        return enrich(listOf(post), mapOf(author.id!! to author)).first()
    }

    @Transactional(readOnly = true)
    fun listByUsername(username: String, limit: Int, offset: Int): List<PostDto> {
        val author = userRepository.findByUsername(username)
            ?: throw NotFoundException("User not found")
        val pageSize = limit.coerceIn(1, 100)
        val pageNum = (offset / pageSize).coerceAtLeast(0)
        val posts = postRepository.findAllByAuthorIdOrderByCreatedAtDesc(author.id!!, PageRequest.of(pageNum, pageSize))
        return enrich(posts, mapOf(author.id!! to author))
    }

    @Transactional(readOnly = true)
    fun feed(limit: Int, before: Long?): List<PostDto> {
        val me = CurrentUser.id()
        val ids = feedService.feedPostIds(me, limit.coerceIn(1, 50), before)
        if (ids.isEmpty()) return emptyList()
        val posts = postRepository.findAllByIdIn(ids).sortedByDescending { it.createdAt }
        val authors = userService.loadUsersById(posts.map { it.authorId }.toSet())
        return enrich(posts, authors)
    }

    fun enrich(posts: List<PostEntity>, authors: Map<Long, UserEntity>): List<PostDto> {
        if (posts.isEmpty()) return emptyList()
        val ids = posts.mapNotNull { it.id }
        val likedSet = CurrentUser.idOrNull()
            ?.let { likeRepository.findLikedPostIds(it, ids).toSet() }
            ?: emptySet()
        val likeCounts = likeRepository.countByPostIdIn(ids)
            .associate { (it[0] as Number).toLong() to (it[1] as Number).toLong() }
        val commentCounts = ids.associateWith { commentRepository.countByPostId(it) }
        return posts.map { p ->
            val author = authors[p.authorId]
                ?: throw NotFoundException("Author missing for post ${p.id}")
            PostDto(
                id = p.id!!,
                author = author.toDto(),
                content = p.content,
                createdAt = p.createdAt,
                likesCount = likeCounts[p.id] ?: 0,
                commentsCount = commentCounts[p.id] ?: 0,
                likedByMe = likedSet.contains(p.id),
            )
        }
    }
}
