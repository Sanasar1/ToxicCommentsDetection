package com.example.toxicity.service

import com.example.toxicity.domain.UserEntity
import com.example.toxicity.dto.ProfileDto
import com.example.toxicity.dto.UpdateProfileRequest
import com.example.toxicity.dto.UserDto
import com.example.toxicity.dto.toDto
import com.example.toxicity.repository.FollowRepository
import com.example.toxicity.repository.PostRepository
import com.example.toxicity.repository.UserRepository
import com.example.toxicity.security.CurrentUser
import com.example.toxicity.web.NotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val followRepository: FollowRepository,
    private val postRepository: PostRepository,
) {

    @Transactional(readOnly = true)
    fun me(): UserDto {
        val id = CurrentUser.id()
        val user = userRepository.findById(id).orElseThrow { NotFoundException("User not found") }
        return user.toDto()
    }

    @Transactional(readOnly = true)
    fun getProfile(username: String): ProfileDto {
        val user = userRepository.findByUsername(username)
            ?: throw NotFoundException("User not found")
        val viewerId = CurrentUser.idOrNull()
        val isFollowing = viewerId != null && viewerId != user.id &&
            followRepository.existsById(com.example.toxicity.domain.FollowId(viewerId, user.id!!))
        return ProfileDto(
            user = user.toDto(),
            postsCount = postRepository.countByAuthorId(user.id!!),
            followersCount = followRepository.countByIdFolloweeId(user.id!!),
            followingCount = followRepository.countByIdFollowerId(user.id!!),
            isFollowing = isFollowing,
            isMe = viewerId == user.id,
        )
    }

    @Transactional
    fun updateProfile(req: UpdateProfileRequest): UserDto {
        val id = CurrentUser.id()
        val user = userRepository.findById(id).orElseThrow { NotFoundException("User not found") }
        req.displayName?.takeIf { it.isNotBlank() }?.let { user.displayName = it.trim() }
        req.bio?.let { user.bio = it.take(500).ifBlank { null } }
        return user.toDto()
    }

    @Transactional(readOnly = true)
    fun loadUsersById(ids: Collection<Long>): Map<Long, UserEntity> =
        if (ids.isEmpty()) emptyMap() else userRepository.findAllByIdIn(ids).associateBy { it.id!! }
}
