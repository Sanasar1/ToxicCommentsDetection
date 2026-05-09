package com.example.toxicity.repository

import com.example.toxicity.domain.UserEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<UserEntity, Long> {
    fun findByUsername(username: String): UserEntity?
    fun existsByUsername(username: String): Boolean
    fun findAllByIdIn(ids: Collection<Long>): List<UserEntity>
}
