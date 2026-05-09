package com.example.toxicity.dto

import com.example.toxicity.domain.UserEntity
import java.time.Instant

data class UserDto(
    val id: Long,
    val username: String,
    val displayName: String,
    val bio: String?,
    val createdAt: Instant,
)

data class ProfileDto(
    val user: UserDto,
    val postsCount: Long,
    val followersCount: Long,
    val followingCount: Long,
    val isFollowing: Boolean,
    val isMe: Boolean,
)

data class UpdateProfileRequest(
    val displayName: String?,
    val bio: String?,
)

fun UserEntity.toDto() = UserDto(
    id = id!!,
    username = username,
    displayName = displayName,
    bio = bio,
    createdAt = createdAt,
)
