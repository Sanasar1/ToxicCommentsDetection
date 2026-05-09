package com.example.toxicity.repository

import com.example.toxicity.domain.FollowEntity
import com.example.toxicity.domain.FollowId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface FollowRepository : JpaRepository<FollowEntity, FollowId> {

    @Query("SELECT f.id.followeeId FROM FollowEntity f WHERE f.id.followerId = :followerId")
    fun findFolloweeIds(@Param("followerId") followerId: Long): List<Long>

    @Query("SELECT f.id.followerId FROM FollowEntity f WHERE f.id.followeeId = :followeeId")
    fun findFollowerIds(@Param("followeeId") followeeId: Long): List<Long>

    fun countByIdFollowerId(followerId: Long): Long
    fun countByIdFolloweeId(followeeId: Long): Long
}
