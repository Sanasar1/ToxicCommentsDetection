package com.example.toxicity.service

import com.example.toxicity.domain.PostLikeEntity
import com.example.toxicity.domain.PostLikeId
import com.example.toxicity.repository.PostLikeRepository
import com.example.toxicity.repository.PostRepository
import com.example.toxicity.security.CurrentUser
import com.example.toxicity.web.NotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class LikeStatus(val likesCount: Long, val likedByMe: Boolean)

@Service
class LikeService(
    private val likeRepository: PostLikeRepository,
    private val postRepository: PostRepository,
) {

    @Transactional
    fun like(postId: Long): LikeStatus {
        val me = CurrentUser.id()
        if (!postRepository.existsById(postId)) throw NotFoundException("Post not found")
        val id = PostLikeId(postId = postId, userId = me)
        if (!likeRepository.existsById(id)) {
            likeRepository.save(PostLikeEntity(id = id))
        }
        return LikeStatus(likeRepository.countByIdPostId(postId), true)
    }

    @Transactional
    fun unlike(postId: Long): LikeStatus {
        val me = CurrentUser.id()
        if (!postRepository.existsById(postId)) throw NotFoundException("Post not found")
        val id = PostLikeId(postId = postId, userId = me)
        if (likeRepository.existsById(id)) {
            likeRepository.deleteById(id)
        }
        return LikeStatus(likeRepository.countByIdPostId(postId), false)
    }
}
