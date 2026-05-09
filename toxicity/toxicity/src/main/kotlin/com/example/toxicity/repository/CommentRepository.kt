package com.example.toxicity.repository

import com.example.toxicity.domain.CommentEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CommentRepository : JpaRepository<CommentEntity, Long> {
    fun findAllByPostIdOrderByCreatedAtAsc(postId: Long): List<CommentEntity>
    fun countByPostId(postId: Long): Long
}
