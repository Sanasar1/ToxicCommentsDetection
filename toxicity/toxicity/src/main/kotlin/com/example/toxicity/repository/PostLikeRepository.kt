package com.example.toxicity.repository

import com.example.toxicity.domain.PostLikeEntity
import com.example.toxicity.domain.PostLikeId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface PostLikeRepository : JpaRepository<PostLikeEntity, PostLikeId> {

    @Query("SELECT pl.id.postId FROM PostLikeEntity pl WHERE pl.id.userId = :userId AND pl.id.postId IN :postIds")
    fun findLikedPostIds(@Param("userId") userId: Long, @Param("postIds") postIds: Collection<Long>): List<Long>

    @Query("SELECT pl.id.postId, COUNT(pl) FROM PostLikeEntity pl WHERE pl.id.postId IN :postIds GROUP BY pl.id.postId")
    fun countByPostIdIn(@Param("postIds") postIds: Collection<Long>): List<Array<Any>>

    fun countByIdPostId(postId: Long): Long
}
