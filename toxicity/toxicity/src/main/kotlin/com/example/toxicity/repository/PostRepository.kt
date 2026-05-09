package com.example.toxicity.repository

import com.example.toxicity.domain.PostEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PostRepository : JpaRepository<PostEntity, Long> {
    fun findAllByAuthorIdOrderByCreatedAtDesc(authorId: Long, pageable: Pageable): List<PostEntity>
    fun findAllByAuthorIdInOrderByCreatedAtDesc(authorIds: Collection<Long>, pageable: Pageable): List<PostEntity>
    fun findAllByIdIn(ids: Collection<Long>): List<PostEntity>
    fun countByAuthorId(authorId: Long): Long
}
