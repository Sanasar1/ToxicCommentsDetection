package com.example.toxicity.service

import com.example.toxicity.adapter.ToxicityDetectionAdapter
import com.example.toxicity.domain.CommentEntity
import com.example.toxicity.dto.CommentDto
import com.example.toxicity.dto.CreateCommentRequest
import com.example.toxicity.dto.toDto
import com.example.toxicity.repository.CommentRepository
import com.example.toxicity.repository.PostRepository
import com.example.toxicity.repository.UserRepository
import com.example.toxicity.security.CurrentUser
import com.example.toxicity.web.BadRequestException
import com.example.toxicity.web.NotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CommentService(
    private val commentRepository: CommentRepository,
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val toxicityAdapter: ToxicityDetectionAdapter,
) {

    @Transactional
    fun create(postId: Long, req: CreateCommentRequest): CommentDto {
        val me = CurrentUser.id()
        if (!postRepository.existsById(postId)) throw NotFoundException("Post not found")
        val check = toxicityAdapter.check(req.content)
        if (check.isToxic) {
            throw BadRequestException(buildToxicMessage("Comment", check.rejectedBy))
        }
        val author = userRepository.findById(me).orElseThrow { NotFoundException("User not found") }
        val saved = commentRepository.save(
            CommentEntity(postId = postId, authorId = me, content = req.content.trim())
        )
        return CommentDto(
            id = saved.id!!,
            postId = postId,
            author = author.toDto(),
            content = saved.content,
            createdAt = saved.createdAt,
        )
    }

    @Transactional
    fun delete(commentId: Long) {
        val me = CurrentUser.id()
        val comment = commentRepository.findById(commentId)
            .orElseThrow { NotFoundException("Comment not found") }
        if (comment.authorId != me) throw BadRequestException("Cannot delete another user's comment")
        commentRepository.deleteById(commentId)
    }

    @Transactional(readOnly = true)
    fun listByPost(postId: Long): List<CommentDto> {
        if (!postRepository.existsById(postId)) throw NotFoundException("Post not found")
        val comments = commentRepository.findAllByPostIdOrderByCreatedAtAsc(postId)
        if (comments.isEmpty()) return emptyList()
        val authors = userRepository.findAllByIdIn(comments.map { it.authorId }.toSet())
            .associateBy { it.id!! }
        return comments.map { c ->
            CommentDto(
                id = c.id!!,
                postId = c.postId,
                author = (authors[c.authorId]
                    ?: throw NotFoundException("Author missing")).toDto(),
                content = c.content,
                createdAt = c.createdAt,
            )
        }
    }
}
